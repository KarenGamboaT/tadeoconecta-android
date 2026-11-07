package co.edu.utadeo.tadeoconecta.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.utadeo.tadeoconecta.auth.PasswordPolicy
import co.edu.utadeo.tadeoconecta.data.auth.AuthRepositoryImpl
import co.edu.utadeo.tadeoconecta.data.catalog.CatalogRepositoryImpl
import co.edu.utadeo.tadeoconecta.data.professor.ProfessorRepositoryImpl
import co.edu.utadeo.tadeoconecta.data.profile.ProfileRepositoryImpl
import co.edu.utadeo.tadeoconecta.domain.model.AppRole
import co.edu.utadeo.tadeoconecta.domain.model.Career
import co.edu.utadeo.tadeoconecta.domain.model.Faculty
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorProfileDetails
import co.edu.utadeo.tadeoconecta.domain.model.StudentProfileDetails
import co.edu.utadeo.tadeoconecta.domain.model.Subject
import co.edu.utadeo.tadeoconecta.domain.model.SubjectCareerLink
import co.edu.utadeo.tadeoconecta.domain.model.UserProfile
import co.edu.utadeo.tadeoconecta.domain.model.appRole
import kotlinx.coroutines.launch

// Módulo 02 — Perfil de usuario. Mismo contrato que el Módulo 01:
// entrada → validación → carga → éxito → error → recuperación → reintento → éxito.
enum class ProfileScreen { Loading, Overview, EditPersonal, ChangePassword, PasswordUpdated, EditSubjects, EditCareers, Privacy, Accessibility, Help, Error }

enum class ProfileError(val title: String, val message: String, val recovery: String) {
    LoadFailed(
        "No pudimos cargar tu perfil",
        "Ocurrió un problema al traer tus datos desde Supabase.",
        "Revisa tu conexión y vuelve a intentarlo."
    ),
    SaveFailed(
        "No pudimos guardar los cambios",
        "Tus datos no se actualizaron en Supabase.",
        "Lo que escribiste no se perdió; intenta guardar de nuevo."
    ),
    WeakPassword(
        "La contraseña no cumple los requisitos",
        "Debe tener mínimo 8 caracteres, mayúscula, minúscula, número y un carácter especial.",
        "Ajusta tu contraseña según la lista de requisitos y vuelve a intentarlo."
    ),
    PasswordMismatch(
        "Las contraseñas no coinciden",
        "La confirmación debe ser idéntica a la nueva contraseña.",
        "Vuelve a escribirla."
    ),
    CurrentPasswordIncorrect(
        "Contraseña actual incorrecta",
        "No pudimos confirmar que eres tú.",
        "Verifica tu contraseña actual e inténtalo de nuevo."
    ),
    PasswordSaveFailed(
        "No pudimos actualizar tu contraseña",
        "Supabase no pudo completar el cambio.",
        "No se cerró tu sesión; intenta de nuevo en unos segundos."
    ),
    AvatarUploadFailed(
        "No pudimos actualizar tu foto",
        "La imagen no se pudo subir a Supabase.",
        "Verifica tu conexión y vuelve a intentarlo; también puedes probar con otra foto."
    ),
    SubjectsLoadFailed(
        "No pudimos cargar tus asignaturas",
        "Ocurrió un problema al traer el catálogo desde Supabase.",
        "Revisa tu conexión y vuelve a intentarlo."
    ),
    SubjectsSaveFailed(
        "No pudimos guardar tus asignaturas",
        "Los cambios no se actualizaron en Supabase.",
        "Tu selección no se perdió; intenta guardar de nuevo."
    ),
    CareersLoadFailed(
        "No pudimos cargar tu carrera",
        "Ocurrió un problema al traer el catálogo de facultades y carreras desde Supabase.",
        "Revisa tu conexión y vuelve a intentarlo."
    ),
    CareersSaveFailed(
        "No pudimos guardar tu carrera",
        "Los cambios no se actualizaron en Supabase.",
        "Tu selección no se perdió; intenta guardar de nuevo."
    )
}

