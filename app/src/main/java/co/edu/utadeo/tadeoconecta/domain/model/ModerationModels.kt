package co.edu.utadeo.tadeoconecta.domain.model

import kotlinx.serialization.Serializable

// Módulo 11 — Moderación con IA (pedido explícito de Juan): dos piezas
// nuevas, cada una con su propia Edge Function (ver supabase_edge_function_
// moderate_report.ts y _moderate_review_text.ts) — nunca llaman a Gemini
// directo, mismo motivo que AiRecommendation en ProfessorAiInsight.kt (la
// llave de la API nunca debe quedar en el APK).

// Motivo fijo que el PROFESOR elige al reportar una reseña de SU propio
// panel — como reportar una cuenta en Meta/Instagram. Mismas claves (en
// inglés simplificado/español, exactas) que MOTIVO_LABELS en
// supabase_edge_function_moderate_report.ts: si cambian acá, cambien allá
// también a mano (no hay forma de compartir esto entre Kotlin y Deno).
enum class ReportReason(val key: String, val label: String) {
    OFFENSIVE("ofensivo", "Contiene insultos o lenguaje ofensivo hacia mí"),
    HARASSMENT("acoso", "Es acoso o busca intimidar"),
    DISCRIMINATION("discriminacion", "Contiene discriminación u odio"),
    FALSE_CONTENT("falso", "El contenido es falso o no corresponde con la clase real"),
    SPAM("spam", "Es spam o contenido repetido / sin sentido"),
    OTHER("otro", "Otro motivo")
}

// Lo que responde la Edge Function moderate-report una vez la IA ya
// decidió — ver RatingRepository.reportFeedbackWithReason(). bajado=true
// significa que professor_ratings.reported ya quedó en true (la reseña ya
// no aparece ni en este panel ni en las reseñas públicas); bajado=false
// significa que la IA determinó que el reporte no es válido y la reseña
// sigue visible tal cual estaba.
@Serializable
data class AiReportVerdict(
    val bajado: Boolean,
    val justificacion: String
)

// Lo que responde la Edge Function moderate-review-text ANTES de guardar
// una reseña — ver RatingRepository.moderateReviewText(). Si ofensivo es
// true, RatingViewModel.submit() nunca llega a llamar submitRating(): la
// fila jamás se inserta. motivo es la explicación en español que se le
// muestra al estudiante para que pueda corregir su texto (vacío cuando
// ofensivo es false).
@Serializable
data class ReviewModerationVerdict(
    val ofensivo: Boolean,
    val motivo: String = ""
)
