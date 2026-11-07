package co.edu.utadeo.tadeoconecta.data.professor

import co.edu.utadeo.tadeoconecta.core.network.SupabaseProvider
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorGoalHistoryEntry
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorImprovementGoal
import co.edu.utadeo.tadeoconecta.domain.model.RatingCategory
import co.edu.utadeo.tadeoconecta.domain.repository.AuthRepository
import co.edu.utadeo.tadeoconecta.domain.repository.ImprovementGoalRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Fila mínima de professor_improvement_goals — ver
// MODULO_08_09_PANEL_PROFESOR.sql. category guarda RatingCategory.key (el
// mismo patrón que category_answers/category_notes en professor_ratings:
// texto plano, no un enum de Postgres, para no necesitar otra migración si
// se agrega una categoría).
@Serializable
private data class ImprovementGoalRow(
    val professor_id: String,
    val category: String,
    val actions_done: List<Boolean> = listOf(false, false, false)
)

// actions_done SIN valor por defecto a propósito: con un default,
// kotlinx.serialization puede omitirlo del JSON cuando coincide con ese
// default, y entonces el upsert (INSERT ... ON CONFLICT DO UPDATE) no lo
// incluiría en el SET — dejaría el progreso VIEJO en vez de reiniciarlo al
// elegir una categoría nueva. Como parámetro obligatorio, siempre viaja.
@Serializable
private data class ImprovementGoalInsert(
    val professor_id: String,
    val category: String,
    val actions_done: List<Boolean>
)

@Serializable
private data class ActionsDoneUpdate(val actions_done: List<Boolean>)

// Módulo 09 — ver MODULO_09_HISTORIAL_METAS.sql. "completed" SIN valor por
// defecto, mismo motivo que ImprovementGoalInsert.actions_done: un insert es
// una fila completamente nueva, no un upsert, así que acá el riesgo real no
// es que se omita en el JSON — es solo disciplina de no olvidarlo al llamar.
@Serializable
private data class GoalHistoryInsert(
    val professor_id: String,
    val category: String,
    val actions_done: List<Boolean>,
    val completed: Boolean
)

@Serializable
private data class GoalHistoryRow(
    val category: String,
    val actions_done: List<Boolean>,
    val completed: Boolean,
    val archived_at: String
)

