package co.edu.utadeo.tadeoconecta.domain.repository

import co.edu.utadeo.tadeoconecta.domain.model.AppNotification

interface NotificationRepository {
    // Todas las notificaciones del usuario autenticado, más recientes primero.
    suspend fun list(): List<AppNotification>

    // "Marcar todas como leídas" de un solo golpe, al abrir la pantalla — ver
    // mark_all_notifications_read() en la migración. No hay un "marcar una
    // sola" todavía porque no hay forma de abrir una notificación individual
    // (no llevan a ningún lado distinto, son solo informativas).
    suspend fun markAllRead()

    // Para el indicador de la campana: no hace falta traer toda la lista
    // completa de nuevo si ya se tiene — ver NotificationsViewModel.
    suspend fun hasUnread(): Boolean
}
