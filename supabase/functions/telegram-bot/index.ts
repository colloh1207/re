import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const ENCRYPTION_KEY = Deno.env.get("TELEGRAM_ENCRYPTION_KEY") ?? "";
const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

async function encryptToken(token: string): Promise<string> {
  const encoder = new TextEncoder();
  const keyData = encoder.encode(ENCRYPTION_KEY.padEnd(32, "0").slice(0, 32));
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const key = await crypto.subtle.importKey("raw", keyData, { name: "AES-GCM" }, false, ["encrypt"]);
  const encrypted = await crypto.subtle.encrypt({ name: "AES-GCM", iv }, key, encoder.encode(token));
  const combined = new Uint8Array(iv.length + encrypted.byteLength);
  combined.set(iv, 0);
  combined.set(new Uint8Array(encrypted), iv.length);
  return btoa(String.fromCharCode(...combined));
}

async function validateTelegramToken(token: string): Promise<{ valid: boolean; botUsername?: string; botId?: string }> {
  try {
    const response = await fetch(`https://api.telegram.org/bot${token}/getMe`);
    const data = await response.json();
    if (data.ok) {
      return { valid: true, botUsername: data.result.username, botId: String(data.result.id) };
    }
    return { valid: false };
  } catch {
    return { valid: false };
  }
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401, headers: corsHeaders });
    }

    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_KEY);
    const userSupabase = createClient(SUPABASE_URL, Deno.env.get("SUPABASE_ANON_KEY") ?? "", {
      global: { headers: { Authorization: authHeader } }
    });

    const { data: { user }, error: userError } = await userSupabase.auth.getUser();
    if (userError || !user) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401, headers: corsHeaders });
    }

    const kycCheck = await supabase.from("kyc_documents").select("status").eq("user_id", user.id).eq("status", "approved").maybeSingle();
    if (!kycCheck.data) {
      return new Response(JSON.stringify({ error: "KYC verification required to use Telegram integration." }), { status: 403, headers: corsHeaders });
    }

    const body = await req.json();
    const { action, token } = body;

    if (action === "connect") {
      if (!token || typeof token !== "string") {
        return new Response(JSON.stringify({ success: false, error: "Invalid token format." }), { headers: corsHeaders });
      }

      const tokenPattern = /^\d+:[A-Za-z0-9_-]{35,}$/;
      if (!tokenPattern.test(token)) {
        return new Response(JSON.stringify({ success: false, error: "Token format invalid. Expected: 1234567890:ABCdef..." }), { headers: corsHeaders });
      }

      const validation = await validateTelegramToken(token);
      if (!validation.valid) {
        return new Response(JSON.stringify({ success: false, error: "Bot token is invalid or bot is not accessible." }), { headers: corsHeaders });
      }

      const encryptedToken = await encryptToken(token);

      await supabase.from("telegram_connections").upsert({
        user_id: user.id,
        encrypted_token: encryptedToken,
        bot_username: validation.botUsername,
        bot_id: validation.botId,
        connected_at: new Date().toISOString(),
        is_active: true
      }, { onConflict: "user_id" });

      return new Response(JSON.stringify({ success: true, botUsername: validation.botUsername }), { headers: corsHeaders });
    }

    if (action === "disconnect") {
      await supabase.from("telegram_connections").update({ is_active: false, encrypted_token: null }).eq("user_id", user.id);
      return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
    }

    if (action === "webhook") {
      const { update } = body;
      if (!update?.message) {
        return new Response(JSON.stringify({ ok: true }), { headers: corsHeaders });
      }

      const chatId = update.message.chat.id;
      const text = (update.message.text ?? "").trim();
      const connection = await supabase.from("telegram_connections").select("user_id, encrypted_token").eq("bot_id", body.botId).eq("is_active", true).maybeSingle();

      if (!connection.data) {
        return new Response(JSON.stringify({ ok: true }), { headers: corsHeaders });
      }

      const userId = connection.data.user_id;
      let replyText = "";

      if (text.startsWith("/help")) {
        replyText = "📦 *Sdd Marketplace Bot*\n\nAvailable commands:\n/post - Post a new product\n/mylistings - View your listings\n/sold [id] - Mark product as sold\n/delete [id] - Delete a listing\n/help - Show this message";
      } else if (text.startsWith("/mylistings")) {
        const { data: products } = await supabase.from("products").select("id, title, price, is_sold").eq("seller_id", userId).eq("is_deleted", false).limit(10);
        if (!products || products.length === 0) {
          replyText = "You have no active listings.";
        } else {
          replyText = "📋 *Your Listings:*\n\n" + products.map((p: any) => `• ${p.title} - ₹${p.price} ${p.is_sold ? "(SOLD)" : ""}\n  ID: \`${p.id}\``).join("\n\n");
        }
      } else if (text.startsWith("/sold ")) {
        const productId = text.replace("/sold ", "").trim();
        await supabase.from("products").update({ is_sold: true }).eq("id", productId).eq("seller_id", userId);
        replyText = `✅ Product marked as sold!`;
      } else if (text.startsWith("/delete ")) {
        const productId = text.replace("/delete ", "").trim();
        await supabase.from("products").update({ is_deleted: true }).eq("id", productId).eq("seller_id", userId);
        replyText = `🗑 Product deleted successfully.`;
      } else if (text.startsWith("/post")) {
        replyText = "📸 To post a product, send details in this format:\n\n*Title:* Your Product Title\n*Price:* 999\n*Category:* Electronics\n*Condition:* New\n*Description:* Brief description\n\nThen send up to 5 photos.";
      } else {
        replyText = "Unknown command. Send /help to see available commands.";
      }

      if (replyText) {
        const botTokenRow = await supabase.from("telegram_connections").select("encrypted_token").eq("user_id", userId).eq("is_active", true).maybeSingle();
        if (botTokenRow.data?.encrypted_token) {
          await fetch(`https://api.telegram.org/bot${token}/sendMessage`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ chat_id: chatId, text: replyText, parse_mode: "Markdown" })
          });
        }
      }

      return new Response(JSON.stringify({ ok: true }), { headers: corsHeaders });
    }

    return new Response(JSON.stringify({ error: "Unknown action" }), { status: 400, headers: corsHeaders });
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), { status: 500, headers: corsHeaders });
  }
});
