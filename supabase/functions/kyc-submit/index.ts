import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

serve(async (req) => {
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!)
  const authHeader = req.headers.get("Authorization")
  if (!authHeader) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
  const { data: { user } } = await supabase.auth.getUser(authHeader.replace("Bearer ", ""))
  if (!user) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
  const body = await req.json()
  const { document_type, document_number, front_url, back_url, selfie_url } = body
  if (!document_type || !front_url) return new Response(JSON.stringify({ error: "document_type and front_url required" }), { status: 400 })
  // Upsert KYC document
  const { data: kyc, error } = await supabase.from("kyc_documents").upsert({
    user_id: user.id, document_type, document_number, front_url, back_url, selfie_url,
    status: "pending", submitted_at: new Date().toISOString()
  }, { onConflict: "user_id,document_type" }).select().single()
  if (error) return new Response(JSON.stringify({ error: error.message }), { status: 500 })
  // Update user KYC status
  await supabase.from("users").update({ kyc_status: "pending" }).eq("id", user.id)
  // Create notification for admin review
  await supabase.from("notifications").insert({
    user_id: user.id, type: "kyc", title: "KYC Submitted",
    body: "Your identity documents have been submitted for review. We'll notify you within 48 hours.",
    data: { kyc_id: kyc.id }
  })
  return new Response(JSON.stringify({ success: true, kyc }), { headers: { "Content-Type": "application/json" } })
})
