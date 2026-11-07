// MÓDULO 10 — Recomendaciones de mejora con IA (puntos 1+2 de la lista de
// Juan). Esto NO es un archivo .sql como el resto de supabase/ — es el
// código de una Edge Function de Supabase (Deno + TypeScript). Ver
// INSTRUCCIONES_DEPLOY.md (al lado de este archivo) para los pasos exactos
// de cómo ponerlo a correr — resumen: se crea y se pega en el Dashboard de
// Supabase (Edge Functions → Deploy a new function), no hace falta la CLI.
//
// Qué hace, en una frase: lee los resultados REALES del profesor
// autenticado (nunca los de otro — ver más abajo por qué), se los manda a
// Gemini pidiendo 3-5 recomendaciones concretas en JSON, y devuelve eso tal
// cual a la app.
//
// Por qué esto vive en una Edge Function y no en la app directamente: la
// app nunca debe tener la llave de la API de Gemini (quedaría visible para
// cualquiera que descompile el APK). La llave vive SOLO como secreto de
// este proyecto de Supabase (GEMINI_API_KEY) y esta función es la única
// pieza que la usa.
//
// Seguridad — mismo patrón que get_professor_feedback() (ver
// MODULO_08_09_PANEL_PROFESOR.sql): esta función NO recibe ni confía en un
// professorId que mande la app. Crea su propio cliente de Supabase con el
// JWT de quien hizo la petición (el header Authorization que supabase-kt
// ya reenvía solo) y llama a esa misma RPC — como esa RPC ya filtra por
// professor_id = auth.uid() DENTRO de la base de datos, es imposible que
// esta función termine mandándole a Gemini (o devolviéndole a la app) los
// resultados de un profesor distinto al que está autenticado, aunque
// alguien manipulara la petición.

import { createClient } from "jsr:@supabase/supabase-js@2";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

// Mismo umbral que MIN_REVIEWS_TO_SHOW_COMMENTS en RatingViewModel.kt — con
// pocas evaluaciones, pedirle a la IA que "encuentre un patrón" sería
// inventarse algo sobre 1-2 comentarios sueltos. No hay forma de compartir
// la constante de Kotlin con este archivo Deno, así que si cambian una
// cambien la otra a mano.
const MIN_REVIEWS_FOR_AI = 5;

// Mismos pesos que RatingCards.WEIGHTS en Rating.kt (índice de tarjeta
// elegida → nota: 0=mejor…3=peor). Se repiten acá porque el promedio por
// categoría hay que recalcularlo del lado de la función — Supabase no
// tiene una vista que ya lo traiga calculado.
const CARD_WEIGHTS = [5, 4, 2.5, 1];

const CATEGORY_KEYS = ["clarity", "organization", "fairness", "availability", "respect", "atmosphere"] as const;
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
    recommendations: {
      type: "array",
      items: {
        type: "object",
        properties: {
          categoria: { type: "string", nullable: true, enum: [...CATEGORY_KEYS, null] },
          titulo: { type: "string" },
          recomendacion: { type: "string" },
        },
        required: ["titulo", "recomendacion"],
      },
    },
  },
  required: ["recommendations"],
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: CORS_HEADERS });

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return jsonError("Falta autenticación.", 401);
    }

    // Cliente "como el usuario": usa la ANON key del proyecto + el JWT de
    // quien llamó, así que toda RLS/RPC de abajo corre exactamente con los
    // mismos permisos que tendría la app — nunca con permisos de admin.
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_ANON_KEY")!,
      { global: { headers: { Authorization: authHeader } } },
    );

    const { data: rows, error } = await supabase.rpc("get_professor_feedback");
    if (error) return jsonError(`No se pudo leer tus resultados: ${error.message}`, 500);

    const feedback = (rows ?? []) as ProfessorFeedbackRow[];

    if (feedback.length < MIN_REVIEWS_FOR_AI) {
      return jsonOk({
        recommendations: [{
          categoria: null,
          titulo: "Todavía reuniendo datos",
          recomendacion:
            `Necesitas al menos ${MIN_REVIEWS_FOR_AI} evaluaciones para poder encontrar un patrón real en tus resultados en vez de reaccionar a uno o dos comentarios sueltos. Vuelve a intentarlo cuando tengas más.`,
        }],
      });
    }

    const apiKey = Deno.env.get("GEMINI_API_KEY");
    if (!apiKey) return jsonError("Falta configurar GEMINI_API_KEY como secreto del proyecto.", 500);

    const prompt = buildPrompt(feedback);

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
            temperature: 0.4,
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

    const parsed = JSON.parse(text);
    if (!Array.isArray(parsed?.recommendations)) {
      return jsonError("Gemini devolvió un formato inesperado.", 502);
    }

    return jsonOk(parsed);
  } catch (e) {
    return jsonError(`Error inesperado: ${e instanceof Error ? e.message : String(e)}`, 500);
  }
});

