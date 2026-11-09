package co.edu.utadeo.tadeoconecta.domain.model

import java.time.OffsetDateTime
import kotlinx.serialization.Serializable

// Módulo 12 — plan de mejora ACEPTADO por el profesor, con historial. Vive en
// professor_improvement_plans (ver MODULO_12_ASISTENCIA_Y_PLANES.sql). Un
// plan activo guarda las recomendaciones, una "foto" de los promedios del
// profesor en el momento de aceptarlo (baseline) y qué acciones marcó como
// aplicadas; al cerrar el corte se compara contra las evaluaciones NUEVAS
// (las que llegaron después de aceptarlo) y el resultado queda guardado.

// Foto de los promedios (ya ponderados por asistencia) al aceptar el plan.
@Serializable
data class PlanBaseline(
    val reviews: Int = 0,
    val overall: Double? = null,
    // RatingCategory.key -> promedio 1-5
    val categories: Map<String, Double> = emptyMap()
)

// Resultado guardado al cerrar el corte (o el motivo, si el plan fue
// reemplazado por uno nuevo antes de cerrarlo).
@Serializable
data class PlanOutcome(
    val motivo: String? = null,
    val reviews: Int = 0,
    val overall: Double? = null,
    val categories: Map<String, Double> = emptyMap(),
    val applied: Int = 0,
    val total: Int = 0,
    val improved: Int = 0,
    val measured: Int = 0,
    val verdict: String? = null
)

@Serializable
data class ImprovementPlanRow(
    val id: String,
    // "activo" | "cerrado" | "reemplazado"
    val status: String,
    val recommendations: List<AiRecommendation> = emptyList(),
    val applied: List<Boolean> = emptyList(),
    val baseline: PlanBaseline = PlanBaseline(),
    val update_reason: String? = null,
    val accepted_at: String,
    val closed_at: String? = null,
    val outcome: PlanOutcome? = null
)

// Mínimo de evaluaciones nuevas para que la comparación de un corte tenga
// sentido (con 1 o 2 sería reaccionar a ruido), y cuánto tiene que subir un
// promedio para contarlo como "mejoró".
const val MIN_NEW_REVIEWS_FOR_COMPARISON = 3
const val IMPROVEMENT_THRESHOLD = 0.2

data class CategoryDelta(val category: RatingCategory, val before: Double?, val after: Double?) {
    val change: Double? get() = if (before != null && after != null) after - before else null
}

data class PlanProgress(
    val newReviews: Int,
    val enough: Boolean,
    val overallBefore: Double?,
    val overallNow: Double?,
    val deltas: List<CategoryDelta>
)

object PlanMath {
    // Promedio general y por categoría, PONDERADO por asistencia (mismo peso
    // que usa RatingRepositoryImpl.summaryFrom). Las de peso 0 no cuentan.
    fun averages(notes: List<ProfessorFeedbackNote>): Pair<Double?, Map<RatingCategory, Double>> {
        val counted = notes.filter { AttendanceBands.weight(it.attendanceBand) > 0.0 }
        if (counted.isEmpty()) return null to emptyMap()
        val total = counted.sumOf { AttendanceBands.weight(it.attendanceBand) }
        val categories = RatingCategory.entries.associateWith { category ->
            val cards = RatingCards.byCategory.getValue(category)
            counted.sumOf { note ->
                val index = note.categoryAnswers.get(category)
                val w = if (index in cards.indices) cards[index].weight else 0.0
                w * AttendanceBands.weight(note.attendanceBand)
            } / total
        }
        return categories.values.average() to categories
    }

    fun countedReviews(notes: List<ProfessorFeedbackNote>): Int =
        notes.count { AttendanceBands.weight(it.attendanceBand) > 0.0 }

    fun baseline(notes: List<ProfessorFeedbackNote>): PlanBaseline {
        val (overall, categories) = averages(notes)
        return PlanBaseline(
            reviews = countedReviews(notes),
            overall = overall,
            categories = categories.mapKeys { it.key.key }
        )
    }

    private fun parse(value: String?): OffsetDateTime? =
        runCatching { OffsetDateTime.parse(value) }.getOrNull()

    // Compara el baseline del plan con las evaluaciones que llegaron DESPUÉS
    // de aceptarlo — eso es "el siguiente corte".
    fun progress(plan: ImprovementPlanRow, notes: List<ProfessorFeedbackNote>): PlanProgress {
        val since = parse(plan.accepted_at)
        val fresh = notes.filter { note ->
            val created = parse(note.createdAt)
            since != null && created != null && created.isAfter(since)
        }
        val (overallNow, categoriesNow) = averages(fresh)
        val count = countedReviews(fresh)
        val targeted = plan.recommendations.mapNotNull { it.category }.distinct()
        return PlanProgress(
            newReviews = count,
            enough = count >= MIN_NEW_REVIEWS_FOR_COMPARISON,
            overallBefore = plan.baseline.overall,
            overallNow = overallNow,
            deltas = targeted.map { CategoryDelta(it, plan.baseline.categories[it.key], categoriesNow[it]) }
        )
    }

    // Lo que se guarda en el historial al cerrar el corte.
    fun outcome(plan: ImprovementPlanRow, progress: PlanProgress): PlanOutcome {
        val measured = if (progress.enough) progress.deltas.filter { it.change != null } else emptyList()
        val improved = measured.count { (it.change ?: 0.0) >= IMPROVEMENT_THRESHOLD }
        val applied = plan.applied.count { it }
        val total = plan.recommendations.size
        val verdict = when {
            !progress.enough ->
                "Corte cerrado con pocas evaluaciones nuevas (${progress.newReviews}): todavía no hay datos suficientes para comparar."
            measured.isEmpty() ->
                "Corte cerrado: el plan no tenía categorías puntuales para comparar. Aplicó $applied de $total acciones."
            else ->
                "Mejoró en $improved de ${measured.size} categorías trabajadas. Aplicó $applied de $total acciones."
        }
        return PlanOutcome(
            motivo = "corte cerrado",
            reviews = progress.newReviews,
            overall = progress.overallNow,
            categories = progress.deltas.mapNotNull { d -> d.after?.let { d.category.key to it } }.toMap(),
            applied = applied,
            total = total,
            improved = improved,
            measured = measured.size,
            verdict = verdict
        )
    }
}
