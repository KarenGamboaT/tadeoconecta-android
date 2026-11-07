package co.edu.utadeo.tadeoconecta.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Subject(
    val id: String,
    val code: String? = null,
    val name: String,
    val active: Boolean = true
)

@Serializable
data class ProfessorSubjectInsert(
    val professor_id: String,
    val subject_id: String,
    val active: Boolean = true
)

// Para leer qué asignaturas tiene marcadas un profesor ya registrado
// (professor_subjects trae más columnas; solo nos interesa subject_id aquí).
@Serializable
data class ProfessorSubjectRow(
    val subject_id: String
)