data class ProfileUiState(
    val screen: ProfileScreen = ProfileScreen.Loading,
    val loadingMessage: String = "Cargando tu perfil…",
    val profile: UserProfile? = null,
    val studentDetails: StudentProfileDetails = StudentProfileDetails(),
    val professorDetails: ProfessorProfileDetails = ProfessorProfileDetails(),
    val subjects: List<Subject> = emptyList(),
    val selectedSubjectIds: Set<String> = emptySet(),
    val faculties: List<Faculty> = emptyList(),
    val careers: List<Career> = emptyList(),
    val subjectCareerLinks: List<SubjectCareerLink> = emptyList(),
    val selectedCareerIds: Set<String> = emptySet(),
    val error: ProfileError? = null
)

class ProfileViewModel : ViewModel() {
    private val authRepository = AuthRepositoryImpl()
    private val profileRepository = ProfileRepositoryImpl(authRepository)
    private val professorRepository = ProfessorRepositoryImpl(authRepository)
    private val catalogRepository = CatalogRepositoryImpl()

    var state by mutableStateOf(ProfileUiState())
        private set

    // A dónde vuelve "Intentar de nuevo" desde la pantalla de error.
    private var recoveryScreen = ProfileScreen.Overview

    // Ojo: a propósito NO se auto-carga aquí. Esta instancia vive mientras vive
    // la Activity (MainAppShell la obtiene con viewModel(), que no la crea de
    // nuevo al cerrar e iniciar sesión con otra cuenta), así que si cargara sola
    // en el init, un segundo usuario vería el perfil del primero hasta que algo
    // más disparara una recarga. Quien compone la pantalla (MainAppShell) es
    // quien decide cuándo llamar a load(), una vez por cada sesión nueva.

    fun load() {
        state = state.copy(screen = ProfileScreen.Loading, loadingMessage = "Cargando tu perfil…", error = null)
        viewModelScope.launch {
            runCatching {
                val userId = authRepository.currentUserId() ?: error("No hay una sesión autenticada.")
                val profile = profileRepository.currentProfile()
                val role = profile.appRole()
                val student = if (role == AppRole.STUDENT) profileRepository.studentDetails(userId) else StudentProfileDetails()
                val professor = if (role == AppRole.PROFESSOR) profileRepository.professorDetails(userId) else ProfessorProfileDetails()
                state = state.copy(
                    screen = ProfileScreen.Overview,
                    profile = profile,
                    studentDetails = student,
                    professorDetails = professor
                )
            }.onFailure {
                recoveryScreen = ProfileScreen.Overview
                state = state.copy(screen = ProfileScreen.Error, error = ProfileError.LoadFailed)
            }
        }
    }

    fun goToEditPersonal() {
        recoveryScreen = ProfileScreen.EditPersonal
        state = state.copy(screen = ProfileScreen.EditPersonal, error = null)
    }

    fun goToChangePassword() {
        recoveryScreen = ProfileScreen.ChangePassword
        state = state.copy(screen = ProfileScreen.ChangePassword, error = null)
    }

    // Módulo 11 adelantado para el perfil docente: en el registro (Módulo 01)
    // las asignaturas solo se eligen una vez; esto deja editarlas después.
    fun goToEditSubjects() {
        val userId = state.profile?.id ?: return
        recoveryScreen = ProfileScreen.EditSubjects
        state = state.copy(screen = ProfileScreen.Loading, loadingMessage = "Cargando tus asignaturas…")
        viewModelScope.launch {
            runCatching {
                val catalog = professorRepository.subjects()
                val selected = professorRepository.professorSubjectIds(userId)
                val faculties = catalogRepository.faculties()
                val careers = catalogRepository.careers()
                val links = catalogRepository.subjectCareerLinks()
                state = state.copy(
                    screen = ProfileScreen.EditSubjects,
                    subjects = catalog,
                    selectedSubjectIds = selected,
                    faculties = faculties,
                    careers = careers,
                    subjectCareerLinks = links,
                    error = null
                )
            }.onFailure {
                state = state.copy(screen = ProfileScreen.Error, error = ProfileError.SubjectsLoadFailed)
            }
        }
    }

    fun toggleSubject(id: String) {
        val next = state.selectedSubjectIds.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
        state = state.copy(selectedSubjectIds = next)
    }

