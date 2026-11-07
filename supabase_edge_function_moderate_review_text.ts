// MÓDULO 11 — Moderación con IA, parte 1/2: bloqueo al ESCRIBIR una reseña
// (pedido explícito de Juan). Ver INSTRUCCIONES_DEPLOY_IA_MODERACION.md
// para cómo desplegar esto — mismo mecanismo que
// generate-improvement-recommendations.ts (pegar en el Dashboard de
// Supabase, Edge Functions → Deploy a new function).
//
// Qué hace, en una frase: justo ANTES de guardar la evaluación (ver
// RatingViewModel.submit() → ratingRepository.moderateReviewText()), se le
// manda a Gemini el texto que el estudiante escribió y se decide si
// contiene lenguaje ofensivo — si es así, nunca se llega a insertar la
// fila en professor_ratings.
//
// Por qué es una función APARTE de moderate-report (y no comparten
// prompt): acá no hay ninguna reseña guardada todavía que verificar — el
// estudiante solo está escribiendo, así que esta función no toca ninguna
// tabla ni necesita ningún RPC, solo recibe el texto tal cual viene del
// formulario y responde con un veredicto. No hace falta llave de service
// role para esto.
//
// Seguridad: exige autenticación (cualquier estudiante autenticado puede
// llamarla) solo para que nadie gaste la cuota de Gemini sin estar logueado
// — el contenido evaluado es el que la propia app manda en el cuerpo,
// nunca se lee nada de la base de datos.
//
// REGLA CENTRAL, la misma que en moderate-report: una crítica dura pero
// legítima a la labor docente NUNCA se bloquea, sin importar qué tan
// negativa sea — eso es justo lo que este formulario le pide al estudiante
// cuando califica mal a un profesor (ver RatingCardOption.requiresNote en
// Rating.kt). Solo se bloquea insultos personales, lenguaje ofensivo,
// acoso, amenazas o discriminación dirigidos a la persona del profesor.

import { createClient } from "jsr:@supabase/supabase-js@2";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const RESPONSE_SCHEMA = {
  type: "object",
  properties: {
    ofensivo: { type: "boolean" },
    motivo: { type: "string" },
  },
  required: ["ofensivo", "motivo"],
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: CORS_HEADERS });

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) return jsonError("Falta autenticación.", 401);

    // Solo para confirmar que el token es de un usuario real — el
    // contenido a evaluar viene del cuerpo, nunca de una consulta acá.
    const userClient = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_ANON_KEY")!,
      { global: { headers: { Authorization: authHeader } } },
    );
    const { data: userData, error: userError } = await userClient.auth.getUser();
    if (userError || !userData?.user) return jsonError("No se pudo identificar al estudiante autenticado.", 401);

    let body: ReviewTextPayload;
    try {
      body = await req.json();
    } catch {
      return jsonError("Cuerpo de la petición inválido.", 400);
    }

    const apiKey = Deno.env.get("GEMINI_API_KEY");
    if (!apiKey) return jsonError("Falta configurar GEMINI_API_KEY como secreto del proyecto.", 500);

    const prompt = buildPrompt(body);

    const geminiRes = await fetch(
      // gemini-2.0-flash fue retirado (1 jun 2026) y gemini-2.5-flash ya no se
      // le da a cuentas nuevas. Se usa el alias "gemini-flash-lite-latest":
      // probado con la llave del proyecto, responde en ~1-2 s y clasifica bien
      // (gemini-flash-latest se quedaba "pensando" más de 25 s). La llave va
      // en el header x-goog-api-key (lo que soportan las llaves nuevas "AQ.").
      // Tope de 8 s: el cliente de la app corta a los 10 s, así que si Gemini
      // se cuelga esto falla rápido y la app sigue (fail-open) en vez de
      // quedarse esperando hasta el límite de 150 s de Supabase.
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent",
      {
        method: "POST",
        signal: AbortSignal.timeout(8000),
        headers: { "Content-Type": "application/json", "x-goog-api-key": apiKey },
        body: JSON.stringify({
          contents: [{ role: "user", parts: [{ text: prompt }] }],
          generationConfig: {
            responseMimeType: "application/json",
            responseSchema: RESPONSE_SCHEMA,
            temperature: 0.2,
          },
        }),
      },
    );

    if (!geminiRes.ok) {
      const detail = await geminiRes.text();
      return jsonError(`Gemini respondió con error (${geminiRes.status}): ${detail}`, 502);
    }

    const geminiBody = await geminiRes.json();
    const text = geminiBody?.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!text) return jsonError("Gemini no devolvió contenido.", 502);

    const verdict = JSON.parse(text) as { ofensivo?: boolean; motivo?: string };
    if (typeof verdict.ofensivo !== "boolean" || typeof verdict.motivo !== "string") {
      return jsonError("Gemini devolvió un formato inesperado.", 502);
    }

    return jsonOk({ ofensivo: verdict.ofensivo, motivo: verdict.motivo });
  } catch (e) {
    return jsonError(`Error inesperado: ${e instanceof Error ? e.message : String(e)}`, 500);
  }
});

