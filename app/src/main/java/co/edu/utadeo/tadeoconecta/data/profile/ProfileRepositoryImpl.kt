package co.edu.utadeo.tadeoconecta.data.profile

import co.edu.utadeo.tadeoconecta.core.network.SupabaseProvider
import co.edu.utadeo.tadeoconecta.domain.model.StudentProfileInsert
import co.edu.utadeo.tadeoconecta.domain.model.UserProfile
import co.edu.utadeo.tadeoconecta.domain.repository.AuthRepository
import co.edu.utadeo.tadeoconecta.domain.repository.ProfileRepository
import io.github.jan.supabase.postgrest.from

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
}
