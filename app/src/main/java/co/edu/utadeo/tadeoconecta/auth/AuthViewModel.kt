package co.edu.utadeo.tadeoconecta.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.utadeo.tadeoconecta.core.network.SupabaseProvider
import co.edu.utadeo.tadeoconecta.data.auth.AuthRepositoryImpl
import co.edu.utadeo.tadeoconecta.data.catalog.CatalogRepositoryImpl
import co.edu.utadeo.tadeoconecta.data.profile.ProfileRepositoryImpl
import co.edu.utadeo.tadeoconecta.data.professor.ProfessorRepositoryImpl
import co.edu.utadeo.tadeoconecta.domain.model.AppRole
import co.edu.utadeo.tadeoconecta.domain.model.Career
import co.edu.utadeo.tadeoconecta.domain.model.Faculty
import co.edu.utadeo.tadeoconecta.domain.model.Subject
import co.edu.utadeo.tadeoconecta.domain.model.SubjectCareerLink
import co.edu.utadeo.tadeoconecta.domain.model.appRole
import kotlinx.coroutines.launch

// El HTML v10 sigue siendo la fuente visual. Esta clase solo reemplaza datos simulados por Supabase.
enum class AuthScreen {
    Startup,
    Login,
    SignUp,
    Verify,
    StudentCareers,
    ProfessorCode,
    ProfessorSubjects,
    Forgot,
    RecoveryCode,
    NewPassword,
    PasswordUpdated,
    Loading,
    Error,
    ErrorLibrary,
    Destination
}

enum class UserRole { Student, Professor }

// Política de contraseña usada tanto para la validación real (signUp) como para el
// checklist visual que se muestra mientras el usuario escribe su contraseña.
object PasswordPolicy {
    data class Rule(val id: String, val label: String, val test: (String) -> Boolean)

    val rules: List<Rule> = listOf(
        Rule("length", "8+ caracteres") { pw -> pw.length >= 8 },
        Rule("upper", "Una mayúscula (A-Z)") { pw -> pw.any { it.isUpperCase() } },
        Rule("lower", "Una minúscula (a-z)") { pw -> pw.any { it.isLowerCase() } },
        Rule("digit", "Un número (0-9)") { pw -> pw.any { it.isDigit() } },
        Rule("special", "Un carácter especial") { pw -> pw.any { !it.isLetterOrDigit() } }
    )

    fun isValid(password: String): Boolean = rules.all { it.test(password) }
}

