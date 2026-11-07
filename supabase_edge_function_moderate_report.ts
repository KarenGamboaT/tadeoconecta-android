// MÓDULO 11 — Moderación con IA, parte 2/2: reporte del PROFESOR con motivo
// (pedido explícito de Juan: "como esas de Meta cuando uno reporta una
// cuenta"). Ver MODULO_11_IA_MODERACION.sql para la mitad de base de datos
// y INSTRUCCIONES_DEPLOY_IA_MODERACION.md para cómo desplegar esto — mismo
// mecanismo que generate-improvement-recommendations.ts (pegar en el
// Dashboard de Supabase, Edge Functions → Deploy a new function).
//
// Qué hace, en una frase: el profesor autenticado elige un MOTIVO para una
// reseña de SU PROPIO panel (nunca de las reseñas públicas — ese reporte
// sigue siendo el de report_professor_rating, con su umbral de 3), esta
// función le manda a Gemini el motivo + el texto real de esa reseña, y
// aplica el veredicto DE INMEDIATO — sin esperar a que nadie más reporte
// nada.
//
// Seguridad — DOS pasos, cada uno con el cliente que le corresponde (nunca
// se usa la llave de service role para LEER nada del profesor, solo para
// el paso final de ESCRIBIR el veredicto ya decidido):
//
// 1) Con el cliente "como el usuario" (ANON key + el JWT de quien llamó,
//    igual que generate-improvement-recommendations.ts) se llama a
//    get_professor_feedback(), que YA filtra por professor_id = auth.uid()
//    dentro de la base de datos. Si el rating_id pedido no aparece en esa
//    lista, la reseña no es del profesor autenticado (o ya no existe) — se
//    corta ahí, antes de gastar ni una llamada a Gemini.
//
// 2) Solo DESPUÉS de tener el veredicto real de Gemini se usa un cliente
//    con la llave de service role (secreto nuevo: SUPABASE_SERVICE_ROLE_KEY)
//    para llamar a resolve_professor_ai_report — esa función, a propósito,
//    NO tiene grant de ejecución para "authenticated" (ver el comentario de
//    diseño en MODULO_11_IA_MODERACION.sql): si lo tuviera, cualquier
//    profesor podría llamarla directo e inventarse un veredicto sin pasar
//    nunca por la IA. Service role es la única forma de llegar a ella, y
//    esta función es la única pieza que tiene esa llave.

import { createClient } from "jsr:@supabase/supabase-js@2";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

// Misma lista en ReportReason.kt (domain/model/ModerationModels.kt) — si
// cambian las claves o las etiquetas allá, cambien acá también a mano (no
// hay forma de compartir el enum de Kotlin con este archivo Deno).
const MOTIVO_LABELS: Record<string, string> = {
  ofensivo: "Contiene insultos o lenguaje ofensivo hacia el profesor",
  acoso: "Es acoso o busca intimidar",
  discriminacion: "Contiene discriminación u odio",
  falso: "El contenido es falso o no corresponde con la clase real",
  spam: "Es spam o contenido repetido / sin sentido",
  otro: "Otro motivo",
};

const CATEGORY_LABELS: Record<string, string> = {
  clarity: "Claridad explicando",
  organization: "Organización del curso",
  fairness: "Justicia al calificar",
  availability: "Disponibilidad fuera de clase",
  respect: "Trato al estudiante",
  atmosphere: "Ambiente de clase",
};

