package co.edu.utadeo.tadeoconecta.domain.model

import kotlinx.serialization.Serializable

// Resultado de búsqueda de un profesor: junta profiles + professor_profiles +
// las materias que dicta (professor_subjects → subjects). Se arma en Kotlin
// con varias consultas simples (como el resto del repo), no con un embed de
// Postgrest, para no depender de nombres exactos de foreign keys.
data class ProfessorSummary(
    val id: String,
    val fullName: String,
    val email: String,
    val avatarUrl: String?,
    val professionalTitle: String?,
    val bio: String?,
    val subjectNames: List<String>,
    // Carreras reales donde dicta clase (profesor → professor_subjects →
    // subject_careers → careers), no un dato inventado. Puede tener más de
    // una porque un profesor puede dictar en varias carreras (Módulo 12).
    val careerNames: List<String> = emptyList(),
    // Promedio real de professor_ratings (Módulo 05) y cuántas calificaciones
    // lo componen. null/0 cuando nadie ha calificado todavía — nunca se
    // inventa un número para rellenar la tarjeta.
    val averageRating: Double? = null,
    val reviewCount: Int = 0
)

// Fila mínima de student_favorites: student_id siempre se conoce desde la
// sesión, así que solo hace falta traer/mandar el professor_id.
@Serializable
data class StudentFavoriteRow(
    val professor_id: String
)

@Serializable
data class StudentFavoriteInsert(
    val student_id: String,
    val professor_id: String
)

// Fila mínima de professor_subjects cuando hace falta professor_id además de
// subject_id (ProfessorSubjectRow, en Subject.kt, solo trae subject_id porque
// ahí siempre se consulta ya filtrado por un professor_id conocido).
@Serializable
data class ProfessorSubjectLink(
    val professor_id: String,
    val subject_id: String
)

// Fila mínima de professor_profiles para traer varios profesores de una vez
// (ProfessorProfileDetails, en ProfileRecords.kt, no trae user_id porque ahí
// siempre se consulta ya filtrado por un solo usuario).
@Serializable
data class ProfessorProfileRow(
    val user_id: String,
    val professional_title: String? = null,
    val bio: String? = null
)
