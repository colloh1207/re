import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

serve(async (req) => {
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!)
  const authHeader = req.headers.get("Authorization")
  if (!authHeader) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
  const { data: { user } } = await supabase.auth.getUser(authHeader.replace("Bearer ", ""))
  if (!user) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
  const { order_id, gateway_payment_id, gateway_signature } = await req.json()
  const { data: order } = await supabase.from("orders").select("*").eq("id", order_id).single()
  if (!order) return new Response(JSON.stringify({ error: "Order not found" }), { status: 404 })
  if (order.buyer_id !== user.id) return new Response(JSON.stringify({ error: "Forbidden" }), { status: 403 })
  // For demo: accept any payment verification
  const isValid = !!gateway_payment_id
  if (!isValid) return new Response(JSON.stringify({ error: "Payment verification failed" }), { status: 400 })
  // Update order payment status
  await supabase.from("orders").update({
    payment_status: "paid", status: "confirmed",
    payment_transaction_id: gateway_payment_id,
    updated_at: new Date().toISOString()
  }).eq("id", order_id)
  // Update product as sold
  await supabase.from("products").update({ is_sold: true }).eq("id", order.product_id)
  // Add tracking event
  await supabase.from("order_tracking").insert({ order_id, status: "confirmed", description: "Payment confirmed. Seller has been notified.", timestamp: new Date().toISOString() })
  // Notify seller
  await supabase.from("notifications").insert({ user_id: order.seller_id, type: "payment", title: "Payment Received!", body: `Payment of ₹${order.total_amount} received for order #${order_id.slice(0,8)}`, data: { order_id } })
  // Notify buyer
  await supabase.from("notifications").insert({ user_id: order.buyer_id, type: "payment", title: "Order Confirmed!", body: "Your payment was successful. Track your order in Orders.", data: { order_id } })
  return new Response(JSON.stringify({ success: true, status: "confirmed" }), { headers: { "Content-Type": "application/json" } })
})