const RESPONSE_SCHEMA = {
  type: "object",
  properties: {
    bajar_comentario: { type: "boolean" },
    justificacion: { type: "string" },
  },
  required: ["bajar_comentario", "justificacion"],
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: CORS_HEADERS });

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) return jsonError("Falta autenticación.", 401);

    let body: { rating_id?: string; motivo?: string };
    try {
      body = await req.json();
    } catch {
      return jsonError("Cuerpo de la petición inválido.", 400);
    }

    const ratingId = body.rating_id;
    const motivo = body.motivo;
    if (!ratingId || !motivo) return jsonError("Falta rating_id o motivo.", 400);
    if (!(motivo in MOTIVO_LABELS)) return jsonError(`Motivo inválido: ${motivo}.`, 400);

    // Cliente "como el usuario" — mismo patrón que
    // generate-improvement-recommendations.ts: nunca permisos de admin acá.
    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
    const userClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: authHeader } },
    });

    const { data: userData, error: userError } = await userClient.auth.getUser();
    if (userError || !userData?.user) return jsonError("No se pudo identificar al profesor autenticado.", 401);
    const professorId = userData.user.id;

    const { data: rows, error: feedbackError } = await userClient.rpc("get_professor_feedback");
    if (feedbackError) return jsonError(`No se pudo leer tus resultados: ${feedbackError.message}`, 500);

    const row = ((rows ?? []) as ProfessorFeedbackRow[]).find((r) => r.id === ratingId);
    if (!row) return jsonError("Esa reseña no existe o no te pertenece.", 404);

    const apiKey = Deno.env.get("GEMINI_API_KEY");
    if (!apiKey) return jsonError("Falta configurar GEMINI_API_KEY como secreto del proyecto.", 500);

    const prompt = buildPrompt(motivo, row);

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

    const verdict = JSON.parse(text) as { bajar_comentario?: boolean; justificacion?: string };
    if (typeof verdict.bajar_comentario !== "boolean" || !verdict.justificacion) {
      return jsonError("Gemini devolvió un formato inesperado.", 502);
    }

    // Solo acá, con el veredicto YA decidido por Gemini, se usa la llave de
    // service role — y solo para este único insert/update, nunca para leer
    // nada del profesor (eso ya se leyó arriba con el cliente normal).
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (!serviceKey) return jsonError("Falta configurar SUPABASE_SERVICE_ROLE_KEY como secreto del proyecto.", 500);
    const adminClient = createClient(supabaseUrl, serviceKey);

    const { error: resolveError } = await adminClient.rpc("resolve_professor_ai_report", {
      p_rating_id: ratingId,
      p_professor_id: professorId,
      p_motivo: motivo,
      p_bajo_comentario: verdict.bajar_comentario,
      p_justificacion: verdict.justificacion,
    });
    if (resolveError) return jsonError(`No se pudo aplicar el veredicto: ${resolveError.message}`, 500);

    return jsonOk({ bajado: verdict.bajar_comentario, justificacion: verdict.justificacion });
  } catch (e) {
    return jsonError(`Error inesperado: ${e instanceof Error ? e.message : String(e)}`, 500);
  }
});

// Mismas columnas que expone get_professor_feedback() (select * vía
// security definer) — solo se listan las que esta función de verdad usa.
interface ProfessorFeedbackRow {
  id: string;
  category_answers: Record<string, number>;
  category_notes: Record<string, string | null>;
  would_recommend: boolean;
  recommend_reason: string;
  tip_comment: string;
  hardest_part_comment: string;
}

function buildPrompt(motivo: string, row: ProfessorFeedbackRow): string {
  const notes = Object.entries(row.category_notes ?? {})
    .filter(([, text]) => !!text?.trim())
    .map(([key, text]) => `- [${CATEGORY_LABELS[key] ?? key}] ${text!.trim()}`)
    .join("\n");

  return `Eres un moderador de contenido para TadeoConecta, una app donde estudiantes de la Universidad de Bogotá Jorge Tadeo Lozano evalúan a sus profesores de forma anónima.

Un PROFESOR está reportando, desde su propio panel privado, una reseña real que un estudiante le escribió. Tu trabajo es decidir si esa reseña viola las reglas de la comunidad lo suficiente para bajarla, o si el reporte no es válido.

MOTIVO QUE ELIGIÓ EL PROFESOR: ${MOTIVO_LABELS[motivo] ?? motivo}

TEXTO REAL DE LA RESEÑA:
- ¿La recomienda? ${row.would_recommend ? "Sí" : "No"}
- Por qué: ${row.recommend_reason?.trim() || "(sin texto)"}
- Tip para futuros estudiantes: ${row.tip_comment?.trim() || "(sin texto)"}
- Lo más difícil del curso: ${row.hardest_part_comment?.trim() || "(sin texto)"}
${notes ? `- Notas por categoría:\n${notes}` : ""}

REGLA CENTRAL — no la rompas: una crítica dura pero legítima a la labor docente ("explica muy mal", "nunca resuelve dudas", "es muy desorganizado", "sus exámenes no tienen que ver con lo que enseña", incluso "es el peor profesor que he tenido") NUNCA es motivo para bajar una reseña, sin importar qué tan negativa sea — esta app exige justamente ese tipo de comentario concreto cuando alguien califica mal a un profesor. Solo bajes la reseña cuando el texto en sí contiene insultos personales, lenguaje ofensivo, acoso, amenazas o discriminación dirigidos a la persona del profesor — no a su desempeño. Si el motivo que eligió el profesor no coincide con lo que el texto realmente dice (por ejemplo, reporta "contenido falso" pero el texto es solo una crítica dura y verificable), el reporte no es válido.

Responde con bajar_comentario (true solo si de verdad hay lenguaje ofensivo/acoso/discriminación/amenazas, o si el "motivo" de falso/spam se confirma con el texto; false si es una crítica dura pero legítima o si el motivo no se sostiene) y justificacion (2-3 frases en español, dirigidas al profesor, explicando tu decisión con base en el texto real de arriba — nunca inventes algo que no esté ahí). Escribe siempre en nombre del equipo de TadeoConecta, en primera persona del plural (p. ej. "Revisamos..."); nunca menciones que eres una IA, un modelo de lenguaje, un sistema automático ni nada parecido.`;
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
