import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

serve(async (req) => {
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!)
  const authHeader = req.headers.get("Authorization")
  if (!authHeader) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
  const { data: { user } } = await supabase.auth.getUser(authHeader.replace("Bearer ", ""))
  if (!user) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
  const { data: profile } = await supabase.from("users").select("joined_at").eq("id", user.id).single()
  const joinedAt = new Date(profile?.joined_at ?? Date.now())
  const thirtyDaysAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)
  const isNewUser = joinedAt > thirtyDaysAgo
  const dailyLimit = isNewUser ? 2 : 20
  const today = new Date().toISOString().split("T")[0]
  const { count } = await supabase.from("products").select("*", { count: "exact", head: true })
    .eq("seller_id", user.id).gte("created_at", today)
  if ((count ?? 0) >= dailyLimit) {
    return new Response(JSON.stringify({ can_post: false, reason: `Daily limit of ${dailyLimit} listings reached${isNewUser ? " (new seller limit)" : ""}` }), { status: 200 })
  }
  return new Response(JSON.stringify({ can_post: true, remaining: dailyLimit - (count ?? 0) }), { headers: { "Content-Type": "application/json" } })
})
