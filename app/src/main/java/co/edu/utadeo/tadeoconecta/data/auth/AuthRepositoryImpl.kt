package co.edu.utadeo.tadeoconecta.data.auth

import co.edu.utadeo.tadeoconecta.core.network.SupabaseProvider
import co.edu.utadeo.tadeoconecta.domain.repository.AuthRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.OtpType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepositoryImpl : AuthRepository {
    private val auth get() = SupabaseProvider.client.auth

    override fun institutionalEmail(usernameOrEmail: String): String {
        val raw = usernameOrEmail.trim().lowercase()
        return if (raw.contains("@")) raw else "$raw@utadeo.edu.co"
    }

    override suspend fun signIn(usernameOrEmail: String, password: String) {
        auth.signInWith(Email) {
            email = institutionalEmail(usernameOrEmail)
            this.password = password
        }
    }

    override suspend fun signUp(
        fullName: String,
        usernameOrEmail: String,
        password: String,
        requestedRole: String
    ) {
        auth.signUpWith(Email) {
            email = institutionalEmail(usernameOrEmail)
            this.password = password
            data = buildJsonObject {
                put("full_name", fullName.trim())
                put("requested_role", requestedRole)
            }
        }
    }

    override suspend fun verifySignupOtp(email: String, code: String) {
        auth.verifyEmailOtp(
            type = OtpType.Email.SIGNUP,
            email = email,
            token = code
        )
    }

    override suspend fun resendSignupOtp(email: String) {
        auth.resendEmail(OtpType.Email.SIGNUP, email)
    }

    override suspend fun sendPasswordReset(usernameOrEmail: String) {
        auth.resetPasswordForEmail(institutionalEmail(usernameOrEmail))
    }

    override suspend fun verifyPasswordRecoveryOtp(usernameOrEmail: String, code: String) {
        auth.verifyEmailOtp(
            type = OtpType.Email.RECOVERY,
            email = institutionalEmail(usernameOrEmail),
            token = code
        )
    }

    override suspend fun updatePassword(newPassword: String) {
        auth.updateUser {
            password = newPassword
        }
    }

    override suspend fun currentUserId(): String? =
        auth.currentUserOrNull()?.id

    override suspend fun currentUserEmail(): String? =
        auth.currentUserOrNull()?.email

    override suspend fun signOut() {
        auth.signOut()
    }
}
