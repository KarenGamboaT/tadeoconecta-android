package co.edu.utadeo.tadeoconecta.domain.repository

import co.edu.utadeo.tadeoconecta.domain.model.UserProfile

interface ProfileRepository {
    suspend fun currentProfile(): UserProfile
    suspend fun ensureStudentProfile(userId: String)
}
