import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const PAYSTACK_SECRET = Deno.env.get("PAYSTACK_SECRET_KEY") ?? ""
const SUPABASE_URL    = Deno.env.get("SUPABASE_URL")!
const SUPABASE_KEY    = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } })

serve(async (req) => {
  const supabase = createClient(SUPABASE_URL, SUPABASE_KEY)

  const authHeader = req.headers.get("Authorization")
  if (!authHeader) return json({ error: "Unauthorized" }, 401)
  const { data: { user } } = await supabase.auth.getUser(authHeader.replace("Bearer ", ""))
  if (!user) return json({ error: "Unauthorized" }, 401)

  const { boost_id } = await req.json()
  if (!boost_id) return json({ error: "boost_id required" }, 400)

  const { data: boost, error } = await supabase
    .from("boosts")
    .select("*")
    .eq("id", boost_id)
    .eq("user_id", user.id)
    .single()

  if (error || !boost) return json({ error: "Boost not found" }, 404)

  // Already resolved — return current state
  if (boost.payment_status === "paid" || boost.status === "active") return json(boost)
  if (boost.payment_status === "failed" || boost.status === "cancelled") return json(boost)

  // Poll Paystack for the real status
  if (PAYSTACK_SECRET && boost.payment_reference) {
    try {
      const res = await fetch(
        `https://api.paystack.co/transaction/verify/${encodeURIComponent(boost.payment_reference)}`,
        { headers: { Authorization: `Bearer ${PAYSTACK_SECRET}` } }
      )
      const ps = await res.json()

      if (ps.status && ps.data?.status === "success") {
        const tierDays: Record<string, number> = {
          starter: 3, basic: 7, standard: 14, premium: 30, business: 60, elite: 90
        }
        const now       = new Date()
        const days      = tierDays[boost.tier_id] ?? 7
        const expiresAt = new Date(now.getTime() + days * 86_400_000).toISOString()

        await supabase.from("boosts").update({
          status: "active", payment_status: "paid",
          started_at: now.toISOString(), expires_at: expiresAt,
        }).eq("id", boost_id)

        if (Array.isArray(boost.product_ids)) {
          await supabase.from("products")
            .update({ is_boosted: true, boosted_until: expiresAt })
            .in("id", boost.product_ids)
        }

        await supabase.from("notifications").insert({
          user_id: boost.user_id, type: "boost",
          title: "🚀 Your Boost is Live!",
          body: `Your ${boost.tier_name} boost is now active!`,
          data: { boost_id },
        })

        const { data: updated } = await supabase.from("boosts").select("*").eq("id", boost_id).single()
        return json(updated ?? boost)
      }

      if (ps.data?.status === "failed") {
        await supabase.from("boosts").update({
          payment_status: "failed", status: "cancelled",
        }).eq("id", boost_id)
        const { data: updated } = await supabase.from("boosts").select("*").eq("id", boost_id).single()
        return json(updated ?? boost)
      }

    } catch (e) {
      console.error("Paystack verify error:", e)
    }
  }

  // Still pending — return current state for client to retry
  return json(boost)
})
