package co.edu.utadeo.tadeoconecta.data.profile

import co.edu.utadeo.tadeoconecta.core.network.SupabaseProvider
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorProfileDetails
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorProfileUpdate
import co.edu.utadeo.tadeoconecta.domain.model.ProfileAvatarUpdate
import co.edu.utadeo.tadeoconecta.domain.model.ProfileNameUpdate
import co.edu.utadeo.tadeoconecta.domain.model.StudentCareerInsert
import co.edu.utadeo.tadeoconecta.domain.model.StudentCareerRow
import co.edu.utadeo.tadeoconecta.domain.model.StudentProfileDetails
import co.edu.utadeo.tadeoconecta.domain.model.StudentProfileInsert
import co.edu.utadeo.tadeoconecta.domain.model.StudentProfileUpdate
import co.edu.utadeo.tadeoconecta.domain.model.UserProfile
import co.edu.utadeo.tadeoconecta.domain.repository.AuthRepository
import co.edu.utadeo.tadeoconecta.domain.repository.ProfileRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage

class ProfileRepositoryImpl(
    private val authRepository: AuthRepository
) : ProfileRepository {
    private val supabase get() = SupabaseProvider.client

    override suspend fun currentProfile(): UserProfile {
        val userId = authRepository.currentUserId()
            ?: error("No hay una sesión autenticada.")

        return supabase.from("profiles")
            .select {
                filter { eq("id", userId) }
            }
            .decodeSingle<UserProfile>()
    }

    override suspend fun ensureStudentProfile(userId: String) {
        supabase.from("student_profiles")
            .upsert(StudentProfileInsert(user_id = userId)) {
                onConflict = "user_id"
            }
    }

    // decodeList + firstOrNull en vez de decodeSingle: si el registro todavía
    // no existe (p. ej. justo después de redimir el código docente) no truena
    // la pantalla de perfil, simplemente muestra los campos vacíos.
    override suspend fun studentDetails(userId: String): StudentProfileDetails =
        supabase.from("student_profiles")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList<StudentProfileDetails>()
            .firstOrNull() ?: StudentProfileDetails()

    override suspend fun professorDetails(userId: String): ProfessorProfileDetails =
        supabase.from("professor_profiles")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList<ProfessorProfileDetails>()
            .firstOrNull() ?: ProfessorProfileDetails()

    override suspend fun updateFullName(userId: String, fullName: String) {
        supabase.from("profiles").update(ProfileNameUpdate(full_name = fullName)) {
            filter { eq("id", userId) }
        }
    }

    override suspend fun updateStudentDetails(userId: String, studentCode: String?, semester: Int?) {
        supabase.from("student_profiles").update(
            StudentProfileUpdate(student_code = studentCode, semester = semester)
        ) {
            filter { eq("user_id", userId) }
        }
    }

    override suspend fun updateProfessorDetails(userId: String, professionalTitle: String?, bio: String?) {
        supabase.from("professor_profiles").update(
            ProfessorProfileUpdate(professional_title = professionalTitle, bio = bio)
        ) {
            filter { eq("user_id", userId) }
        }
    }

    // Nombre fijo "{userId}.jpg" + upsert: cada foto nueva reemplaza la anterior
    // en vez de acumular archivos huérfanos en el bucket.
    override suspend fun uploadAvatar(userId: String, bytes: ByteArray): String {
        val path = "$userId.jpg"
        supabase.storage.from("avatars").upload(path, bytes) {
            upsert = true
        }
        // La URL pública no cambia entre subidas (mismo path), así que se le
        // agrega un parámetro de versión para que la app y Coil no sigan
        // mostrando la foto anterior cacheada.
        val freshUrl = supabase.storage.from("avatars").publicUrl(path) + "?v=${System.currentTimeMillis()}"
        supabase.from("profiles").update(ProfileAvatarUpdate(avatar_url = freshUrl)) {
            filter { eq("id", userId) }
        }
        return freshUrl
    }

    // Mismo patrón delete-then-insert que ProfessorRepositoryImpl.saveSubjects:
    // reemplaza el conjunto completo en vez de hacer un diff.
    override suspend fun saveStudentCareers(careerIds: Set<String>) {
        val userId = authRepository.currentUserId()
            ?: error("No hay una sesión autenticada.")

        supabase.from("student_careers").delete {
            filter { eq("student_id", userId) }
        }

        if (careerIds.isNotEmpty()) {
            supabase.from("student_careers").insert(
                careerIds.map { careerId ->
                    StudentCareerInsert(student_id = userId, career_id = careerId)
                }
            )
        }
    }

    override suspend fun studentCareerIds(userId: String): Set<String> =
        supabase.from("student_careers")
            .select {
                filter { eq("student_id", userId) }
            }
            .decodeList<StudentCareerRow>()
            .map { it.career_id }
            .toSet()
}