    fun saveSubjects(): String? {
        if (state.subjects.isNotEmpty() && state.selectedSubjectIds.isEmpty()) {
            return "Selecciona al menos una asignatura."
        }
        val userId = state.profile?.id ?: return "No hay una sesión autenticada."

        recoveryScreen = ProfileScreen.EditSubjects
        state = state.copy(screen = ProfileScreen.Loading, loadingMessage = "Guardando tus asignaturas…")
        viewModelScope.launch {
            runCatching {
                professorRepository.saveSubjects(state.selectedSubjectIds)
                state = state.copy(screen = ProfileScreen.Overview)
            }.onFailure {
                state = state.copy(screen = ProfileScreen.Error, error = ProfileError.SubjectsSaveFailed)
            }
        }
        return null
    }

    // Igual que goToEditSubjects/saveSubjects, pero para la carrera del
    // estudiante (M05/12 adelantado): el registro (AuthViewModel) solo la pide
    // una vez; esto deja editarla después, incluyendo doble titulación.
    fun goToEditCareers() {
        val userId = state.profile?.id ?: return
        recoveryScreen = ProfileScreen.EditCareers
        state = state.copy(screen = ProfileScreen.Loading, loadingMessage = "Cargando tu carrera…")
        viewModelScope.launch {
            runCatching {
                val faculties = catalogRepository.faculties()
                val careers = catalogRepository.careers()
                val selected = profileRepository.studentCareerIds(userId)
                state = state.copy(
                    screen = ProfileScreen.EditCareers,
                    faculties = faculties,
                    careers = careers,
                    selectedCareerIds = selected,
                    error = null
                )
            }.onFailure {
                state = state.copy(screen = ProfileScreen.Error, error = ProfileError.CareersLoadFailed)
            }
        }
    }

    fun toggleCareer(id: String) {
        val next = state.selectedCareerIds.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
        state = state.copy(selectedCareerIds = next)
    }

    fun saveCareers(): String? {
        if (state.careers.isNotEmpty() && state.selectedCareerIds.isEmpty()) {
            return "Selecciona al menos una carrera."
        }

        recoveryScreen = ProfileScreen.EditCareers
        state = state.copy(screen = ProfileScreen.Loading, loadingMessage = "Guardando tu carrera…")
        viewModelScope.launch {
            runCatching {
                profileRepository.saveStudentCareers(state.selectedCareerIds)
                state = state.copy(screen = ProfileScreen.Overview)
            }.onFailure {
                state = state.copy(screen = ProfileScreen.Error, error = ProfileError.CareersSaveFailed)
            }
        }
        return null
    }

    // Privacidad, Accesibilidad y Ayuda — a diferencia de EditSubjects/EditCareers,
    // son contenido estático (no leen nada de Supabase), así que no necesitan el
    // patrón async de goToEditSubjects(): un simple cambio de pantalla alcanza,
    // igual que goToEditPersonal()/goToChangePassword().
    fun goToPrivacy() {
        recoveryScreen = ProfileScreen.Privacy
        state = state.copy(screen = ProfileScreen.Privacy, error = null)
    }

    fun goToAccessibility() {
        recoveryScreen = ProfileScreen.Accessibility
        state = state.copy(screen = ProfileScreen.Accessibility, error = null)
    }

    fun goToHelp() {
        recoveryScreen = ProfileScreen.Help
        state = state.copy(screen = ProfileScreen.Help, error = null)
    }

    fun backToOverview() {
        state = state.copy(screen = ProfileScreen.Overview, error = null)
    }

    fun retryFromError() {
        state = state.copy(screen = recoveryScreen, error = null)
    }

