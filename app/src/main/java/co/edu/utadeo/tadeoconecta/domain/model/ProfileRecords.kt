package co.edu.utadeo.tadeoconecta.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class StudentProfileInsert(
    val user_id: String
)

// Campos editables de student_profiles. La carrera NO vive aquí: un
// estudiante puede tener más de una (doble titulación), así que se maneja
// como relación muchos-a-muchos en student_careers (ver Catalog.kt y
// ProfileRepository.saveStudentCareers/studentCareerIds), no como columna.
@Serializable
data class StudentProfileDetails(
    val student_code: String? = null,
    val semester: Int? = null
)

@Serializable
data class StudentProfileUpdate(
    val student_code: String?,
    val semester: Int?
)

// Campos editables de professor_profiles.
@Serializable
data class ProfessorProfileDetails(
    val professional_title: String? = null,
    val bio: String? = null
)

@Serializable
data class ProfessorProfileUpdate(
    val professional_title: String?,
    val bio: String?
)

@Serializable
data class ProfileNameUpdate(
    val full_name: String
)

@Serializable
data class ProfileAvatarUpdate(
    val avatar_url: String
)
