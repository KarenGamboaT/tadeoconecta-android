package co.edu.utadeo.tadeoconecta.domain.repository

import co.edu.utadeo.tadeoconecta.domain.model.ProfessorGoalHistoryEntry
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorImprovementGoal
import co.edu.utadeo.tadeoconecta.domain.model.RatingCategory

// Módulo 09 — el objetivo activo del plan de mejora del profesor
// autenticado, persistido en professor_improvement_goals (una fila por
// profesor; sin objetivo activo = sin fila para ese profesor).
interface ImprovementGoalRepository {
    suspend fun myGoal(): ProfessorImprovementGoal?

    // Crea el objetivo si no existía, o lo reemplaza si ya tenía uno
    // activo en otra categoría — siempre reinicia el checklist de las 3
    // acciones en blanco.
    suspend fun setGoal(category: RatingCategory)

    suspend fun setActionDone(index: Int, done: Boolean)

    // "Quitar objetivo" (o reiniciar un plan ya completado). Archiva la meta
    // que había antes de borrarla — ver professor_improvement_goal_history.
    suspend fun clearGoal()

    // Metas anteriores, más reciente primero — ver
    // professor_improvement_goal_history.
    suspend fun history(): List<ProfessorGoalHistoryEntry>
}
