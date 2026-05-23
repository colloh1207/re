import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const PAYSTACK_SECRET = Deno.env.get("PAYSTACK_SECRET_KEY") ?? ""
const SUPABASE_URL    = Deno.env.get("SUPABASE_URL")!
const SUPABASE_KEY    = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!

// Boost tier definitions — must match Android BoostTiers object
const TIERS: Record<string, { name: string; impressions: number; days: number; priceUsd: number }> = {
  starter:  { name: "Starter",  impressions: 500,     days: 3,  priceUsd: 1.99  },
  basic:    { name: "Basic",    impressions: 2500,    days: 7,  priceUsd: 4.99  },
  standard: { name: "Standard", impressions: 7500,    days: 14, priceUsd: 9.99  },
  premium:  { name: "Premium",  impressions: 20000,   days: 30, priceUsd: 24.99 },
  business: { name: "Business", impressions: 75000,   days: 60, priceUsd: 49.99 },
  elite:    { name: "Elite",    impressions: 250000,  days: 90, priceUsd: 99.99 },
}

// Approximate USD → local currency conversion rates
const USD_RATES: Record<string, number> = {
  USD: 1, NGN: 1550, GHS: 15.8, KES: 129, ZAR: 18.7,
  GBP: 0.79, EUR: 0.92, INR: 83.5, EGP: 48, UGX: 3740,
  TZS: 2640, XOF: 603, CAD: 1.36, AUD: 1.53, ZMW: 27, RWF: 1310,
}

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } })

serve(async (req) => {
  const supabase = createClient(SUPABASE_URL, SUPABASE_KEY)

  const authHeader = req.headers.get("Authorization")
  if (!authHeader) return json({ error: "Unauthorized" }, 401)
  const { data: { user } } = await supabase.auth.getUser(authHeader.replace("Bearer ", ""))
  if (!user) return json({ error: "Unauthorized" }, 401)

  const { product_ids, tier_id, currency = "USD" } = await req.json()
  if (!product_ids?.length) return json({ error: "product_ids required" }, 400)
  if (!TIERS[tier_id])      return json({ error: "Invalid tier" }, 400)

  const tier = TIERS[tier_id]
  const rate = USD_RATES[currency] ?? 1
  const localAmount = Math.round(tier.priceUsd * rate * 100) // kobo/pesewas/cents

  // Validate product ownership
  const { data: products, error: prodErr } = await supabase
    .from("products")
    .select("id, seller_id, title")
    .in("id", product_ids)
  if (prodErr || !products?.length) return json({ error: "Products not found" }, 404)
  if (products.some((p: any) => p.seller_id !== user.id))
    return json({ error: "You can only boost your own products" }, 403)

  // Create boost record (pending)
  const expiresAt = new Date(Date.now() + tier.days * 86_400_000).toISOString()
  const { data: boost, error: boostErr } = await supabase
    .from("boosts")
    .insert({
      user_id:                user.id,
      product_ids:            product_ids,
      tier_id:                tier_id,
      tier_name:              tier.name,
      status:                 "pending",
      payment_status:         "pending",
      currency:               currency,
      amount_paid:            tier.priceUsd * rate,
      impressions_guaranteed: tier.impressions,
      expires_at:             expiresAt,
    })
    .select()
    .single()
  if (boostErr) return json({ error: boostErr.message }, 500)

  // Initialize Paystack transaction
  let paystackAuthUrl = ""
  let paymentReference = `boost_${boost.id}_${Date.now()}`

  if (PAYSTACK_SECRET) {
    try {
      const { data: userData } = await supabase.from("users").select("email").eq("id", user.id).single()
      const paystackRes = await fetch("https://api.paystack.co/transaction/initialize", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${PAYSTACK_SECRET}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email:     userData?.email ?? user.email,
          amount:    localAmount,
          currency:  currency,
          reference: paymentReference,
          metadata: {
            boost_id:    boost.id,
            tier_id,
            user_id:     user.id,
            product_ids,
            custom_fields: products.map((p: any) => ({ display_name: "Product", value: p.title })),
          },
          callback_url: `${SUPABASE_URL}/functions/v1/paystack-webhook`,
        }),
      })
      const ps = await paystackRes.json()
      if (ps.status) {
        paystackAuthUrl = ps.data.authorization_url
        paymentReference = ps.data.reference
      }
    } catch (e) {
      console.error("Paystack init failed:", e)
      // Fall back to sandbox URL for testing
      paystackAuthUrl = `https://paystack.com/pay/sdd-boost-${boost.id}`
    }
  } else {
    // Dev/test mode — generate a fake auth URL
    paystackAuthUrl = `https://paystack.com/pay/sdd-boost-test-${boost.id}`
  }

  // Update boost with payment reference + auth URL
  await supabase
    .from("boosts")
    .update({ payment_reference: paymentReference, paystack_auth_url: paystackAuthUrl })
    .eq("id", boost.id)

  const result = { ...boost, payment_reference: paymentReference, paystack_auth_url: paystackAuthUrl }
  return json(result)
})
