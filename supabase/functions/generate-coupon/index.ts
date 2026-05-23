import { serve } from "https://deno.land/x/sift@0.6.0/mod.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(JSON.stringify({ error: "No authorization header" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 401,
      });
    }

    const token = authHeader.replace("Bearer ", "");
    const { data: { user }, error: authError } = await supabaseClient.auth.getUser(token);
    if (authError || !user) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 401,
      });
    }

    const { orderId } = await req.json();
    if (!orderId) {
      return new Response(JSON.stringify({ error: "orderId is required" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 400,
      });
    }

    // Check order belongs to user and is delivered
    const { data: order, error: orderError } = await supabaseClient
      .from("orders")
      .select("id, buyer_id, status, total_amount")
      .eq("id", orderId)
      .eq("buyer_id", user.id)
      .single();

    if (orderError || !order) {
      return new Response(JSON.stringify({ error: "Order not found" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 404,
      });
    }

    if (order.status !== "DELIVERED" && order.status !== "CONFIRMED_RECEIVED") {
      return new Response(JSON.stringify({ error: "Order must be delivered to claim coupon" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 400,
      });
    }

    // Check coupon hasn't already been issued for this order
    const { data: existing } = await supabaseClient
      .from("coupons")
      .select("id")
      .eq("source_order_id", orderId)
      .single();

    if (existing) {
      return new Response(JSON.stringify({ error: "Coupon already issued for this order" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 409,
      });
    }

    // Generate unique coupon code
    const code = `THANKS${Math.random().toString(36).substring(2, 8).toUpperCase()}`;
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + 30); // 30 days validity

    const discountValue = Math.min(Math.round(order.total_amount * 0.10), 500); // 10% up to ₹500

    const { data: coupon, error: couponError } = await supabaseClient
      .from("coupons")
      .insert({
        code,
        user_id: user.id,
        discount_type: "PERCENTAGE",
        discount_value: 10,
        max_discount_amount: discountValue,
        min_order_amount: 0,
        is_used: false,
        expires_at: expiresAt.toISOString(),
        source_order_id: orderId,
        description: "Thank you for your purchase! Enjoy 10% off your next order.",
        created_at: new Date().toISOString(),
      })
      .select()
      .single();

    if (couponError) {
      throw couponError;
    }

    // Mark order as received
    await supabaseClient
      .from("orders")
      .update({ status: "CONFIRMED_RECEIVED", updated_at: new Date().toISOString() })
      .eq("id", orderId);

    return new Response(JSON.stringify({ success: true, coupon }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 200,
    });
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 500,
    });
  }
});
