package co.edu.utadeo.tadeoconecta.domain.repository

interface AuthRepository {
    suspend fun signIn(usernameOrEmail: String, password: String)
    suspend fun signUp(fullName: String, usernameOrEmail: String, password: String, requestedRole: String)
    suspend fun verifySignupOtp(email: String, code: String)
    suspend fun resendSignupOtp(email: String)
    suspend fun sendPasswordReset(usernameOrEmail: String)
    suspend fun verifyPasswordRecoveryOtp(usernameOrEmail: String, code: String)
    suspend fun updatePassword(newPassword: String)
    suspend fun currentUserId(): String?
    // Módulo 02 — cambiar contraseña exige reautenticar con la actual
    // primero (ver ProfileViewModel.changePassword): sin esto, cualquiera
    // con acceso a una sesión ya abierta y sin vigilancia podía cambiar la
    // contraseña sin saber la anterior, dejando fuera al dueño real.
    suspend fun currentUserEmail(): String?
    suspend fun signOut()
    fun institutionalEmail(usernameOrEmail: String): String
}
