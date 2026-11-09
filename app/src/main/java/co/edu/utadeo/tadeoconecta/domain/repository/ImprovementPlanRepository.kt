package co.edu.utadeo.tadeoconecta.domain.repository

import co.edu.utadeo.tadeoconecta.domain.model.AiRecommendation
import co.edu.utadeo.tadeoconecta.domain.model.ImprovementPlanRow
import co.edu.utadeo.tadeoconecta.domain.model.PlanBaseline
import co.edu.utadeo.tadeoconecta.domain.model.PlanOutcome

// Módulo 12 — planes de mejora aceptados + su historial permanente (ver
// MODULO_12_ASISTENCIA_Y_PLANES.sql). Nada se borra: aceptar un plan nuevo
// deja el anterior como "reemplazado", y cerrar un corte lo deja "cerrado".
interface ImprovementPlanRepository {
    // El plan vigente del profesor autenticado, o null si no tiene.
    suspend fun activePlan(): ImprovementPlanRow?

    // Planes anteriores (cerrados o reemplazados), el más reciente primero.
    suspend fun history(): List<ImprovementPlanRow>

    // Acepta un plan: guarda las recomendaciones + el baseline, y deja el plan
    // activo anterior (si había) como "reemplazado" — todo en una transacción.
    suspend fun accept(recommendations: List<AiRecommendation>, baseline: PlanBaseline, reason: String?)

    // Marca qué acciones del plan activo ya aplicó el profesor.
    suspend fun setApplied(planId: String, applied: List<Boolean>)

    // Cierra el corte del plan activo guardando el resultado en el historial.
    suspend fun close(planId: String, outcome: PlanOutcome)
}
