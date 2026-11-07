package co.edu.utadeo.tadeoconecta.domain.model

import kotlinx.serialization.Serializable

// Módulo 12 (parcial) — catálogo institucional: facultades, carreras, y las
// relaciones muchos-a-muchos entre materias y carreras (una materia puede
// dictarse en más de una carrera) y entre estudiantes y carreras (doble
// titulación = más de una carrera por estudiante). Se carga a mano en
// Supabase por ahora (igual que las materias), no hay pantalla de
// administración todavía.

@Serializable
data class Faculty(
    val id: String,
    val name: String,
    val code: String? = null
)

@Serializable
data class Career(
    val id: String,
    val faculty_id: String,
    val name: String,
    val code: String? = null
)

@Serializable
data class SubjectCareerLink(
    val subject_id: String,
    val career_id: String
)

@Serializable
data class StudentCareerInsert(
    val student_id: String,
    val career_id: String
)

// Fila mínima de student_careers cuando ya se consulta filtrado por un solo
// estudiante (igual patrón que ProfessorSubjectRow en Subject.kt).
@Serializable
data class StudentCareerRow(
    val career_id: String
)
