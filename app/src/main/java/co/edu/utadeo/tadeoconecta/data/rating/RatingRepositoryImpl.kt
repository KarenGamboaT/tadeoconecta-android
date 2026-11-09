package co.edu.utadeo.tadeoconecta.data.rating

import co.edu.utadeo.tadeoconecta.core.network.SupabaseProvider
import co.edu.utadeo.tadeoconecta.domain.model.AiRecommendation
import co.edu.utadeo.tadeoconecta.domain.model.AiRecommendationsResponse
import co.edu.utadeo.tadeoconecta.domain.model.AiReportVerdict
import co.edu.utadeo.tadeoconecta.domain.model.AttendanceBands
import co.edu.utadeo.tadeoconecta.domain.model.CategoryAnswers
import co.edu.utadeo.tadeoconecta.domain.model.CategoryBreakdown
import co.edu.utadeo.tadeoconecta.domain.model.CategoryNotes
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorFeedbackNote
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorRatingInsert
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorRatingRow
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorRatingSummary
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorReview
import co.edu.utadeo.tadeoconecta.domain.model.RatingCards
import co.edu.utadeo.tadeoconecta.domain.model.RatingCategory
import co.edu.utadeo.tadeoconecta.domain.model.ReportReason
import co.edu.utadeo.tadeoconecta.domain.model.ReviewModerationVerdict
import co.edu.utadeo.tadeoconecta.domain.repository.AuthRepository
import co.edu.utadeo.tadeoconecta.domain.repository.RatingRepository
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.call.body
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

// Columnas que CUALQUIER autenticado puede leer de professor_ratings (ver
// MODULO_08_09_PANEL_PROFESOR.sql + MODULO_REVISION_PRIVACIDAD_REPORTES.sql):
// a propósito NO incluye category_notes ni recommend_reason — esas dos ya no
// tienen grant de select para el rol authenticated, así que pedirlas acá con
// un "select *" haría fallar la consulta con "permission denied for
// column...". Quien de verdad necesita esas dos columnas las pide por RPC
// (get_my_rating / get_professor_feedback), que valida fila por fila quién
// puede verlas. TAMPOCO incluye student_id (a propósito, desde la revisión
// general): ese id viajaba a cualquier estudiante que leyera reseñas
// públicas, y como profiles.select está abierto a todo authenticated,
// cualquiera podía cruzarlo y saber quién escribió cada comentario. Se usa
// "id" (el id propio de la fila) para poder reportar sin exponer quién
// escribió qué.
private val PUBLIC_RATING_COLUMNS = Columns.list(
    "id", "professor_id", "category_answers", "would_recommend", "recommend_for",
    "workload", "attendance_mandatory", "attendance_band", "grading_basis", "allows_review_of_exams",
    "class_style", "tip_comment", "hardest_part_comment", "reported", "created_at", "updated_at"
)

