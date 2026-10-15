package co.edu.utadeo.tadeoconecta.domain.repository

interface AuthRepository {
    suspend fun signIn(usernameOrEmail: String, password: String)
    suspend fun signUp(fullName: String, usernameOrEmail: String, password: String, requestedRole: String)
    suspend fun verifySignupOtp(email: String, code: String)
    suspend fun resendSignupOtp(email: String)
    suspend fun sendPasswordReset(usernameOrEmail: String)
    suspend fun currentUserId(): String?
    suspend fun signOut()
    fun institutionalEmail(usernameOrEmail: String): String
}
