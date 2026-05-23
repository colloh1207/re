import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
}

const GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"

interface ProductAnalysis {
  isIllegal: boolean
  isSuspicious: boolean
  violationType: string | null
  confidence: number
  reason: string
}

async function analyzeProductWithGroq(
  title: string,
  description: string,
  category: string
): Promise<ProductAnalysis> {
  const groqApiKey = Deno.env.get("GROQ_API_KEY")
  if (!groqApiKey) throw new Error("GROQ_API_KEY not configured")

  const prompt = `You are a marketplace content moderation AI. Determine if this product listing violates platform policies.

Title: "${title.replace(/"/g, '\\"')}"
Category: "${category}"
Description: "${description.substring(0, 800).replace(/"/g, '\\"')}"

Prohibited items:
1. Illegal weapons, firearms, ammunition, explosives
2. Controlled substances, narcotics, or drug paraphernalia
3. Counterfeit goods, fake branded items
4. Stolen property
5. Explicit sexual content
6. Human trafficking or exploitation
7. Pirated software, cracked apps, or copyright violations
8. Prescription medicine or regulated health products
9. Protected wildlife or animal products
10. Hacking tools, malware, or surveillance software

Respond ONLY with valid JSON, no extra text:
{"isIllegal":false,"isSuspicious":false,"violationType":null,"confidence":0.0,"reason":"appears legitimate"}`

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
    return JSON.parse(text) as ProductAnalysis
  } catch {
    return { isIllegal: false, isSuspicious: false, violationType: null, confidence: 0, reason: "parse_error" }
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
    const product_id: string | undefined = payload.product_id ?? payload.id ?? payload.record?.id
    if (!product_id) {
      return new Response(
        JSON.stringify({ error: "product_id required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // Step 2: Fetch the authoritative record from DB (never trust body fields)
    const { data: productRow, error: productErr } = await supabase
      .from("products")
      .select("id, seller_id, title, description, category")
      .eq("id", product_id)
      .single()

    if (productErr || !productRow) {
      return new Response(
        JSON.stringify({ error: "product not found" }),
        { status: 404, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // Step 3: Ownership check — caller must be the seller who listed this product
    if (productRow.seller_id !== user.id) {
      return new Response(
        JSON.stringify({ error: "Forbidden" }),
        { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    const { seller_id, title, description, category } = productRow

    if (!title) {
      return new Response(
        JSON.stringify({ error: "product has no title" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    const analysis = await analyzeProductWithGroq(title, description ?? "", category ?? "")

    // Step 4: Service-role mutations — only reached after auth + ownership verified
    await supabase.from("product_moderation_logs").insert({
      product_id,
      seller_id,
      is_illegal: analysis.isIllegal,
      is_suspicious: analysis.isSuspicious,
      violation_type: analysis.violationType,
      confidence: analysis.confidence,
      reason: analysis.reason,
    })

    if (!analysis.isIllegal && !analysis.isSuspicious) {
      await supabase
        .from("products")
        .update({ moderation_status: "approved" })
        .eq("id", product_id)

      return new Response(
        JSON.stringify({ safe: true, analysis }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    if (analysis.isIllegal) {
      await supabase
        .from("products")
        .update({ moderation_status: "removed", is_active: false })
        .eq("id", product_id)

      const { count: priorCount } = await supabase
        .from("product_moderation_logs")
        .select("id", { count: "exact", head: true })
        .eq("seller_id", seller_id)
        .eq("is_illegal", true)
        .neq("product_id", product_id)
        .gte("created_at", new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString())

      const priorViolations = priorCount ?? 0

      if (priorViolations >= 2) {
        await supabase.from("suspensions").insert({
          user_id: seller_id,
          reason: `Repeated illegal product listings (${priorViolations + 1} violations). Latest: ${analysis.reason} [${analysis.violationType}]`,
          type: "PERMANENT",
          issued_by: "ai_moderation",
        })
      } else if (priorViolations === 1) {
        const endsAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString()
        await supabase.from("suspensions").insert({
          user_id: seller_id,
          reason: `Illegal listing removed (2nd violation): ${analysis.reason}. KYC re-verification required to restore posting.`,
          type: "TEMPORARY",
          ends_at: endsAt,
          issued_by: "ai_moderation",
        })
        await supabase
          .from("users")
          .update({ kyc_flagged: true })
          .eq("id", seller_id)
      } else {
        const endsAt = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString()
        await supabase.from("suspensions").insert({
          user_id: seller_id,
          reason: `Illegal product listing removed: ${analysis.reason}. Repeated violations will result in permanent ban.`,
          type: "TEMPORARY",
          ends_at: endsAt,
          issued_by: "ai_moderation",
        })
      }

      await supabase.functions.invoke("send-notification", {
        body: {
          user_id: seller_id,
          type: "moderation",
          title: "Listing Removed",
          body: `"${title.substring(0, 40)}" was removed for violating our policies: ${analysis.reason}`,
          data: { type: "product_removed", product_id, violation: analysis.violationType },
        },
      })
    } else if (analysis.isSuspicious) {
      await supabase
        .from("products")
        .update({ moderation_status: "under_review" })
        .eq("id", product_id)

      await supabase.functions.invoke("send-notification", {
        body: {
          user_id: seller_id,
          type: "moderation",
          title: "Listing Under Review",
          body: `"${title.substring(0, 40)}" is being reviewed by our team. We'll notify you within 24 hours.`,
          data: { type: "product_under_review", product_id },
        },
      })
    }

    return new Response(
      JSON.stringify({
        flagged: true,
        isIllegal: analysis.isIllegal,
        isSuspicious: analysis.isSuspicious,
        violationType: analysis.violationType,
      }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  } catch (err) {
    console.error("moderate-product error:", err)
    return new Response(
      JSON.stringify({ error: err.message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  }
})
