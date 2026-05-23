/**
 * expire-boosts — Supabase scheduled edge function.
 * Schedule in supabase/config.toml or via cron:
 *   "0 * * * *"  (every hour)
 *
 * Marks expired active boosts as "expired" and clears is_boosted flags
 * on their products.
 */
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!
const SUPABASE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } })

serve(async (_req) => {
  const supabase = createClient(SUPABASE_URL, SUPABASE_KEY)
  const now = new Date().toISOString()

  // Fetch all active boosts whose expires_at has passed
  const { data: expired, error } = await supabase
    .from("boosts")
    .select("id, user_id, product_ids, tier_name")
    .eq("status", "active")
    .lt("expires_at", now)

  if (error) {
    console.error("Error fetching expired boosts:", error.message)
    return json({ error: error.message }, 500)
  }

  if (!expired?.length) return json({ expired: 0 })

  const boostIds    = expired.map((b: any) => b.id)
  const productIds  = expired.flatMap((b: any) => b.product_ids ?? [])

  // Mark boosts as expired
  await supabase.from("boosts")
    .update({ status: "expired" })
    .in("id", boostIds)

  // Clear is_boosted on products (only if no other active boost covers them)
  if (productIds.length > 0) {
    // Find products still covered by a different active boost
    const { data: stillActive } = await supabase
      .from("boosts")
      .select("product_ids")
      .eq("status", "active")
      .gte("expires_at", now)

    const stillBoostedIds = new Set<string>(
      (stillActive ?? []).flatMap((b: any) => b.product_ids ?? [])
    )

    const toUnboost = productIds.filter((id: string) => !stillBoostedIds.has(id))
    if (toUnboost.length > 0) {
      await supabase.from("products")
        .update({ is_boosted: false, boosted_until: null })
        .in("id", toUnboost)
    }
  }

  // Notify users their boost expired
  const notifications = expired.map((b: any) => ({
    user_id: b.user_id,
    type:    "boost",
    title:   "Boost Expired",
    body:    `Your ${b.tier_name} boost has ended. Boost again to keep your products at the top!`,
    data:    { boost_id: b.id },
  }))
  if (notifications.length > 0) {
    await supabase.from("notifications").insert(notifications)
  }

  console.log(`Expired ${boostIds.length} boosts, unboosted ${productIds.length} products`)
  return json({ expired: boostIds.length, products_unboosted: productIds.length })
})
