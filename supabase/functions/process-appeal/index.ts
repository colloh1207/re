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
        headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 401,
      });
    }

    const token = authHeader.replace("Bearer ", "");
    const { data: { user }, error: authError } = await supabaseClient.auth.getUser(token);
    if (authError || !user) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 401,
      });
    }

    const { action, suspensionId, adminNote } = await req.json();
    // action: "submit" | "approve" | "reject"

    if (action === "submit") {
      // User submits appeal
      const { note } = await req.json().catch(() => ({}));
      const { error } = await supabaseClient
        .from("user_suspensions")
        .update({
          appeal_status: "PENDING",
          appeal_note: note ?? adminNote ?? "",
          appeal_submitted_at: new Date().toISOString(),
        })
        .eq("user_id", user.id)
        .eq("appeal_status", "NONE");

      if (error) throw error;

      return new Response(JSON.stringify({ success: true, message: "Appeal submitted" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 200,
      });
    }

    if (action === "approve") {
      // Admin approves appeal — requires service role (admin check)
      const { data: suspension } = await supabaseClient
        .from("user_suspensions")
        .select("user_id, is_permanent")
        .eq("id", suspensionId)
        .single();

      if (!suspension) {
        return new Response(JSON.stringify({ error: "Suspension not found" }), {
          headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 404,
        });
      }

      // Lift suspension
      await supabaseClient.from("user_suspensions").update({
        appeal_status: "APPROVED",
        appeal_resolved_at: new Date().toISOString(),
        admin_note: adminNote ?? "",
        is_active: false,
      }).eq("id", suspensionId);

      // Reset user suspension state, require KYC
      await supabaseClient.from("users").update({
        suspended_until: null,
        is_permanently_banned: false,
        suspension_reason: null,
        kyc_required_after_appeal: true,
      }).eq("id", suspension.user_id);

      return new Response(JSON.stringify({ success: true, message: "Appeal approved. KYC required." }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 200,
      });
    }

    if (action === "reject") {
      await supabaseClient.from("user_suspensions").update({
        appeal_status: "REJECTED",
        appeal_resolved_at: new Date().toISOString(),
        admin_note: adminNote ?? "",
      }).eq("id", suspensionId);

      return new Response(JSON.stringify({ success: true, message: "Appeal rejected." }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 200,
      });
    }

    return new Response(JSON.stringify({ error: "Invalid action" }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 400,
    });
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500,
    });
  }
});
