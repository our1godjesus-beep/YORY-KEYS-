import { createClient } from "jsr:@supabase/supabase-js@2.119.1";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function getPublishableKey(): string | null {
  const keyMap = Deno.env.get("SUPABASE_PUBLISHABLE_KEYS");
  if (keyMap) {
    try {
      const parsed = JSON.parse(keyMap) as Record<string, string>;
      if (parsed.default) return parsed.default;
      const first = Object.values(parsed).find((value) => typeof value === "string");
      if (first) return first;
    } catch {
      // Fall through to compatibility variables.
    }
  }
  return Deno.env.get("SUPABASE_PUBLISHABLE_KEY") ?? Deno.env.get("SUPABASE_ANON_KEY") ?? null;
}

Deno.serve(async (request: Request) => {
  if (request.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const authorization = request.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) {
    return json({ error: "A signed-in user session is required." }, 401);
  }

  const url = Deno.env.get("SUPABASE_URL");
  const publishableKey = getPublishableKey();
  if (!url || !publishableKey) {
    return json({ error: "Backend environment is not configured." }, 500);
  }

  const length = Number(request.headers.get("content-length") ?? "0");
  if (length > 128_000) return json({ error: "Theme payload is too large." }, 413);

  let payload: Record<string, unknown>;
  try {
    payload = await request.json();
  } catch {
    return json({ error: "Request body must be valid JSON." }, 400);
  }

  const name = typeof payload.name === "string" ? payload.name.trim() : "";
  const theme = payload.theme_json;
  if (name.length < 1 || name.length > 48) {
    return json({ error: "Theme name must contain 1 to 48 characters." }, 400);
  }
  if (!theme || typeof theme !== "object" || Array.isArray(theme)) {
    return json({ error: "theme_json must be an object." }, 400);
  }
  const themeObject = theme as Record<string, unknown>;
  if (themeObject.schemaVersion !== 1 || !themeObject.appearance || typeof themeObject.appearance !== "object") {
    return json({ error: "Unsupported or invalid YORY theme schema." }, 400);
  }
  const encodedSize = new TextEncoder().encode(JSON.stringify(themeObject)).length;
  if (encodedSize > 96_000) return json({ error: "Theme document exceeds the 96 KB limit." }, 413);

  const isPublic = payload.is_public === true;
  const previewPath = typeof payload.preview_path === "string" ? payload.preview_path : null;

  const supabase = createClient(url, publishableKey, {
    global: { headers: { Authorization: authorization } },
    auth: { persistSession: false, autoRefreshToken: false, detectSessionInUrl: false },
  });

  const token = authorization.slice("Bearer ".length);
  const { data: userData, error: userError } = await supabase.auth.getUser(token);
  const user = userData.user;
  if (userError || !user) return json({ error: "Session is invalid or expired." }, 401);

  if (previewPath && !previewPath.startsWith(user.id + "/")) {
    return json({ error: "Preview files must be stored in the owner's folder." }, 400);
  }

  // The request keeps the caller JWT, so database RLS remains in force.
  const { data, error } = await supabase
    .from("themes")
    .insert({
      owner_id: user.id,
      name,
      schema_version: 1,
      theme_json: themeObject,
      is_public: isPublic,
      preview_path: previewPath,
    })
    .select("id, name, is_public, created_at")
    .single();

  if (error) return json({ error: "Theme could not be published.", detail: error.message }, 400);
  return json({ theme: data }, 201);
});