// Mismos nombres de campo que RatingFormState en RatingViewModel.kt (ver
// moderateReviewText() en RatingRepositoryImpl.kt, que construye esto
// directo desde el formulario) — category_notes es un mapa clave→texto
// (solo las categorías que de verdad tengan algo escrito).
interface ReviewTextPayload {
  category_notes?: Record<string, string>;
  recommend_reason?: string;
  tip_comment?: string;
  hardest_part_comment?: string;
}

function buildPrompt(payload: ReviewTextPayload): string {
  const notes = Object.entries(payload.category_notes ?? {})
    .filter(([, text]) => !!text?.trim())
    .map(([key, text]) => `- [${key}] ${text.trim()}`)
    .join("\n");

  return `Eres un moderador de contenido para TadeoConecta, una app donde estudiantes de la Universidad de Bogotá Jorge Tadeo Lozano evalúan a sus profesores de forma anónima. Un estudiante está a punto de ENVIAR esta reseña — tu trabajo es decidir si contiene lenguaje ofensivo antes de que se guarde.

TEXTO QUE ESCRIBIÓ EL ESTUDIANTE:
- Por qué (lo)/(no lo) recomendaría: ${payload.recommend_reason?.trim() || "(sin texto)"}
- Tip para futuros estudiantes: ${payload.tip_comment?.trim() || "(sin texto)"}
- Lo más difícil del curso: ${payload.hardest_part_comment?.trim() || "(sin texto)"}
${notes ? `- Notas por categoría:\n${notes}` : ""}

REGLA CENTRAL — no la rompas: una crítica dura pero legítima a la labor docente ("explica muy mal", "nunca resuelve dudas", "es muy desorganizado", "sus exámenes no corresponden con lo que enseña", incluso "es el peor profesor que he tenido") NUNCA se marca como ofensiva, sin importar qué tan negativa o dura sea — esta app exige justamente ese tipo de comentario concreto y honesto cuando alguien califica mal a un profesor. SOLO marca ofensivo=true cuando el texto en sí contiene insultos personales, groserías dirigidas a la persona, lenguaje discriminatorio, acoso o amenazas — nunca por ser negativo, duro o informal.

Responde con ofensivo (true/false) y motivo: si ofensivo=true, 1-2 frases en español explicando qué parte del texto es el problema, dirigidas al estudiante para que pueda corregirlo (nunca cites el insulto textual, descríbelo en general); si ofensivo=false, puedes dejar motivo como una cadena vacía. Escribe siempre en nombre del equipo de TadeoConecta, en primera persona del plural (p. ej. "Revisamos..."); nunca menciones que eres una IA, un modelo de lenguaje, un sistema automático ni nada parecido.`;
}

function jsonOk(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
  });
}

function jsonError(message: string, status: number): Response {
  return new Response(JSON.stringify({ error: message }), {
    status,
    headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
  });
}
