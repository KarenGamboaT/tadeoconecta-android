package co.edu.utadeo.tadeoconecta.domain.repository

import co.edu.utadeo.tadeoconecta.domain.model.ProfessorSummary
import co.edu.utadeo.tadeoconecta.domain.model.Subject

interface ProfessorRepository {
    suspend fun redeemRegistrationCode(code: String): Boolean
    suspend fun subjects(): List<Subject>
    suspend fun saveSubjects(subjectIds: Set<String>)

    // Qué asignaturas tiene marcadas HOY un profesor ya registrado (para
    // precargar la selección al editar desde el perfil, no en el registro).
    suspend fun professorSubjectIds(professorId: String): Set<String>

    // Módulo 03 — Búsqueda de profesores. query vacío = sin filtro de nombre/
    // materia (lista todos); subjectId no nulo = solo profesores que dictan
    // esa materia (filtro de chip "Materia"); careerId no nulo = solo
    // profesores que dictan alguna materia de esa carrera (filtro de chip
    // "Carrera", ya posible ahora que existen careers/subject_careers).
    suspend fun searchProfessors(query: String, subjectId: String?, careerId: String?): List<ProfessorSummary>

    // Favoritos del estudiante autenticado. Antes vivía como un Set<String>
    // de ejemplo en memoria (MainShell.kt); ahora es real en Supabase
    // (student_favorites) para que sobreviva a cerrar la app y a profesores
    // reales de la búsqueda, no solo a los 3 de ejemplo.
    suspend fun favoriteProfessors(): List<ProfessorSummary>
    suspend fun setFavorite(professorId: String, favorite: Boolean)
}
