package co.edu.utadeo.tadeoconecta.data.professor

import co.edu.utadeo.tadeoconecta.core.network.SupabaseProvider
import co.edu.utadeo.tadeoconecta.domain.model.AiRecommendation
import co.edu.utadeo.tadeoconecta.domain.model.ImprovementPlanRow
import co.edu.utadeo.tadeoconecta.domain.model.PlanBaseline
import co.edu.utadeo.tadeoconecta.domain.model.PlanOutcome
import co.edu.utadeo.tadeoconecta.domain.repository.ImprovementPlanRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

// Sin valores por defecto a propósito: siempre viajan en el update.
@Serializable
private data class AppliedUpdate(val applied: List<Boolean>)

@Serializable
private data class CloseUpdate(val status: String, val closed_at: String, val outcome: PlanOutcome)

class ImprovementPlanRepositoryImpl : ImprovementPlanRepository {
    private val supabase get() = SupabaseProvider.client
    private val table = "professor_improvement_plans"

    // La RLS ya filtra por professor_id = auth.uid(): cada profesor solo ve
    // sus propios planes, no hace falta filtrarlo acá.
    override suspend fun activePlan(): ImprovementPlanRow? =
        supabase.from(table)
            .select { filter { eq("status", "activo") } }
            .decodeList<ImprovementPlanRow>()
            .firstOrNull()

    override suspend fun history(): List<ImprovementPlanRow> =
        supabase.from(table)
            .select { filter { neq("status", "activo") } }
            .decodeList<ImprovementPlanRow>()
            .sortedByDescending { it.accepted_at }

    // Por RPC (no insert directo): la tabla no tiene permiso de INSERT para
    // authenticated a propósito, así el cierre del plan anterior y la
    // creación del nuevo son una sola operación atómica.
    override suspend fun accept(recommendations: List<AiRecommendation>, baseline: PlanBaseline, reason: String?) {
        supabase.postgrest.rpc(
            function = "accept_improvement_plan",
            parameters = buildJsonObject {
                put("p_recommendations", Json.encodeToJsonElement(recommendations))
                put("p_baseline", Json.encodeToJsonElement(baseline))
                if (!reason.isNullOrBlank()) put("p_reason", reason.trim())
            }
        )
    }

    override suspend fun setApplied(planId: String, applied: List<Boolean>) {
        supabase.from(table).update(AppliedUpdate(applied = applied)) {
            filter { eq("id", planId) }
        }
    }

    override suspend fun close(planId: String, outcome: PlanOutcome) {
        supabase.from(table).update(
            CloseUpdate(status = "cerrado", closed_at = Instant.now().toString(), outcome = outcome)
        ) {
            filter { eq("id", planId) }
        }
    }
}
