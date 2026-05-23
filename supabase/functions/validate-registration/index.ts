import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

serve(async (req) => {
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!)
  const { device_id } = await req.json()
  if (!device_id) return new Response(JSON.stringify({ error: "device_id required" }), { status: 400 })
  const { count } = await supabase.from("device_accounts").select("*", { count: "exact", head: true }).eq("device_id", device_id)
  if ((count ?? 0) >= 2) {
    return new Response(JSON.stringify({ can_register: false, reason: "Maximum 2 accounts per device reached" }), { status: 200 })
  }
  return new Response(JSON.stringify({ can_register: true }), { headers: { "Content-Type": "application/json" } })
})
