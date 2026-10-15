package co.edu.utadeo.tadeoconecta.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    @SerialName("full_name") val fullName: String,
    val role: String,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

enum class AppRole { STUDENT, PROFESSOR }

fun UserProfile.appRole(): AppRole =
    if (role.equals("professor", ignoreCase = true)) AppRole.PROFESSOR else AppRole.STUDENT
