import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  try {
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    )

    const { user_id, type, title, body, data = {}, vibrate = true } = await req.json()

    if (!user_id || !type || !title || !body) {
      return new Response(
        JSON.stringify({ error: "user_id, type, title, body are required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // Insert notification record
    await supabase.from("notifications").insert({
      user_id,
      type,
      title,
      body,
      data,
      is_read: false,
      created_at: new Date().toISOString()
    })

    // Fetch recipient FCM token
    const { data: profile, error: profileError } = await supabase
      .from("users")
      .select("fcm_token, full_name")
      .eq("id", user_id)
      .single()

    if (profileError || !profile?.fcm_token) {
      // Notification stored in DB, just no push token available
      return new Response(
        JSON.stringify({ success: true, pushed: false, reason: "No FCM token" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    const fcmKey = Deno.env.get("FCM_SERVER_KEY")
    if (!fcmKey) {
      return new Response(
        JSON.stringify({ success: true, pushed: false, reason: "FCM key not configured" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // Build FCM payload with Android-specific config for vibration + sound
    const fcmPayload = {
      to: profile.fcm_token,
      priority: "high",
      notification: {
        title,
        body,
        sound: "default",
        badge: 1,
        click_action: "FLUTTER_NOTIFICATION_CLICK"
      },
      android: {
        priority: "high",
        notification: {
          channel_id: type === "message" || type === "inbox" ? "messages" : type === "order" ? "orders" : "general",
          sound: "default",
          vibrate_timings: vibrate ? ["0s", "0.3s", "0.1s", "0.3s"] : [],
          default_vibrate_timings: vibrate,
          notification_priority: "PRIORITY_HIGH",
          visibility: "PUBLIC",
          icon: "ic_notification",
          color: "#FF4D82"
        },
        ttl: "86400s"
      },
      apns: {
        payload: {
          aps: {
            sound: "default",
            badge: 1,
            "content-available": 1
          }
        },
        headers: {
          "apns-priority": "10"
        }
      },
      data: {
        ...data,
        type,
        click_action: getClickAction(type, data)
      }
    }

    const fcmResponse = await fetch("https://fcm.googleapis.com/fcm/send", {
      method: "POST",
      headers: {
        "Authorization": `key=${fcmKey}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify(fcmPayload)
    })

    const fcmResult = await fcmResponse.json()

    // Handle token expiry — clean up invalid tokens
    if (fcmResult.failure > 0 && fcmResult.results?.[0]?.error === "NotRegistered") {
      await supabase.from("users").update({ fcm_token: null }).eq("id", user_id)
    }

    return new Response(
      JSON.stringify({ success: true, pushed: true, fcm: fcmResult }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  } catch (err) {
    return new Response(
      JSON.stringify({ error: err.message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  }
})

function getClickAction(type: string, data: Record<string, unknown>): string {
  switch (type) {
    case "message":
    case "inbox":
      return data.chat_id ? `chat/${data.chat_id}` : "inbox"
    case "order":
      return data.order_id ? `order/${data.order_id}` : "orders"
    case "review":
      return "profile"
    default:
      return "home"
  }
}
