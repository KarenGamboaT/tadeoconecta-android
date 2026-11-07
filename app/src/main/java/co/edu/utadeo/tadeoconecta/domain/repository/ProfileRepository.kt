package co.edu.utadeo.tadeoconecta.domain.repository

import co.edu.utadeo.tadeoconecta.domain.model.ProfessorProfileDetails
import co.edu.utadeo.tadeoconecta.domain.model.StudentProfileDetails
import co.edu.utadeo.tadeoconecta.domain.model.UserProfile

interface ProfileRepository {
    suspend fun currentProfile(): UserProfile
    suspend fun ensureStudentProfile(userId: String)

    suspend fun studentDetails(userId: String): StudentProfileDetails
    suspend fun professorDetails(userId: String): ProfessorProfileDetails

    suspend fun updateFullName(userId: String, fullName: String)
    suspend fun updateStudentDetails(userId: String, studentCode: String?, semester: Int?)
    suspend fun updateProfessorDetails(userId: String, professionalTitle: String?, bio: String?)

    // Sube la foto a Supabase Storage (bucket "avatars", archivo "{userId}.jpg"),
    // actualiza profiles.avatar_url y devuelve la URL pública ya lista para mostrar.
    suspend fun uploadAvatar(userId: String, bytes: ByteArray): String

    // Carreras del estudiante autenticado (reemplaza todo el conjunto, igual
    // que ProfessorRepository.saveSubjects). Puede haber más de una por
    // doble titulación.
    suspend fun saveStudentCareers(careerIds: Set<String>)
    suspend fun studentCareerIds(userId: String): Set<String>
}