    fun savePersonalData(fullName: String, studentCode: String, semesterText: String, professionalTitle: String, bio: String): String? {
        val name = fullName.trim()
        if (name.isBlank()) return "El nombre no puede estar vacío."
        val semester = semesterText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        if (semesterText.isNotBlank() && semester == null) return "El semestre debe ser un número."

        val userId = state.profile?.id ?: return "No hay una sesión autenticada."
        val role = state.profile?.appRole() ?: AppRole.STUDENT

        recoveryScreen = ProfileScreen.EditPersonal
        state = state.copy(screen = ProfileScreen.Loading, loadingMessage = "Guardando cambios…")
        viewModelScope.launch {
            runCatching {
                profileRepository.updateFullName(userId, name)
                if (role == AppRole.STUDENT) {
                    profileRepository.updateStudentDetails(userId, studentCode.trim().ifBlank { null }, semester)
                } else {
                    profileRepository.updateProfessorDetails(userId, professionalTitle.trim().ifBlank { null }, bio.trim().ifBlank { null })
                }
                val profile = profileRepository.currentProfile()
                val student = if (role == AppRole.STUDENT) profileRepository.studentDetails(userId) else state.studentDetails
                val professor = if (role == AppRole.PROFESSOR) profileRepository.professorDetails(userId) else state.professorDetails
                state = state.copy(screen = ProfileScreen.Overview, profile = profile, studentDetails = student, professorDetails = professor)
            }.onFailure {
                state = state.copy(screen = ProfileScreen.Error, error = ProfileError.SaveFailed)
            }
        }
        return null
    }

    // Módulo 02 — foto de perfil. Mismo contrato de carga/error que el resto:
    // la imagen ya viene comprimida en JPEG desde la UI (ver MainShell.kt).
    fun uploadAvatar(jpegBytes: ByteArray) {
        val userId = state.profile?.id ?: return
        recoveryScreen = ProfileScreen.Overview
        state = state.copy(screen = ProfileScreen.Loading, loadingMessage = "Subiendo tu foto…")
        viewModelScope.launch {
            runCatching {
                profileRepository.uploadAvatar(userId, jpegBytes)
                val profile = profileRepository.currentProfile()
                state = state.copy(screen = ProfileScreen.Overview, profile = profile)
            }.onFailure {
                state = state.copy(screen = ProfileScreen.Error, error = ProfileError.AvatarUploadFailed)
            }
        }
    }

    // La UI llama esto cuando no pudo ni siquiera leer/comprimir la imagen elegida
    // (p. ej. un archivo corrupto): mismo error visible, nunca un toast silencioso.
    fun avatarPrepFailed() {
        recoveryScreen = ProfileScreen.Overview
        state = state.copy(screen = ProfileScreen.Error, error = ProfileError.AvatarUploadFailed)
    }

    // currentPassword se exige a propósito: antes de esto, cualquiera con
    // acceso a una sesión ya abierta y sin vigilancia podía cambiar la
    // contraseña sin saber la anterior, dejando fuera al dueño real de la
    // cuenta. Reautenticar (volver a iniciar sesión con el mismo correo +
    // la contraseña actual) es la confirmación — si falla, NUNCA se llega
    // a updatePassword.
    fun changePassword(currentPassword: String, password: String, confirm: String): String? {
        if (currentPassword.isBlank() || password.isBlank() || confirm.isBlank()) {
            return "Completa los tres campos."
        }
        if (!PasswordPolicy.isValid(password)) {
            recoveryScreen = ProfileScreen.ChangePassword
            state = state.copy(screen = ProfileScreen.Error, error = ProfileError.WeakPassword)
            return null
        }
        if (password != confirm) {
            recoveryScreen = ProfileScreen.ChangePassword
            state = state.copy(screen = ProfileScreen.Error, error = ProfileError.PasswordMismatch)
            return null
        }

        recoveryScreen = ProfileScreen.ChangePassword
        state = state.copy(screen = ProfileScreen.Loading, loadingMessage = "Confirmando tu identidad…")
        viewModelScope.launch {
            val email = authRepository.currentUserEmail()
            if (email == null) {
                state = state.copy(screen = ProfileScreen.Error, error = ProfileError.PasswordSaveFailed)
                return@launch
            }
            val reauthenticated = runCatching { authRepository.signIn(email, currentPassword) }
            if (reauthenticated.isFailure) {
                state = state.copy(screen = ProfileScreen.Error, error = ProfileError.CurrentPasswordIncorrect)
                return@launch
            }
            state = state.copy(loadingMessage = "Actualizando tu contraseña…")
            runCatching {
                authRepository.updatePassword(password)
                state = state.copy(screen = ProfileScreen.PasswordUpdated)
            }.onFailure {
                state = state.copy(screen = ProfileScreen.Error, error = ProfileError.PasswordSaveFailed)
            }
        }
        return null
    }
}
