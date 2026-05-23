import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

serve(async (req) => {
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!)
  const authHeader = req.headers.get("Authorization")
  if (!authHeader) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
  const { data: { user } } = await supabase.auth.getUser(authHeader.replace("Bearer ", ""))
  if (!user) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
  const { product_id } = await req.json()
  // Check account age (must be 3 weeks old)
  const { data: profile } = await supabase.from("users").select("joined_at").eq("id", user.id).single()
  if (!profile) return new Response(JSON.stringify({ can_review: false, reason: "Profile not found" }), { status: 200 })
  const joinedAt = new Date(profile.joined_at)
  const threeWeeksAgo = new Date(Date.now() - 21 * 24 * 60 * 60 * 1000)
  if (joinedAt > threeWeeksAgo) return new Response(JSON.stringify({ can_review: false, reason: "Account must be at least 3 weeks old to write reviews" }), { status: 200 })
  // Check no existing review
  const { count } = await supabase.from("reviews").select("*", { count: "exact", head: true }).eq("reviewer_id", user.id).eq("product_id", product_id)
  if ((count ?? 0) > 0) return new Response(JSON.stringify({ can_review: false, reason: "You have already reviewed this product" }), { status: 200 })
  return new Response(JSON.stringify({ can_review: true }), { headers: { "Content-Type": "application/json" } })
})
