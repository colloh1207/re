import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

serve(async (req) => {
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!)
  const authHeader = req.headers.get("Authorization")
  if (!authHeader) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
  const { data: { user } } = await supabase.auth.getUser(authHeader.replace("Bearer ", ""))
  if (!user) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
  const { order_id, gateway } = await req.json()
  const { data: order } = await supabase.from("orders").select("*").eq("id", order_id).single()
  if (!order) return new Response(JSON.stringify({ error: "Order not found" }), { status: 404 })
  if (order.buyer_id !== user.id) return new Response(JSON.stringify({ error: "Forbidden" }), { status: 403 })
  // For demo: return a Razorpay checkout URL
  const gatewayOrderId = `rz_${Date.now()}`
  await supabase.from("orders").update({ payment_gateway: gateway, payment_transaction_id: gatewayOrderId }).eq("id", order_id)
  const checkoutUrl = `https://checkout.razorpay.com/v1/checkout?order_id=${gatewayOrderId}&amount=${Math.round(order.total_amount * 100)}&currency=INR`
  return new Response(JSON.stringify({ checkout_url: checkoutUrl, gateway_order_id: gatewayOrderId }), { headers: { "Content-Type": "application/json" } })
})
