package co.edu.utadeo.tadeoconecta.domain.repository

import co.edu.utadeo.tadeoconecta.domain.model.AiRecommendation
import co.edu.utadeo.tadeoconecta.domain.model.AiReportVerdict
import co.edu.utadeo.tadeoconecta.domain.model.CategoryAnswers
import co.edu.utadeo.tadeoconecta.domain.model.CategoryNotes
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorFeedbackNote
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorRatingRow
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorRatingSummary
import co.edu.utadeo.tadeoconecta.domain.model.ReportReason
import co.edu.utadeo.tadeoconecta.domain.model.ReviewModerationVerdict

interface RatingRepository {
    // La calificación que el estudiante autenticado ya le puso a este
    // profesor, si existe — para precargar el formulario en modo edición.
    suspend fun myRating(professorId: String): ProfessorRatingRow?

    // Guarda (o actualiza, si ya existía) la evaluación por tarjetas del
    // estudiante autenticado para este profesor. RatingViewModel es quien
    // valida que esté todo lo obligatorio antes de llamar esto — acá solo
    // se envía.
    suspend fun submitRating(
        professorId: String,
        categoryAnswers: CategoryAnswers,
        categoryNotes: CategoryNotes,
        wouldRecommend: Boolean,
        recommendReason: String,
        recommendFor: List<String>,
        workload: String,
        attendanceMandatory: Boolean,
        gradingBasis: String,
        allowsReviewOfExams: Boolean,
        classStyle: String,
        tipComment: String,
        hardestPartComment: String
    )

    // Resumen agregado de UN profesor — ver summariesFor para varios a la vez.
    suspend fun summaryFor(professorId: String): ProfessorRatingSummary

    // Promedios/reseñas de varios profesores en una sola consulta (usado por
    // ProfessorRepository al armar resultados de búsqueda/favoritos, para no
    // hacer una consulta de ratings por cada profesor de la lista).
    suspend fun summariesFor(professorIds: List<String>): Map<String, ProfessorRatingSummary>

    // Moderación: cada estudiante que reporta una reseña (identificada por
    // su id propio, no por quién la escribió) queda registrado en
    // professor_rating_reports (vía RPC con security definer); la reseña
    // solo pasa a "reportada" — y por lo tanto oculta para todos menos
    // quien la escribió — cuando suficientes estudiantes DISTINTOS la
    // reportaron (ver MODULO_REVISION_PRIVACIDAD_REPORTES.sql). Un solo
    // click ya no basta, a propósito: antes cualquiera podía borrar de la
    // vista pública cualquier evaluación con un solo reporte.
    suspend fun reportReview(ratingId: String)

    // Módulo 11 — reporte del PROFESOR sobre una reseña de SU PROPIO panel
    // (nunca de las reseñas públicas — ese camino sigue siendo
    // reportReview() de arriba, con su umbral de 3). Un solo reporte, con
    // un motivo elegido (ver ReportReason), basta: la Edge Function
    // moderate-report le manda el motivo + el texto real de la reseña a
    // Gemini y aplica el veredicto de inmediato — no hace falta que nadie
    // más reporte nada. Puede lanzar (red, Gemini sin cuota, etc.); quien
    // llama decide qué mostrar si falla.
    suspend fun reportFeedbackWithReason(ratingId: String, reason: ReportReason): AiReportVerdict

    // Módulo 11 — se llama ANTES de submitRating(), nunca después: si la
    // IA determina que el texto que el estudiante está a punto de enviar
    // contiene lenguaje ofensivo (insultos, acoso, discriminación — NUNCA
    // una crítica dura pero legítima, eso sigue permitido), la fila jamás
    // se inserta en professor_ratings. form son los campos de texto libre
    // del formulario tal como están en RatingFormState — esta función solo
    // los empaqueta para la Edge Function moderate-review-text, no valida
    // nada más. Puede lanzar (red, Gemini sin cuota, etc.); quien llama
    // decide si eso debe bloquear el envío o dejarlo pasar.
    suspend fun moderateReviewText(
        categoryNotes: CategoryNotes,
        recommendReason: String,
        tipComment: String,
        hardestPartComment: String
    ): ReviewModerationVerdict

    // Módulo 08/09 — TODO lo que le han contestado al profesor AUTENTICADO,
    // completo (incluidas category_notes/recommend_reason). Vía RPC
    // get_professor_feedback(), que filtra por professor_id = auth.uid()
    // dentro de la base de datos — no por confiar en que esta app nunca se
    // lo pida para otro profesor.
    suspend fun professorFeedback(): List<ProfessorFeedbackNote>

    // Puntos 1+2 de la lista de Juan — le pide a la Edge Function
    // generate-improvement-recommendations que genere (con Gemini) 3-5
    // recomendaciones personalizadas para el profesor AUTENTICADO, a partir
    // de sus propios resultados. Sin parámetros: igual que
    // professorFeedback(), quién es "el profesor" lo decide la función en
    // el servidor a partir del JWT, nunca un id que mande la app. Puede
    // lanzar (red, cuota de la API agotada, etc.) — quien llama decide qué
    // mostrar si falla; NO hay caché local, cada llamada es una petición
    // nueva a propósito (ver el comentario en ProfessorDashboardViewModel
    // sobre por qué esto es a demanda y no automático).
    suspend fun generateImprovementRecommendations(): List<AiRecommendation>
}