enum class AuthError(val title: String, val message: String, val recovery: String) {
    NotConfigured("Falta conectar Supabase", "La app no encuentra las variables de conexión.", "Agrega SUPABASE_URL y SUPABASE_KEY en local.properties y vuelve a compilar."),
    InvalidCredentials("Correo o contraseña incorrectos", "No pudimos validar tus credenciales.", "Revisa el usuario institucional y la contraseña."),
    EmailNotConfirmed("Correo pendiente de verificación", "Tu cuenta existe, pero el correo aún no ha sido confirmado.", "Revisa tu correo institucional y completa la verificación."),
    RateLimit("Acceso temporalmente bloqueado", "Detectamos varios intentos seguidos.", "Espera unos minutos antes de volver a intentarlo."),
    AccountLocked("Demasiados intentos fallidos", "Fallaste la contraseña 3 veces seguidas.", "Por seguridad, espera 5 minutos antes de volver a intentarlo."),
    Offline("Sin conexión a internet", "No podemos completar esta acción sin conexión.", "Revisa tu red y vuelve a intentarlo."),
    Timeout("La solicitud tardó demasiado", "No recibimos respuesta a tiempo.", "Comprueba tu conexión y vuelve a intentar."),
    Server("Servicio temporalmente no disponible", "Supabase no pudo completar la solicitud.", "No se perdió información. Intenta de nuevo más tarde."),
    InvalidInstitutional("Usuario institucional no válido", "Debes utilizar una cuenta @utadeo.edu.co.", "Corrige el usuario y vuelve a intentarlo."),
    Duplicate("Esta cuenta ya está registrada", "Ya existe una cuenta asociada a este usuario.", "Inicia sesión o recupera tu contraseña."),
    WeakPassword("La contraseña no cumple los requisitos", "Debe tener mínimo 8 caracteres, mayúscula, minúscula, número y un carácter especial.", "Ajusta tu contraseña según la lista de requisitos y vuelve a intentarlo."),
    PasswordMismatch("Las contraseñas no coinciden", "La confirmación debe ser idéntica.", "Vuelve a escribir la contraseña."),
    InvalidCode("Código incorrecto", "El código ingresado no coincide con el enviado.", "Comprueba los 6 dígitos o solicita uno nuevo."),
    ExpiredCode("El código expiró", "Por seguridad el código dejó de ser válido.", "Solicita uno nuevo para continuar."),
    SessionExpired("Tu sesión expiró", "Por seguridad cerramos tu sesión.", "Vuelve a iniciar sesión."),
    InvalidProfessorCode("Código docente no válido", "No pudimos validar el código de registro docente.", "Verifica el código entregado por la universidad."),
    NoSubjects("No hay asignaturas disponibles", "Supabase no tiene asignaturas activas configuradas.", "Carga las asignaturas en la tabla subjects y vuelve a intentarlo."),
    NoCareers("No hay carreras disponibles", "Supabase no tiene facultades o carreras cargadas todavía.", "Carga el catálogo de facultades y carreras y vuelve a intentarlo."),
    Unknown("No pudimos completar la acción", "Ocurrió un error inesperado.", "Intenta nuevamente. Si continúa, revisa los logs de Supabase.")
}

