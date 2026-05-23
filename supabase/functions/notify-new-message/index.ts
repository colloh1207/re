import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
}

/**
 * notify-new-message
 *
 * Called via a Supabase database webhook on INSERT to the `messages` table.
 * Sends a push notification to the recipient of the new message.
 *
 * Webhook setup in Supabase Dashboard:
 *   Table: messages
 *   Events: INSERT
 *   Type: HTTP Request
 *   URL: {SUPABASE_URL}/functions/v1/notify-new-message
 *   Headers: Authorization: Bearer {SERVICE_ROLE_KEY}
 */
serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  try {
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    )

    const payload = await req.json()

    // Support both direct call and Supabase webhook format
    const record = payload.record ?? payload
    const { id: message_id, chat_id, sender_id, content, type = "text" } = record

    if (!chat_id || !sender_id) {
      return new Response(
        JSON.stringify({ error: "chat_id and sender_id are required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // Get chat participants to find recipient(s)
    const { data: chat, error: chatError } = await supabase
      .from("chats")
      .select("participant_ids, product_id")
      .eq("id", chat_id)
      .single()

    if (chatError || !chat) {
      return new Response(
        JSON.stringify({ error: "Chat not found" }),
        { status: 404, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // Recipients = all participants except the sender
    const recipientIds: string[] = (chat.participant_ids ?? []).filter(
      (id: string) => id !== sender_id
    )

    if (recipientIds.length === 0) {
      return new Response(
        JSON.stringify({ success: true, sent: 0 }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // Get sender name
    const { data: sender } = await supabase
      .from("users")
      .select("full_name, avatar_url")
      .eq("id", sender_id)
      .single()

    const senderName = sender?.full_name ?? "Someone"

    // Build notification body based on message type
    const notifBody = (() => {
      switch (type) {
        case "image": return "📷 Sent you a photo"
        case "location": return "📍 Shared a location"
        case "offer": return "💰 Made you an offer"
        default:
          return content
            ? (content.length > 80 ? content.substring(0, 80) + "…" : content)
            : "Sent you a message"
      }
    })()

    // Send notifications to all recipients in parallel
    const results = await Promise.allSettled(
      recipientIds.map(async (recipient_id) => {
        // Check if recipient has this chat open (read status) — skip if last_read is very recent
        const { data: membership } = await supabase
          .from("chat_members")
          .select("is_muted")
          .eq("chat_id", chat_id)
          .eq("user_id", recipient_id)
          .single()

        if (membership?.is_muted) return { skipped: true, reason: "muted" }

        // Call the send-notification function
        const notifResponse = await supabase.functions.invoke("send-notification", {
          body: {
            user_id: recipient_id,
            type: "message",
            title: senderName,
            body: notifBody,
            vibrate: true,
            data: {
              chat_id,
              sender_id,
              message_id,
              product_id: chat.product_id ?? null,
              type: "message"
            }
          }
        })

        return notifResponse
      })
    )

    const sent = results.filter(r => r.status === "fulfilled").length

    return new Response(
      JSON.stringify({ success: true, sent, total: recipientIds.length }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  } catch (err) {
    return new Response(
      JSON.stringify({ error: err.message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  }
})
