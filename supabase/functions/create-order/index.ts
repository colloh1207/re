import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

serve(async (req) => {
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!)
  const authHeader = req.headers.get("Authorization")
  if (!authHeader) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
  const { data: { user } } = await supabase.auth.getUser(authHeader.replace("Bearer ", ""))
  if (!user) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
  const body = await req.json()
  const { product_id, quantity, shipping_address, payment_method_id, buyer_terms_accepted } = body
  if (!buyer_terms_accepted) return new Response(JSON.stringify({ error: "Buyer must accept terms" }), { status: 400 })
  const { data: product } = await supabase.from("products").select("*").eq("id", product_id).single()
  if (!product) return new Response(JSON.stringify({ error: "Product not found" }), { status: 404 })
  if (product.is_sold) return new Response(JSON.stringify({ error: "Product already sold" }), { status: 409 })
  if (product.seller_id === user.id) return new Response(JSON.stringify({ error: "Cannot buy your own product" }), { status: 400 })
  const unitPrice = product.price
  const platformFee = unitPrice * quantity * 0.07
  const totalAmount = unitPrice * quantity + platformFee
  const { data: order, error } = await supabase.from("orders").insert({
    buyer_id: user.id, seller_id: product.seller_id, product_id, quantity,
    unit_price: unitPrice, total_amount: totalAmount, currency: product.currency,
    status: "pending", payment_status: "pending", payment_method_id,
    shipping_address, buyer_terms_accepted, buyer_terms_accepted_at: new Date().toISOString()
  }).select().single()
  if (error) return new Response(JSON.stringify({ error: error.message }), { status: 500 })
  // Insert initial tracking event
  await supabase.from("order_tracking").insert({ order_id: order.id, status: "pending", description: "Order placed successfully", timestamp: new Date().toISOString() })
  // Notify seller
  await supabase.from("notifications").insert({ user_id: product.seller_id, type: "sale", title: "New Order!", body: `You received a new order for ${product.title}`, data: { order_id: order.id } })
  return new Response(JSON.stringify(order), { headers: { "Content-Type": "application/json" } })
})
