import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

interface NotificationPayload {
  // Accept both camelCase and snake_case for backward compatibility
  userId?: string;
  user_id?: string;
  type: "message" | "order" | "offer" | "rating" | "follow" | "sale" | "kyc" | "general" |
        "MESSAGE" | "ORDER_UPDATE" | "OFFER" | "REVIEW" | "FOLLOW" | "SALE" | "SYSTEM";
  title: string;
  body: string;
  referenceId?: string;
  reference_id?: string;
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

    // Accept both camelCase and snake_case userId / referenceId
    const userId = payload.userId ?? payload.user_id ?? "";
    const referenceId = payload.referenceId ?? payload.reference_id ?? null;

    if (!userId || !payload.type || !payload.title) {
      return new Response(
        JSON.stringify({ error: "userId (or user_id), type, and title are required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Map caller type to the schema CHECK constraint values
    const dbType = toSchemaType(payload.type);

    // 1. Store notification in DB (schema uses column "body", not "message")
    const { error: insertError } = await supabase.from("notifications").insert({
      user_id: userId,
      type: dbType,
      title: payload.title,
      body: payload.body,
      reference_id: referenceId,
      is_read: false,
    });
    if (insertError) console.error("Failed to insert notification:", insertError.message);

    // 2. Get FCM token + user notification settings
    const { data: user } = await supabase
      .from("users")
      .select("fcm_token, notification_settings")
      .eq("id", userId)
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
    const typeLower = payload.type.toLowerCase();
    const isHighPriority = typeLower === "message" || typeLower === "offer";
    const fcmPayload = {
      to: fcmToken,
      notification: { title: payload.title, body: payload.body },
      data: {
        type: typeLower,
        reference_id: referenceId ?? "",
        title: payload.title,
        body: payload.body,
        ...(payload.data ?? {}),
      },
      priority: isHighPriority ? "high" : "normal",
      android: {
        priority: isHighPriority ? "high" : "normal",
        notification: {
          channel_id: getChannelId(typeLower),
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
        await supabase.from("users").update({ fcm_token: null }).eq("id", userId);
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

/** Map incoming type string to the CHECK constraint values in the notifications table. */
function toSchemaType(type: string): string {
  switch (type.toLowerCase()) {
    case "message":                    return "MESSAGE";
    case "order": case "order_update": return "ORDER_UPDATE";
    case "offer":                      return "OFFER";
    case "rating": case "review":      return "REVIEW";
    case "follow":                     return "FOLLOW";
    case "sale":                       return "SALE";
    case "kyc": case "system":         return "SYSTEM";
    default:                           return "SYSTEM";
  }
}

function isTypeEnabled(type: string, settings: Record<string, boolean>): boolean {
  switch (type.toLowerCase()) {
    case "message":                     return settings.messages !== false;
    case "order": case "order_update": return settings.orders !== false;
    case "offer":                       return settings.offers !== false;
    case "rating": case "review":       return settings.ratings !== false;
    case "follow":                      return settings.follows !== false;
    case "kyc": case "system":          return true;
    default:                            return settings.general !== false;
  }
}

function getChannelId(type: string): string {
  switch (type.toLowerCase()) {
    case "message":                     return "sdd_messages";
    case "order": case "order_update":  return "sdd_orders";
    case "offer":                       return "sdd_offers";
    case "rating": case "review":       return "sdd_ratings";
    default:                            return "sdd_general";
  }
}
