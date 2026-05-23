import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"
import { createHmac } from "https://deno.land/std@0.168.0/node/crypto.ts"

const PAYSTACK_SECRET = Deno.env.get("PAYSTACK_SECRET_KEY") ?? ""
const SUPABASE_URL    = Deno.env.get("SUPABASE_URL")!
const SUPABASE_KEY    = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } })

serve(async (req) => {
  const supabase = createClient(SUPABASE_URL, SUPABASE_KEY)

  // Verify Paystack signature
  const signature = req.headers.get("x-paystack-signature") ?? ""
  const rawBody   = await req.text()

  if (PAYSTACK_SECRET) {
    // Signature MUST be present and valid whenever a secret is configured
    if (!signature) return json({ error: "Missing x-paystack-signature header" }, 401)
    const hash = createHmac("sha512", PAYSTACK_SECRET).update(rawBody).digest("hex")
    if (hash !== signature) return json({ error: "Invalid signature" }, 401)
  }

  let event: any
  try { event = JSON.parse(rawBody) } catch { return json({ error: "Invalid JSON" }, 400) }

  const { event: eventType, data } = event
  if (!data?.reference) return json({ ok: true }) // Ignore non-transaction events

  const reference = data.reference as string

  // Find the boost by payment reference
  const { data: boost } = await supabase
    .from("boosts")
    .select("*")
    .eq("payment_reference", reference)
    .single()

  if (!boost) return json({ ok: true }) // Not our transaction, ignore

  if (eventType === "charge.success" && data.status === "success") {
    const now = new Date()
    const tierDays: Record<string, number> = {
      starter: 3, basic: 7, standard: 14, premium: 30, business: 60, elite: 90
    }
    const days     = tierDays[boost.tier_id] ?? 7
    const expiresAt = new Date(now.getTime() + days * 86_400_000).toISOString()

    // Activate the boost
    await supabase.from("boosts").update({
      status:         "active",
      payment_status: "paid",
      started_at:     now.toISOString(),
      expires_at:     expiresAt,
    }).eq("id", boost.id)

    // Mark all boosted products as isBoosted = true
    if (Array.isArray(boost.product_ids)) {
      await supabase.from("products")
        .update({ is_boosted: true, boosted_until: expiresAt })
        .in("id", boost.product_ids)
    }

    // Notify user
    await supabase.from("notifications").insert({
      user_id: boost.user_id,
      type:    "boost",
      title:   "🚀 Your Boost is Live!",
      body:    `Your ${boost.tier_name} boost is now active. Your products are getting premium visibility!`,
      data:    { boost_id: boost.id },
    })

    console.log(`Boost ${boost.id} activated for products: ${boost.product_ids?.join(", ")}`)

  } else if (["charge.failed", "transfer.failed"].includes(eventType)) {
    await supabase.from("boosts").update({
      payment_status: "failed",
      status:         "cancelled",
    }).eq("id", boost.id)

    await supabase.from("notifications").insert({
      user_id: boost.user_id,
      type:    "boost",
      title:   "Payment Failed",
      body:    "Your boost payment could not be processed. Please try again.",
      data:    { boost_id: boost.id },
    })
  }

  return json({ ok: true })
})
