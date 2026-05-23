import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
}

const GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"

interface ModerationResult {
  isScam: boolean
  isSuspicious: boolean
  threatType: string | null
  confidence: number
  reason: string
}

async function analyzeMessageWithGroq(content: string): Promise<ModerationResult> {
  const groqApiKey = Deno.env.get("GROQ_API_KEY")
  if (!groqApiKey) throw new Error("GROQ_API_KEY not configured")

  const prompt = `You are a marketplace safety AI. Analyze this chat message for scam or fraud activity.

Message: "${content.replace(/"/g, '\\"')}"

Check for:
1. Requests to pay outside the platform (WhatsApp/Telegram payment, wire transfer, gift cards)
2. Phishing links or suspicious URLs
3. Requests for personal information (banking details, passwords, ID numbers, OTPs)
4. Fake escrow or fake delivery services
5. Urgency manipulation ("send money now or lose deal")
6. Impersonation of platform support or admin
7. Advance-fee fraud patterns

Respond ONLY in this exact JSON format with no extra text:
{"isScam":false,"isSuspicious":false,"threatType":null,"confidence":0.0,"reason":"clean message"}`

  const response = await fetch(GROQ_API_URL, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${groqApiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: "llama-3.1-8b-instant",
      messages: [{ role: "user", content: prompt }],
      temperature: 0.1,
      max_tokens: 150,
      response_format: { type: "json_object" },
    }),
  })

  if (!response.ok) {
    const err = await response.text()
    throw new Error(`Groq API error ${response.status}: ${err}`)
  }

  const data = await response.json()
  const text = data.choices?.[0]?.message?.content?.trim() ?? "{}"

  try {
    return JSON.parse(text) as ModerationResult
  } catch {
    return { isScam: false, isSuspicious: false, threatType: null, confidence: 0, reason: "parse_error" }
  }
}

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders })

  const supabaseUrl = Deno.env.get("SUPABASE_URL")!
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY")!
  const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!

  try {
    // Step 1: Verify caller JWT — reject unauthenticated requests
    const authHeader = req.headers.get("Authorization")
    if (!authHeader?.startsWith("Bearer ")) {
      return new Response(
        JSON.stringify({ error: "Authentication required" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    const userClient = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } },
    })
    const { data: { user }, error: authError } = await userClient.auth.getUser()
    if (authError || !user) {
      return new Response(
        JSON.stringify({ error: "Unauthorized" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // Privileged client used only after identity and ownership are confirmed
    const supabase = createClient(supabaseUrl, supabaseServiceKey)

    const payload = await req.json()
    const message_id: string | undefined = payload.message_id ?? payload.record?.id
    if (!message_id) {
      return new Response(
        JSON.stringify({ error: "message_id required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // Step 2: Fetch the authoritative record from DB (never trust body content)
    const { data: msgRow, error: msgErr } = await supabase
      .from("messages")
      .select("id, chat_id, sender_id, content, type")
      .eq("id", message_id)
      .single()

    if (msgErr || !msgRow) {
      return new Response(
        JSON.stringify({ skipped: true, reason: "message not found" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // Step 3: Ownership check — caller must be the sender of the message
    if (msgRow.sender_id !== user.id) {
      return new Response(
        JSON.stringify({ error: "Forbidden" }),
        { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    const { chat_id, sender_id, content, type } = msgRow

    if (!content || type !== "text" || !sender_id) {
      return new Response(
        JSON.stringify({ skipped: true, reason: "non-text or empty message" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    const analysis = await analyzeMessageWithGroq(content)

    if (!analysis.isScam && !analysis.isSuspicious) {
      return new Response(
        JSON.stringify({ safe: true, confidence: analysis.confidence }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // Step 4: Service-role mutations — only reached after auth + ownership verified
    await supabase
      .from("messages")
      .update({ is_flagged: true, flag_reason: analysis.threatType })
      .eq("id", message_id)

    await supabase.from("message_warnings").insert({
      message_id,
      chat_id,
      sender_id,
      threat_type: analysis.threatType,
      confidence: analysis.confidence,
      reason: analysis.reason,
      is_scam: analysis.isScam,
    })

    const { count: recentCount } = await supabase
      .from("message_warnings")
      .select("id", { count: "exact", head: true })
      .eq("sender_id", sender_id)
      .eq("is_scam", true)
      .gte("created_at", new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString())

    const violations = recentCount ?? 0

    if (analysis.isScam && violations >= 3) {
      const endsAt = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString()
      await supabase.from("suspensions").insert({
        user_id: sender_id,
        reason: `AI detected ${violations} scam messages in 7 days. Latest: ${analysis.reason}`,
        type: "TEMPORARY",
        ends_at: endsAt,
        issued_by: "ai_moderation",
      })
      await supabase.functions.invoke("send-notification", {
        body: {
          user_id: sender_id,
          type: "moderation",
          title: "Account Temporarily Suspended",
          body: "Your account has been suspended for 24 hours due to repeated suspicious messaging.",
          data: { type: "suspension" },
        },
      })
      if (chat_id) {
        await supabase.from("messages").insert({
          chat_id,
          sender_id: null,
          content: "⚠️ A message in this conversation was removed and the sender's account has been suspended for suspicious activity.",
          type: "system",
        })
      }
    } else if (analysis.isScam) {
      if (chat_id) {
        await supabase.from("messages").insert({
          chat_id,
          sender_id: null,
          content: `⚠️ Safety Warning: A potentially fraudulent message was detected. Never share payment details or personal information outside this platform. (${violations}/3 violations before suspension)`,
          type: "system",
        })
      }
    } else if (analysis.isSuspicious) {
      if (chat_id) {
        await supabase.from("messages").insert({
          chat_id,
          sender_id: null,
          content: "ℹ️ Reminder: Always complete transactions through the official payment system. Never pay outside the platform.",
          type: "system",
        })
      }
    }

    return new Response(
      JSON.stringify({ flagged: true, isScam: analysis.isScam, violations, threatType: analysis.threatType }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  } catch (err) {
    console.error("moderate-message error:", err)
    return new Response(
      JSON.stringify({ error: err.message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  }
})
