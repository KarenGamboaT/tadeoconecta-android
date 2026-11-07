package co.edu.utadeo.tadeoconecta.domain.repository

import co.edu.utadeo.tadeoconecta.domain.model.Career
import co.edu.utadeo.tadeoconecta.domain.model.Faculty
import co.edu.utadeo.tadeoconecta.domain.model.SubjectCareerLink

// Catálogo institucional (Módulo 12, parcial): facultades, carreras, y qué
// materias pertenece a cada carrera. Solo lectura — el catálogo se carga a
// mano en Supabase, no hay pantalla de administración todavía (ver roadmap).
interface CatalogRepository {
    suspend fun faculties(): List<Faculty>
    suspend fun careers(): List<Career>
    suspend fun subjectCareerLinks(): List<SubjectCareerLink>
}
