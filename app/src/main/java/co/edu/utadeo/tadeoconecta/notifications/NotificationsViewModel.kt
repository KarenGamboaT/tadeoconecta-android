package co.edu.utadeo.tadeoconecta.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.utadeo.tadeoconecta.data.auth.AuthRepositoryImpl
import co.edu.utadeo.tadeoconecta.data.notifications.NotificationRepositoryImpl
import co.edu.utadeo.tadeoconecta.domain.model.AppNotification
import co.edu.utadeo.tadeoconecta.domain.repository.AuthRepository
import co.edu.utadeo.tadeoconecta.domain.repository.NotificationRepository
import kotlinx.coroutines.launch

// Módulo 07 — un solo ViewModel para dos cosas relacionadas pero separadas
// en el tiempo: el indicador de la campana (se consulta apenas entra
// StudentShell/ProfessorShell, en cada pestaña) y la pantalla completa de
// notificaciones (se consulta solo al abrirla). Vive en MainAppShell y se
// pasa hacia abajo a ambos shells — mismo patrón que profileVm, para no
// tener dos instancias (una por rol) que no se enteren la una de la otra.
enum class NotificationsScreen { Loading, Content, Error }

enum class NotificationsError(val title: String, val message: String, val recovery: String) {
    LoadFailed(
        "No pudimos cargar tus notificaciones",
        "Ocurrió un problema al consultar Supabase.",
        "Revisa tu conexión y vuelve a intentarlo."
    )
}

data class NotificationsUiState(
    val screen: NotificationsScreen = NotificationsScreen.Loading,
    val notifications: List<AppNotification> = emptyList(),
    // Lo que de verdad enciende el punto rojo de la campana — ya no un punto
    // fijo como antes de este módulo.
    val hasUnread: Boolean = false,
    // Revisión general — distinto de screen == Loading a propósito: eso
    // reemplaza toda la pantalla por el spinner inicial. isRefreshing es
    // solo para el gesto de pull-to-refresh, que debe dejar la lista actual
    // visible debajo del indicador mientras se vuelve a consultar.
    val isRefreshing: Boolean = false,
    val error: NotificationsError? = null
)

class NotificationsViewModel : ViewModel() {
    private val authRepository: AuthRepository = AuthRepositoryImpl()
    private val notificationRepository: NotificationRepository = NotificationRepositoryImpl(authRepository)

    var state by mutableStateOf(NotificationsUiState())
        private set

    // Revisión general — guardado por USUARIO, no por un boolean simple a
    // secas: este ViewModel vive mientras vive la Activity (viewModel() no
    // lo recrea al cerrar sesión y entrar con otra cuenta), así que un
    // boolean fijo dejaba la campana pegada con el valor del usuario
    // ANTERIOR para quien entrara después — nunca volvía a consultar
    // Supabase. Guardarlo por id de usuario también arregla de paso que un
    // fallo (p. ej. un problema de red justo al iniciar) ya no deja la
    // campana atascada para siempre: como loadedForUser no se actualiza
    // si falla, el siguiente refreshBadgeIfNeeded() sí reintenta.
    private var badgeLoadedForUser: String? = null

    // Solo prende/apaga la campana — no trae la lista completa. Se llama
    // desde MainAppShell cada vez que se monta (cada login nuevo incluido).
    fun refreshBadgeIfNeeded() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId()
            if (userId != null && userId == badgeLoadedForUser) return@launch
            runCatching { notificationRepository.hasUnread() }
                .onSuccess {
                    state = state.copy(hasUnread = it)
                    badgeLoadedForUser = userId
                }
        }
    }

    // Se llama al entrar a la pantalla de notificaciones (overlay en
    // StudentShell/ProfessorShell): carga la lista real y, si se obtiene con
    // éxito, marca todo como leído — abrir la pantalla ya cuenta como
    // "las viste", apaga la campana de inmediato en pantalla.
    fun open() {
        state = state.copy(screen = NotificationsScreen.Loading, error = null)
        viewModelScope.launch {
            runCatching { notificationRepository.list() }
                .onSuccess { list ->
                    state = state.copy(screen = NotificationsScreen.Content, notifications = list, hasUnread = false)
                    // Housekeeping en segundo plano: si falla, el usuario ya
                    // vio su lista completa — lo único pendiente es que
                    // Supabase no quedó marcado, y se reintenta solo la
                    // próxima vez que se abra esta pantalla.
                    runCatching { notificationRepository.markAllRead() }
                }
                .onFailure {
                    state = state.copy(screen = NotificationsScreen.Error, error = NotificationsError.LoadFailed)
                }
        }
    }

    fun retryFromError() = open()

    // Revisión general — pull-to-refresh: a diferencia de open(), NO pasa
    // por screen = Loading (eso reemplazaría toda la lista por el spinner
    // de pantalla completa) — solo prende isRefreshing mientras se vuelve a
    // consultar, y la lista actual se queda visible debajo del indicador.
    fun refresh() {
        if (state.isRefreshing) return
        state = state.copy(isRefreshing = true)
        viewModelScope.launch {
            runCatching { notificationRepository.list() }
                .onSuccess { list -> state = state.copy(notifications = list, hasUnread = false) }
            state = state.copy(isRefreshing = false)
        }
    }
}