class ImprovementGoalRepositoryImpl(
    private val authRepository: AuthRepository
) : ImprovementGoalRepository {
    private val supabase get() = SupabaseProvider.client

    // Revisión general — setGoal/setActionDone/clearGoal son todos
    // lectura→cálculo→escritura (leen la meta activa para decidir qué
    // escribir o qué archivar). Sin serializarlos, dos llamadas casi
    // simultáneas (dos taps rápidos del checklist, o "Marcar objetivo"
    // tocado dos veces) podían leer la misma meta vieja y pisarse: una
    // marca del checklist se perdía en silencio, o se archivaban dos
    // copias de la misma meta en el historial inmutable (que ya no se
    // puede corregir después, por diseño). Un Mutex por instancia alcanza
    // porque las tres siempre actúan sobre la única fila del usuario
    // autenticado de esta misma instancia.
    private val goalMutex = Mutex()

    override suspend fun myGoal(): ProfessorImprovementGoal? {
        val userId = authRepository.currentUserId() ?: return null
        val row = supabase.from("professor_improvement_goals")
            .select { filter { eq("professor_id", userId) } }
            .decodeList<ImprovementGoalRow>()
            .firstOrNull() ?: return null
        // Si algún día se borra/renombra una categoría, una fila huérfana
        // con una clave desconocida simplemente se trata como "sin
        // objetivo" en vez de tumbar la pantalla.
        val category = RatingCategory.entries.firstOrNull { it.key == row.category } ?: return null
        return ProfessorImprovementGoal(category, row.actions_done)
    }

    // Mismo patrón upsert-por-llave-primaria que professor_ratings: la
    // primary key (professor_id) es lo que de verdad garantiza una sola
    // fila de objetivo por profesor.
    override suspend fun setGoal(category: RatingCategory) {
        goalMutex.withLock {
            val userId = authRepository.currentUserId() ?: error("No hay una sesión autenticada.")
            // Módulo 09 — si ya había una meta activa, se archiva antes de
            // reemplazarla (ver archive() y professor_improvement_goal_history).
            // Hoy "Marcar como objetivo" en la UI solo aparece cuando no hay meta
            // activa, pero esto cubre igual el caso por si ese punto de entrada
            // cambia más adelante — nunca se pierde un registro en silencio.
            myGoal()?.let { archive(userId, it) }
            supabase.from("professor_improvement_goals").upsert(
                ImprovementGoalInsert(professor_id = userId, category = category.key, actions_done = listOf(false, false, false))
            ) {
                onConflict = "professor_id"
            }
        }
    }

    override suspend fun setActionDone(index: Int, done: Boolean) {
        goalMutex.withLock {
            val userId = authRepository.currentUserId() ?: return@withLock
            val current = myGoal() ?: return@withLock
            if (index !in current.actionsDone.indices) return@withLock
            val wasComplete = current.actionsDone.all { it }
            val updated = current.actionsDone.toMutableList().also { it[index] = done }
            supabase.from("professor_improvement_goals").update(
                ActionsDoneUpdate(actions_done = updated)
            ) {
                filter { eq("professor_id", userId) }
            }

            // Módulo 07 — "meta cumplida" se notifica una sola vez: justo en
            // el toggle que hace que las 3 acciones queden en true, no en
            // cada recarga ni cada vez que se destilda/vuelve a tildar una
            // acción con el objetivo ya completo (wasComplete evita repetir
            // el aviso).
            if (!wasComplete && updated.all { it }) {
                runCatching {
                    supabase.postgrest.rpc(
                        function = "create_notification",
                        parameters = buildJsonObject {
                            put("p_user_id", userId)
                            put("p_type", "goal_completed")
                            put("p_title", "¡Meta cumplida!")
                            put("p_body", "Completaste las 3 acciones sugeridas para mejorar en ${current.category.shortLabel}.")
                        }
                    )
                }
            }
        }
    }

    // Módulo 09 — "Quitar objetivo" / "Reiniciar objetivo" en la UI: antes de
    // borrar la meta activa, se archiva tal cual estaba (completed = si las
    // 3 acciones ya estaban en true) — ver professor_improvement_goal_history.
    override suspend fun clearGoal() {
        goalMutex.withLock {
            val userId = authRepository.currentUserId() ?: return@withLock
            myGoal()?.let { archive(userId, it) }
            supabase.from("professor_improvement_goals").delete {
                filter { eq("professor_id", userId) }
            }
        }
    }

    // Best-effort a propósito: si archivar falla (p.ej. un problema de red
    // justo en ese instante), igual se deja reemplazar/quitar la meta activa
    // — un historial incompleto es mejor que bloquear al profesor por algo
    // que para él es secundario.
    private suspend fun archive(userId: String, goal: ProfessorImprovementGoal) {
        runCatching {
            supabase.from("professor_improvement_goal_history").insert(
                GoalHistoryInsert(
                    professor_id = userId,
                    category = goal.category.key,
                    actions_done = goal.actionsDone,
                    completed = goal.actionsDone.all { it }
                )
            )
        }
    }

    override suspend fun history(): List<ProfessorGoalHistoryEntry> {
        val userId = authRepository.currentUserId() ?: return emptyList()
        val rows = supabase.from("professor_improvement_goal_history")
            .select { filter { eq("professor_id", userId) } }
            .decodeList<GoalHistoryRow>()
            .sortedByDescending { it.archived_at }
        // Si algún día se borra/renombra una categoría, una fila huérfana con
        // una clave desconocida simplemente se omite del historial en vez de
        // tumbar la pantalla — mismo criterio que myGoal().
        return rows.mapNotNull { row ->
            val category = RatingCategory.entries.firstOrNull { it.key == row.category } ?: return@mapNotNull null
            ProfessorGoalHistoryEntry(
                category = category,
                actionsDone = row.actions_done,
                completed = row.completed,
                archivedAt = row.archived_at
            )
        }
    }
}
