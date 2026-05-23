import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

serve(async (req) => {
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!)
  const authHeader = req.headers.get("Authorization")
  if (!authHeader) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
  const { data: { user } } = await supabase.auth.getUser(authHeader.replace("Bearer ", ""))
  if (!user) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
  const body = await req.json()
  const { reported_user_id, reported_product_id, category, description, evidence_urls = [] } = body
  if (!reported_user_id && !reported_product_id) return new Response(JSON.stringify({ error: "Target required" }), { status: 400 })
  const { data, error } = await supabase.from("reports").insert({
    reporter_id: user.id, reported_user_id, reported_product_id,
    category, description, evidence_urls, status: "pending"
  }).select().single()
  if (error) return new Response(JSON.stringify({ error: error.message }), { status: 500 })
  // Auto-suspend user if 5+ reports
  if (reported_user_id) {
    const { count } = await supabase.from("reports").select("*", { count: "exact", head: true }).eq("reported_user_id", reported_user_id).eq("status", "pending")
    if ((count ?? 0) >= 5) {
      await supabase.from("users").update({ is_suspended: true }).eq("id", reported_user_id)
    }
  }
  return new Response(JSON.stringify({ success: true, report: data }), { headers: { "Content-Type": "application/json" } })
})