// Forma de cada fila que devuelve get_professor_feedback() — ver
// ProfessorRatingRow/ProfessorFeedbackNote en el código Kotlin. Solo se
// listan los campos que esta función de verdad usa; a propósito NO se
// reenvía student_id a Gemini en ningún lado de este archivo, aunque venga
// en la fila — el prompt nunca debe poder identificar a quién escribió qué.
interface ProfessorFeedbackRow {
  category_answers: Record<string, number>;
  category_notes: Record<string, string | null>;
  would_recommend: boolean;
  recommend_reason: string;
  tip_comment: string;
  hardest_part_comment: string;
}

function buildPrompt(feedback: ProfessorFeedbackRow[]): string {
  const categoryLines = CATEGORY_KEYS.map((key) => {
    const indices = feedback.map((row) => row.category_answers?.[key]).filter((i) => typeof i === "number" && i >= 0);
    const average = indices.length
      ? indices.reduce((sum, i) => sum + CARD_WEIGHTS[i], 0) / indices.length
      : null;
    const critical = indices.filter((i) => i >= 2).length;
    return `- ${CATEGORY_LABELS[key]} (${key}): promedio ${average?.toFixed(1) ?? "sin datos"} / 5, ${critical} de ${feedback.length} marcaron una tarjeta crítica aquí.`;
  }).join("\n");

  // Hasta 15 textos por campo — acota el tamaño del prompt (y el costo) sin
  // perder la idea de "varios estudiantes dijeron algo parecido", que es
  // justo lo que un promedio por categoría no puede mostrar.
  const take = <T,>(arr: T[], n = 15) => arr.slice(0, n);
  const criticalNotes = take(
    CATEGORY_KEYS.flatMap((key) =>
      feedback
        .map((row) => row.category_notes?.[key])
        .filter((n): n is string => !!n?.trim())
        .map((n) => `[${CATEGORY_LABELS[key]}] ${n.trim()}`)
    ),
  );
  const negativeReasons = take(
    feedback.filter((r) => !r.would_recommend).map((r) => r.recommend_reason?.trim()).filter(Boolean),
  );
  const hardestParts = take(feedback.map((r) => r.hardest_part_comment?.trim()).filter(Boolean));

  return `Eres un asesor pedagógico que ayuda a un profesor universitario a mejorar, a partir de evaluaciones ANÓNIMAS reales de sus propios estudiantes (TadeoConecta, Universidad de Bogotá Jorge Tadeo Lozano).

PROMEDIOS POR CATEGORÍA (escala 1-5, de ${feedback.length} evaluaciones):
${categoryLines}

COMENTARIOS DE JUSTIFICACIÓN EN TARJETAS CRÍTICAS (por qué marcaron mal esa categoría):
${criticalNotes.length ? criticalNotes.map((n) => `- ${n}`).join("\n") : "(ninguno)"}

RAZONES DE QUIENES NO LO RECOMENDARÍAN:
${negativeReasons.length ? negativeReasons.map((n) => `- ${n}`).join("\n") : "(ninguna)"}

"LO MÁS DIFÍCIL DEL CURSO" SEGÚN LOS ESTUDIANTES:
${hardestParts.length ? hardestParts.map((n) => `- ${n}`).join("\n") : "(ninguno)"}

Genera entre 3 y 5 recomendaciones de mejora, en español, concretas y accionables — basadas en patrones reales de los datos de arriba, no en consejos genéricos que aplicarían a cualquier profesor. Cuando una recomendación responda a una categoría puntual, usa su "categoria" (clave en inglés, p.ej. "clarity"); si es un consejo general, usa categoria=null. El tono debe ser constructivo y respetuoso — esto lo va a leer el profesor sobre su propio desempeño, no un tercero. Nunca inventes datos que no estén en el texto de arriba. Escribe siempre en nombre del equipo de TadeoConecta, en primera persona del plural (p. ej. "Revisamos..."); nunca menciones que eres una IA, un modelo de lenguaje, un sistema automático ni nada parecido.`;
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