class RatingRepositoryImpl(
    private val authRepository: AuthRepository
) : RatingRepository {
    private val supabase get() = SupabaseProvider.client

    // Vía RPC (no select directo): category_notes/recommend_reason de la
    // propia fila ya no son legibles con un select normal para nadie, ni
    // siquiera para quien la escribió — get_my_rating() es la única forma
    // de recuperarlas, y solo devuelve algo cuando student_id = auth.uid().
    override suspend fun myRating(professorId: String): ProfessorRatingRow? {
        authRepository.currentUserId() ?: return null
        return supabase.postgrest.rpc(
            function = "get_my_rating",
            parameters = buildJsonObject { put("p_professor_id", professorId) }
        ).decodeList<ProfessorRatingRow>().firstOrNull()
    }

    // Mismo patrón upsert-por-llave-compuesta que student_careers/student_favorites:
    // la primary key (student_id, professor_id) en Supabase es lo que de verdad
    // impide calificar dos veces — volver a enviar la pareja actualiza la fila.
    override suspend fun submitRating(
        professorId: String,
        categoryAnswers: CategoryAnswers,
        categoryNotes: CategoryNotes,
        wouldRecommend: Boolean,
        recommendReason: String,
        recommendFor: List<String>,
        workload: String,
        attendanceMandatory: Boolean,
        attendanceBand: String,
        gradingBasis: String,
        allowsReviewOfExams: Boolean,
        classStyle: String,
        tipComment: String,
        hardestPartComment: String
    ) {
        val userId = authRepository.currentUserId()
            ?: error("No hay una sesión autenticada.")

        supabase.from("professor_ratings").upsert(
            ProfessorRatingInsert(
                student_id = userId,
                professor_id = professorId,
                category_answers = categoryAnswers,
                category_notes = categoryNotes,
                would_recommend = wouldRecommend,
                recommend_reason = recommendReason.trim(),
                recommend_for = recommendFor,
                workload = workload,
                attendance_mandatory = attendanceMandatory,
                attendance_band = attendanceBand,
                grading_basis = gradingBasis,
                allows_review_of_exams = allowsReviewOfExams,
                class_style = classStyle,
                tip_comment = tipComment.trim(),
                hardest_part_comment = hardestPartComment.trim()
            )
        ) {
            onConflict = "student_id,professor_id"
        }

        // Módulo 07 — dos notificaciones reales por cada evaluación: al
        // profesor (algo nuevo que revisar) y al propio estudiante
        // (confirmación de que sí quedó registrada). Van después del
        // upsert, nunca antes: si el upsert falla, submit() ya lanza y no
        // debe llegar a notificar nada que en realidad no se guardó.
        // runCatching a propósito: que fallen las notificaciones (p.ej. un
        // problema de red justo después de guardar) no debe deshacer ni
        // mostrar error por algo que, de cara al usuario, ya se completó —
        // lo que de verdad importa (la fila en professor_ratings) sí quedó.
        runCatching {
            supabase.postgrest.rpc(
                function = "create_notification",
                parameters = buildJsonObject {
                    put("p_user_id", professorId)
                    put("p_type", "new_review")
                    put("p_title", "Nueva evaluación recibida")
                    put("p_body", "Un estudiante evaluó tu desempeño como docente. Entra a Resultados para verla.")
                }
            )
        }
        runCatching {
            supabase.postgrest.rpc(
                function = "create_notification",
                parameters = buildJsonObject {
                    put("p_user_id", userId)
                    put("p_type", "review_submitted")
                    put("p_title", "Evaluación enviada")
                    put("p_body", "Tu evaluación fue registrada correctamente. Gracias por tu opinión.")
                }
            )
        }
    }

    override suspend fun summaryFor(professorId: String): ProfessorRatingSummary =
        summariesFor(listOf(professorId))[professorId] ?: ProfessorRatingSummary.EMPTY

    // Una sola consulta a professor_ratings para todos los ids pedidos; todo
    // el agregado (promedios, % que recomienda, conteos de datos rápidos) se
    // calcula acá en Kotlin a partir de las filas crudas, igual que el resto
    // de este repo — no hay funciones de agregación en Supabase.
    override suspend fun summariesFor(professorIds: List<String>): Map<String, ProfessorRatingSummary> {
        if (professorIds.isEmpty()) return emptyMap()

        val rows = supabase.from("professor_ratings")
            .select(columns = PUBLIC_RATING_COLUMNS) { filter { isIn("professor_id", professorIds) } }
            .decodeList<ProfessorRatingRow>()
            .groupBy { it.professor_id }

        return professorIds.associateWith { summaryFrom(rows[it].orEmpty()) }
    }

    // Módulo 12 — cada reseña pesa según la asistencia que declaró quien la
    // escribió (ver AttendanceBands): promedios y % que recomienda son
    // PONDERADOS, y las de asistencia "mínima" (peso 0) se guardan pero no
    // cuentan en nada de esto — ni en el conteo, ni en los datos rápidos, ni
    // aparece su comentario. Las reseñas viejas (sin rango) pesan 1.0, así
    // que para ellas todo da igual que antes.
    private fun summaryFrom(allRows: List<ProfessorRatingRow>): ProfessorRatingSummary {
        val rows = allRows.filter { AttendanceBands.weight(it.attendance_band) > 0.0 }
        if (rows.isEmpty()) return ProfessorRatingSummary.EMPTY
        val rowWeights = rows.map { AttendanceBands.weight(it.attendance_band) }
        val totalWeight = rowWeights.sum()

        val categoryAverages = RatingCategory.entries.associateWith { category ->
            val cards = RatingCards.byCategory.getValue(category)
            val weightedSum = rows.indices.sumOf { i -> cards[rows[i].category_answers.get(category)].weight * rowWeights[i] }
            val counts = IntArray(4)
            rows.forEach { counts[it.category_answers.get(category)]++ }
            CategoryBreakdown(average = weightedSum / totalWeight, cardCounts = counts.toList())
        }
        val overall = categoryAverages.values.map { it.average }.average()

        fun <T> countBy(pick: (ProfessorRatingRow) -> T): Map<T, Int> =
            rows.groupingBy(pick).eachCount()

        // category_notes y recommend_reason de cada fila NO entran acá a
        // propósito (ver el comentario de ProfessorReview): esos textos
        // quedan en `rows` / en la base de datos, pero nunca se arman en el
        // modelo que ReviewsScreen lee.
        val reviews = rows
            .sortedByDescending { it.created_at.orEmpty() }
            .map { row ->
                ProfessorReview(
                    ratingId = row.id,
                    overall = row.overallWeight(),
                    wouldRecommend = row.would_recommend,
                    tipComment = row.tip_comment,
                    hardestPartComment = row.hardest_part_comment
                )
            }

        return ProfessorRatingSummary(
            reviewCount = rows.size,
            averageOverall = overall,
            categoryAverages = categoryAverages,
            recommendPercent = (rows.indices.sumOf { i -> if (rows[i].would_recommend) rowWeights[i] else 0.0 } * 100.0 / totalWeight).toInt(),
            recommendForCounts = rows.flatMap { it.recommend_for }.groupingBy { it }.eachCount(),
            workloadCounts = countBy { it.workload },
            attendanceMandatoryCount = rows.count { it.attendance_mandatory },
            gradingBasisCounts = countBy { it.grading_basis },
            allowsReviewOfExamsCount = rows.count { it.allows_review_of_exams },
            classStyleCounts = countBy { it.class_style },
            reviews = reviews
        )
    }

    // RPC con security definer (mismo patrón que redeem_professor_code): la
    // RLS normal solo deja a cada estudiante editar SU PROPIA fila, así que
    // reportar la reseña de otro necesita saltarse esa restricción puntual.
    // Ya no recibe professorId/studentId: el id de la fila alcanza, y así
    // nunca hace falta conocer quién la escribió para reportarla. El RPC
    // registra el reporte y solo oculta la reseña cuando varios estudiantes
    // distintos la reportaron (ver MODULO_REVISION_PRIVACIDAD_REPORTES.sql).
    override suspend fun reportReview(ratingId: String) {
        supabase.postgrest.rpc(
            function = "report_professor_rating",
            parameters = buildJsonObject {
                put("p_rating_id", ratingId)
            }
        )
    }

    // Módulo 11 — a diferencia de reportReview() (RPC directo, umbral de 3
    // reportes anónimos), esto llama a la Edge Function moderate-report:
    // ella es quien verifica que la reseña sea del profesor autenticado,
    // le pide el veredicto a Gemini y lo aplica — acá solo se manda la
    // petición y se decodifica la respuesta.
    override suspend fun reportFeedbackWithReason(ratingId: String, reason: ReportReason): AiReportVerdict {
        val response = supabase.functions.invoke(
            function = "moderate-report",
            body = buildJsonObject {
                put("rating_id", ratingId)
                put("motivo", reason.key)
            },
            headers = Headers.build { append(HttpHeaders.ContentType, "application/json") }
        )
        return response.body<AiReportVerdict>()
    }

    // Módulo 11 — llama a moderate-review-text ANTES de que
    // RatingViewModel.submit() llegue a llamar submitRating(). Solo manda
    // los campos de texto libre que el estudiante escribió, nunca nada que
    // ya esté guardado (esta reseña puede ni siquiera existir todavía).
    override suspend fun moderateReviewText(
        categoryNotes: CategoryNotes,
        recommendReason: String,
        tipComment: String,
        hardestPartComment: String,
        question: String?,
        attendanceLabel: String?
    ): ReviewModerationVerdict {
        val response = supabase.functions.invoke(
            function = "moderate-review-text",
            body = buildJsonObject {
                put("category_notes", Json.encodeToJsonElement(categoryNotes))
                put("recommend_reason", recommendReason.trim())
                put("tip_comment", tipComment.trim())
                put("hardest_part_comment", hardestPartComment.trim())
                if (!question.isNullOrBlank()) put("pregunta", question)
                if (!attendanceLabel.isNullOrBlank()) put("asistencia", attendanceLabel)
            },
            headers = Headers.build { append(HttpHeaders.ContentType, "application/json") }
        )
        return response.body<ReviewModerationVerdict>()
    }

    // Módulo 08/09 — vía RPC get_professor_feedback(), que ya filtra
    // server-side por professor_id = auth.uid(): acá solo se arma el
    // modelo de presentación (overallWeight, categoría → nota, etc.) a
    // partir de lo que la base de datos ya garantizó que es del profesor
    // autenticado.
    override suspend fun professorFeedback(): List<ProfessorFeedbackNote> {
        val rows = supabase.postgrest.rpc(function = "get_professor_feedback")
            .decodeList<ProfessorRatingRow>()
        return rows
            .sortedByDescending { it.created_at.orEmpty() }
            .map { row ->
                // Revisión general — qué tarjeta se eligió en cada categoría
                // decide si su nota es "crítica" (tarjeta mala, obligatoria)
                // o "positiva" (tarjeta buena, opcional) — ver
                // OptionalMultilineField en MainShell.kt. Antes category_notes
                // solo existía para tarjetas críticas; ahora puede venir de
                // cualquiera, así que ya no basta con "¿hay texto?".
                val critical = mutableMapOf<RatingCategory, String>()
                val positive = mutableMapOf<RatingCategory, String>()
                RatingCategory.entries.forEach { category ->
                    val note = row.category_notes.get(category)?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
                    val index = row.category_answers.get(category)
                    val isCritical = index >= 0 && RatingCards.byCategory.getValue(category)[index].requiresNote
                    if (isCritical) critical[category] = note else positive[category] = note
                }
                ProfessorFeedbackNote(
                    studentId = row.student_id,
                    ratingId = row.id,
                    createdAt = row.created_at,
                    overall = row.overallWeight(),
                    categoryAnswers = row.category_answers,
                    criticalNotes = critical,
                    positiveNotes = positive,
                    wouldRecommend = row.would_recommend,
                    recommendReason = row.recommend_reason,
                    tipComment = row.tip_comment,
                    hardestPartComment = row.hardest_part_comment,
                    attendanceBand = row.attendance_band
                )
            }
    }

    // Puntos 1+2 de la lista de Juan — ver el comentario de esta función en
    // RatingRepository. La Edge Function lee el JWT del header de
    // autorización (que el plugin Functions reenvía solo porque Auth ya
    // tiene la sesión activa, igual que Postgrest con las RPC de arriba),
    // así que nunca hace falta mandarle un professorId — ella decide quién
    // es "el profesor" del mismo modo que get_professor_feedback().
    override suspend fun generateImprovementRecommendations(
        updateReason: String?,
        previousPlan: List<AiRecommendation>
    ): AiRecommendationsResponse {
        val response = if (updateReason.isNullOrBlank()) {
            supabase.functions.invoke(function = "generate-improvement-recommendations")
        } else {
            supabase.functions.invoke(
                function = "generate-improvement-recommendations",
                body = buildJsonObject {
                    put("motivo_actualizacion", updateReason.trim())
                    put("plan_anterior", Json.encodeToJsonElement(previousPlan))
                },
                headers = Headers.build { append(HttpHeaders.ContentType, "application/json") }
            )
        }
        return response.body<AiRecommendationsResponse>()
    }
}
