import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

serve(async (req) => {
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!)
  const authHeader = req.headers.get("Authorization")
  if (!authHeader) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
  const { data: { user } } = await supabase.auth.getUser(authHeader.replace("Bearer ", ""))
  if (!user) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
  const { blocked_id, reason } = await req.json()
  if (!blocked_id) return new Response(JSON.stringify({ error: "blocked_id required" }), { status: 400 })
  if (blocked_id === user.id) return new Response(JSON.stringify({ error: "Cannot block yourself" }), { status: 400 })
  const { error: blockError } = await supabase.from("blocks").upsert(
    { blocker_id: user.id, blocked_id, reason }, { onConflict: "blocker_id,blocked_id" }
  )
  if (blockError) return new Response(JSON.stringify({ error: blockError.message }), { status: 500 })
  // Block reverse chat access
  await supabase.from("chats").update({ is_blocked: true }).or(`and(participant1_id.eq.${user.id},participant2_id.eq.${blocked_id}),and(participant1_id.eq.${blocked_id},participant2_id.eq.${user.id})`)
  return new Response(JSON.stringify({ success: true }), { headers: { "Content-Type": "application/json" } })
})