data class AuthUiState(
    val screen: AuthScreen = AuthScreen.Startup,
    val role: UserRole = UserRole.Student,
    val error: AuthError? = null,
    val destinationRole: UserRole? = null,
    val signUpName: String = "",
    val signUpUser: String = "",
    val signUpEmail: String = "",
    val recoveryEmail: String = "",
    val loginFailedAttempts: Int = 0,
    val loginFailedUser: String = "",
    val loginLockedUntil: Long = 0L,
    val loadingMessage: String = "Revisando tu sesión…",
    val subjects: List<Subject> = emptyList(),
    val selectedSubjectIds: Set<String> = emptySet(),
    val professorCodeValidated: Boolean = false,
    // Catálogo institucional compartido por el paso de materias del docente
    // y el de carrera(s) del estudiante (ver AuthScreens.kt).
    val faculties: List<Faculty> = emptyList(),
    val careers: List<Career> = emptyList(),
    val subjectCareerLinks: List<SubjectCareerLink> = emptyList(),
    val selectedCareerIds: Set<String> = emptySet()
)

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepositoryImpl()
    private val profileRepository = ProfileRepositoryImpl(authRepository)
    private val professorRepository = ProfessorRepositoryImpl(authRepository)
    private val catalogRepository = CatalogRepositoryImpl()

    var state by mutableStateOf(AuthUiState())
        private set

    // Se incrementa solo al cerrar sesión, para que la UI pueda limpiar los
    // campos de formulario (borradores) que se conservan entre pantallas.
    var formResetSignal by mutableStateOf(0)
        private set

    private val history = mutableListOf<AuthScreen>()
    private var recoveryScreen: AuthScreen = AuthScreen.Login

    // Cuando un paso IRREVERSIBLE (verificar el OTP, redimir el código
    // docente) ya se completó y lo que falló fue el paso siguiente (cargar
    // catálogo, crear el perfil), un reintento normal (volver a la pantalla
    // anterior) le pediría al usuario repetir algo que ya no es válido —
    // el código ya se consumió y Supabase lo va a rechazar la segunda vez.
    // Si esto no es null, retryFromError() lo ejecuta en vez de solo
    // navegar a recoveryScreen.
    private var postVerificationRetry: (() -> Unit)? = null

    // Punto único para fijar a dónde volver si el usuario reintenta: así
    // nunca queda un postVerificationRetry de un flujo anterior colgado por
    // accidente cuando empieza uno nuevo.
    private fun beginFlow(screen: AuthScreen) {
        recoveryScreen = screen
        postVerificationRetry = null
    }

    init {
        restoreSession()
    }

    fun go(screen: AuthScreen) {
        if (state.screen != screen) history.add(state.screen)
        state = state.copy(screen = screen, error = null)
    }

    fun back() {
        val prev = history.removeLastOrNull() ?: AuthScreen.Login
        state = state.copy(screen = prev, error = null)
    }

    fun setRole(role: UserRole) {
        state = state.copy(role = role)
    }

    fun login(user: String, password: String): String? {
        val validation = validateInstitutionalLogin(user, password)
        if (validation != null) return validation
        if (!SupabaseProvider.isConfigured) return showError(AuthError.NotConfigured)

        val email = authRepository.institutionalEmail(user)
        val now = System.currentTimeMillis()
        if (state.loginFailedUser == email && state.loginLockedUntil > now) {
            return showError(AuthError.AccountLocked)
        }

        beginFlow(AuthScreen.Login)
        state = state.copy(screen = AuthScreen.Loading, loadingMessage = "Ingresando…", error = null)
        viewModelScope.launch {
            runCatching {
                authRepository.signIn(user, password)
                state = state.copy(loginFailedAttempts = 0, loginFailedUser = "", loginLockedUntil = 0L)
                routeCurrentUser()
            }.onFailure { throwable ->
                val mapped = mapError(throwable)
                if (mapped == AuthError.InvalidCredentials) {
                    val attempts = if (state.loginFailedUser == email) state.loginFailedAttempts + 1 else 1
                    if (attempts >= 3) {
                        state = state.copy(
                            loginFailedAttempts = 0,
                            loginFailedUser = email,
                            loginLockedUntil = System.currentTimeMillis() + 5 * 60 * 1000L
                        )
                        showError(AuthError.AccountLocked)
                    } else {
                        state = state.copy(loginFailedAttempts = attempts, loginFailedUser = email)
                        showError(AuthError.InvalidCredentials)
                    }
                } else {
                    showError(mapped)
                }
            }
        }
        return null
    }

    fun signUp(name: String, user: String, password: String, confirm: String, terms: Boolean): String? {
        if (name.isBlank() || user.isBlank() || password.isBlank() || confirm.isBlank() || !terms) {
            return "Completa todos los campos y acepta las políticas."
        }
        if (!isValidInstitutionalUser(user)) return showError(AuthError.InvalidInstitutional)
        if (!PasswordPolicy.isValid(password)) return showError(AuthError.WeakPassword)
        if (password != confirm) return showError(AuthError.PasswordMismatch)
        if (!SupabaseProvider.isConfigured) return showError(AuthError.NotConfigured)

        val email = authRepository.institutionalEmail(user)
        beginFlow(AuthScreen.SignUp)
        state = state.copy(
            signUpName = name.trim(),
            signUpUser = user.trim(),
            signUpEmail = email,
            screen = AuthScreen.Loading,
            loadingMessage = "Creando tu cuenta…",
            error = null
        )

        viewModelScope.launch {
            runCatching {
                authRepository.signUp(
                    fullName = name,
                    usernameOrEmail = user,
                    password = password,
                    requestedRole = if (state.role == UserRole.Professor) "professor" else "student"
                )
                state = state.copy(screen = AuthScreen.Verify)
            }.onFailure { showAsyncError(it) }
        }
        return null
    }

    fun verify(code: String): String? {
        if (code.length != 6 || code.any { !it.isDigit() }) return "Ingresa los 6 dígitos del código."
        if (!SupabaseProvider.isConfigured) return showError(AuthError.NotConfigured)
        val email = state.signUpEmail
        if (email.isBlank()) return showError(AuthError.Unknown)

        beginFlow(AuthScreen.Verify)
        state = state.copy(screen = AuthScreen.Loading, loadingMessage = "Verificando tu correo…")
        viewModelScope.launch {
            val verified = runCatching { authRepository.verifySignupOtp(email, code) }
            if (verified.isFailure) {
                showAsyncError(verified.exceptionOrNull()!!)
                return@launch
            }
            // El OTP ya se consumió a partir de acá: lo que sigue
            // (crear el perfil, cargar el catálogo) es lo único que un
            // reintento debe repetir — nunca el código.
            continueAfterSignupVerified()
        }
        return null
    }

    private fun continueAfterSignupVerified() {
        postVerificationRetry = { continueAfterSignupVerified() }
        state = state.copy(screen = AuthScreen.Loading, loadingMessage = "Preparando tu cuenta…")
        viewModelScope.launch {
            runCatching {
                val userId = authRepository.currentUserId() ?: error("No session after OTP verification")
                if (state.role == UserRole.Professor) {
                    state = state.copy(screen = AuthScreen.ProfessorCode)
                } else {
                    profileRepository.ensureStudentProfile(userId)
                    val faculties = catalogRepository.faculties()
                    val careers = catalogRepository.careers()
                    state = state.copy(
                        faculties = faculties,
                        careers = careers,
                        screen = AuthScreen.StudentCareers
                    )
                }
                postVerificationRetry = null
            }.onFailure { showAsyncError(it) }
        }
    }

    fun resendCode() {
        val email = state.signUpEmail
        if (email.isBlank() || !SupabaseProvider.isConfigured) return
        viewModelScope.launch {
            runCatching { authRepository.resendSignupOtp(email) }
                .onFailure { showAsyncError(it) }
        }
    }

    fun validateProfessorCode(code: String): String? {
        if (code.isBlank()) return "Ingresa el código institucional docente."
        beginFlow(AuthScreen.ProfessorCode)
        state = state.copy(screen = AuthScreen.Loading, loadingMessage = "Validando código docente…")
        viewModelScope.launch {
            val redeemed = runCatching { professorRepository.redeemRegistrationCode(code) }
            val accepted = redeemed.fold(onSuccess = { it }, onFailure = { showAsyncError(it); return@launch })
            if (!accepted) {
                showError(AuthError.InvalidProfessorCode)
                return@launch
            }
            // El código docente ya se redimió (uses+1, rol actualizado) a
            // partir de acá: un reintento nunca debe volver a pedirlo —
            // solo repetir la carga del catálogo que sigue.
            continueAfterProfessorCodeRedeemed()
        }
        return null
    }

    private fun continueAfterProfessorCodeRedeemed() {
        postVerificationRetry = { continueAfterProfessorCodeRedeemed() }
        state = state.copy(screen = AuthScreen.Loading, loadingMessage = "Cargando catálogo…")
        viewModelScope.launch {
            runCatching {
                val subjects = professorRepository.subjects()
                val faculties = catalogRepository.faculties()
                val careers = catalogRepository.careers()
                val links = catalogRepository.subjectCareerLinks()
                state = state.copy(
                    professorCodeValidated = true,
                    subjects = subjects,
                    faculties = faculties,
                    careers = careers,
                    subjectCareerLinks = links,
                    screen = AuthScreen.ProfessorSubjects,
                    error = null
                )
                postVerificationRetry = null
            }.onFailure { showAsyncError(it) }
        }
    }

    fun toggleSubject(id: String) {
        val next = state.selectedSubjectIds.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
        state = state.copy(selectedSubjectIds = next)
    }

    fun saveProfessorSubjects(): String? {
        if (state.subjects.isEmpty()) return showError(AuthError.NoSubjects)
        if (state.selectedSubjectIds.isEmpty()) return "Selecciona al menos una asignatura."
        beginFlow(AuthScreen.ProfessorSubjects)
        state = state.copy(screen = AuthScreen.Loading, loadingMessage = "Guardando tus asignaturas…")
        viewModelScope.launch {
            runCatching {
                professorRepository.saveSubjects(state.selectedSubjectIds)
                state = state.copy(destinationRole = UserRole.Professor, screen = AuthScreen.Destination)
            }.onFailure { showAsyncError(it) }
        }
        return null
    }

    // Módulo 01 (estudiante) — carrera(s) al registrarse. Permite más de una
    // por doble titulación; a diferencia de las materias del profesor, este
    // paso es nuevo y no existía antes (no hay pantalla previa que saltarse).
    fun toggleCareer(id: String) {
        val next = state.selectedCareerIds.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
        state = state.copy(selectedCareerIds = next)
    }

    fun saveStudentCareers(): String? {
        if (state.careers.isEmpty()) return showError(AuthError.NoCareers)
        if (state.selectedCareerIds.isEmpty()) return "Selecciona al menos una carrera."
        beginFlow(AuthScreen.StudentCareers)
        state = state.copy(screen = AuthScreen.Loading, loadingMessage = "Guardando tu carrera…")
        viewModelScope.launch {
            runCatching {
                profileRepository.saveStudentCareers(state.selectedCareerIds)
                state = state.copy(destinationRole = UserRole.Student, screen = AuthScreen.Destination)
            }.onFailure { showAsyncError(it) }
        }
        return null
    }

    fun forgot(user: String): String? {
        if (user.isBlank()) return "Ingresa tu usuario institucional."
        if (!isValidInstitutionalUser(user)) return showError(AuthError.InvalidInstitutional)
        if (!SupabaseProvider.isConfigured) return showError(AuthError.NotConfigured)

        val email = authRepository.institutionalEmail(user)
        beginFlow(AuthScreen.Forgot)
        state = state.copy(
            recoveryEmail = email,
            screen = AuthScreen.Loading,
            loadingMessage = "Enviando código de recuperación…"
        )
        viewModelScope.launch {
            runCatching {
                authRepository.sendPasswordReset(user)
                state = state.copy(screen = AuthScreen.RecoveryCode)
            }.onFailure { showAsyncError(it) }
        }
        return null
    }

    fun verifyRecoveryCode(code: String): String? {
        if (code.length != 6 || code.any { !it.isDigit() }) return "Ingresa los 6 dígitos del código."
        if (!SupabaseProvider.isConfigured) return showError(AuthError.NotConfigured)
        val email = state.recoveryEmail
        if (email.isBlank()) return showError(AuthError.Unknown)

        beginFlow(AuthScreen.RecoveryCode)
        state = state.copy(screen = AuthScreen.Loading, loadingMessage = "Verificando código…")
        viewModelScope.launch {
            runCatching {
                authRepository.verifyPasswordRecoveryOtp(email, code)
                state = state.copy(screen = AuthScreen.NewPassword)
            }.onFailure { showAsyncError(it) }
        }
        return null
    }

    fun resendRecoveryCode() {
        val email = state.recoveryEmail
        if (email.isBlank() || !SupabaseProvider.isConfigured) return
        viewModelScope.launch {
            runCatching { authRepository.sendPasswordReset(email) }
                .onFailure { showAsyncError(it) }
        }
    }

    fun updateRecoveredPassword(password: String, confirm: String): String? {
        if (password.isBlank() || confirm.isBlank()) return "Completa los dos campos."
        if (!PasswordPolicy.isValid(password)) return showError(AuthError.WeakPassword)
        if (password != confirm) return showError(AuthError.PasswordMismatch)
        if (!SupabaseProvider.isConfigured) return showError(AuthError.NotConfigured)

        beginFlow(AuthScreen.NewPassword)
        state = state.copy(screen = AuthScreen.Loading, loadingMessage = "Actualizando tu contraseña…")
        viewModelScope.launch {
            runCatching {
                authRepository.updatePassword(password)
                runCatching { authRepository.signOut() }
                state = state.copy(recoveryEmail = "", screen = AuthScreen.PasswordUpdated)
            }.onFailure { showAsyncError(it) }
        }
        return null
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { authRepository.signOut() }
            history.clear()
            state = AuthUiState(screen = AuthScreen.Login)
            formResetSignal++
        }
    }

    fun showError(error: AuthError): String? {
        if (error == AuthError.SessionExpired) {
            // La sesión murió a mitad de un flujo (token/JWT expirado):
            // reintentar contra esa misma sesión muerta solo repetiría el
            // mismo error para siempre. El único reintento válido es volver
            // a loguearse, así que se fuerza eso en vez de recoveryScreen.
            postVerificationRetry = null
            recoveryScreen = AuthScreen.Login
            viewModelScope.launch { runCatching { authRepository.signOut() } }
        } else if (state.screen != AuthScreen.Loading && state.screen != AuthScreen.Error) {
            recoveryScreen = state.screen
        }
        state = state.copy(screen = AuthScreen.Error, error = error)
        return null
    }

    fun retryFromError() {
        val retry = postVerificationRetry
        if (retry != null) {
            postVerificationRetry = null
            retry()
            return
        }
        state = state.copy(screen = recoveryScreen, error = null)
    }

    private fun restoreSession() {
        if (!SupabaseProvider.isConfigured) {
            state = state.copy(screen = AuthScreen.Login)
            return
        }
        viewModelScope.launch {
            runCatching {
                val userId = authRepository.currentUserId()
                if (userId == null) {
                    state = state.copy(screen = AuthScreen.Login)
                } else {
                    routeCurrentUser()
                }
            }.onFailure {
                state = state.copy(screen = AuthScreen.Login)
            }
        }
    }

    private suspend fun routeCurrentUser() {
        val profile = profileRepository.currentProfile()
        val role = when (profile.appRole()) {
            AppRole.PROFESSOR -> UserRole.Professor
            AppRole.STUDENT -> UserRole.Student
        }
        state = state.copy(destinationRole = role, screen = AuthScreen.Destination, error = null)
    }

    private fun validateInstitutionalLogin(user: String, password: String): String? {
        if (user.isBlank() || password.isBlank()) return "Completa correo y contraseña."
        if (!isValidInstitutionalUser(user)) return "Usa tu usuario o correo institucional @utadeo.edu.co."
        return null
    }

    private fun isValidInstitutionalUser(value: String): Boolean {
        val raw = value.trim().lowercase()
        if (raw.contains(" ")) return false
        return !raw.contains("@") || raw.endsWith("@utadeo.edu.co")
    }

    private fun showAsyncError(throwable: Throwable) {
        showError(mapError(throwable))
    }

    private fun mapError(throwable: Throwable): AuthError {
        val message = buildString {
            append(throwable.message.orEmpty())
            append(' ')
            append(throwable.cause?.message.orEmpty())
        }.lowercase()

        return when {
            "invalid login credentials" in message || "invalid credentials" in message -> AuthError.InvalidCredentials
            "email not confirmed" in message || "email_not_confirmed" in message -> AuthError.EmailNotConfirmed
            "already registered" in message || "user already" in message -> AuthError.Duplicate
            "rate limit" in message || "too many" in message -> AuthError.RateLimit
            "expired" in message && ("otp" in message || "token" in message) -> AuthError.ExpiredCode
            ("otp" in message || "token" in message) && "invalid" in message -> AuthError.InvalidCode
            // Sesión/JWT muerta a mitad de un flujo autenticado (no durante
            // login/signup, donde "invalid"/"expired" ya se mapearon arriba
            // a errores más específicos) — distinto de un simple timeout de
            // red: acá SÍ hay que forzar volver a Login (ver showError).
            "jwt" in message || "refresh_token" in message ||
                ("session" in message && ("expired" in message || "missing" in message || "not found" in message)) ->
                AuthError.SessionExpired
            "unable to resolve host" in message || "network" in message || "failed to connect" in message -> AuthError.Offline
            "timeout" in message || "timed out" in message -> AuthError.Timeout
            "500" in message || "502" in message || "503" in message || "server" in message -> AuthError.Server
            else -> AuthError.Unknown
        }
    }
}
