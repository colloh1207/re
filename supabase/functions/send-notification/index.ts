import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

interface NotificationPayload {
  userId: string;
  type: "message" | "order" | "offer" | "rating" | "follow" | "sale" | "kyc" | "general";
  title: string;
  body: string;
  referenceId?: string;
  data?: Record<string, string>;
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const fcmServerKey = Deno.env.get("FCM_SERVER_KEY") ?? "";

    const supabase = createClient(supabaseUrl, serviceKey);
    const payload: NotificationPayload = await req.json();

    if (!payload.userId || !payload.type || !payload.title) {
      return new Response(
        JSON.stringify({ error: "userId, type, and title are required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 1. Store notification in DB
    const { error: insertError } = await supabase.from("notifications").insert({
      user_id: payload.userId,
      type: payload.type,
      title: payload.title,
      message: payload.body,
      reference_id: payload.referenceId ?? null,
      is_read: false,
    });
    if (insertError) console.error("Failed to insert notification:", insertError.message);

    // 2. Get FCM token + user notification settings
    const { data: user } = await supabase
      .from("users")
      .select("fcm_token, notification_settings")
      .eq("id", payload.userId)
      .single();

    if (!user) {
      return new Response(
        JSON.stringify({ success: true, push: false, reason: "user not found" }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 3. Check notification preferences
    const settings = user.notification_settings ?? {};
    if (settings.all === false || !isTypeEnabled(payload.type, settings)) {
      return new Response(
        JSON.stringify({ success: true, push: false, reason: "notifications disabled" }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const fcmToken = user.fcm_token;
    if (!fcmToken || !fcmServerKey) {
      return new Response(
        JSON.stringify({ success: true, push: false, reason: !fcmToken ? "no FCM token" : "FCM key not configured" }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 4. Send push via FCM
    const isHighPriority = ["message", "offer"].includes(payload.type);
    const fcmPayload = {
      to: fcmToken,
      notification: { title: payload.title, body: payload.body },
      data: {
        type: payload.type,
        reference_id: payload.referenceId ?? "",
        title: payload.title,
        body: payload.body,
        ...(payload.data ?? {}),
      },
      priority: isHighPriority ? "high" : "normal",
      android: {
        priority: isHighPriority ? "high" : "normal",
        notification: {
          channel_id: getChannelId(payload.type),
          sound: "default",
        },
      },
    };

    const fcmResponse = await fetch("https://fcm.googleapis.com/fcm/send", {
      method: "POST",
      headers: { Authorization: `key=${fcmServerKey}`, "Content-Type": "application/json" },
      body: JSON.stringify(fcmPayload),
    });

    const fcmResult = await fcmResponse.json();

    if (fcmResult.failure > 0) {
      console.error("FCM delivery failed:", JSON.stringify(fcmResult));
      if (fcmResult.results?.[0]?.error === "NotRegistered") {
        await supabase.from("users").update({ fcm_token: null }).eq("id", payload.userId);
      }
    }

    return new Response(
      JSON.stringify({ success: true, push: true, fcm: fcmResult }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err) {
    console.error("send-notification error:", err);
    return new Response(
      JSON.stringify({ error: "Internal server error", details: String(err) }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});

function isTypeEnabled(type: string, settings: Record<string, boolean>): boolean {
  switch (type) {
    case "message":          return settings.messages !== false;
    case "order": case "sale": return settings.orders !== false;
    case "offer":            return settings.offers !== false;
    case "rating":           return settings.ratings !== false;
    case "follow":           return settings.follows !== false;
    case "kyc":              return true;
    default:                 return settings.general !== false;
  }
}

function getChannelId(type: string): string {
  switch (type) {
    case "message":           return "sdd_messages";
    case "order": case "sale": return "sdd_orders";
    case "offer":             return "sdd_offers";
    case "rating":            return "sdd_ratings";
    default:                  return "sdd_general";
  }
}
