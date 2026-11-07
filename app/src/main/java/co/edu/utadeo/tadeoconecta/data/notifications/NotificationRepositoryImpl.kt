package co.edu.utadeo.tadeoconecta.data.notifications

import co.edu.utadeo.tadeoconecta.core.network.SupabaseProvider
import co.edu.utadeo.tadeoconecta.domain.model.AppNotification
import co.edu.utadeo.tadeoconecta.domain.model.AppNotificationType
import co.edu.utadeo.tadeoconecta.domain.repository.AuthRepository
import co.edu.utadeo.tadeoconecta.domain.repository.NotificationRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

// Fila mínima de public.notifications — ver MODULO_07_NOTIFICACIONES.sql.
// type guarda AppNotificationType.key (texto plano, mismo patrón que
// RatingCategory/category_answers): así se puede agregar un nuevo origen de
// notificación ampliando el check constraint, sin otra migración de enum.
@Serializable
private data class NotificationRow(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val created_at: String,
    val read_at: String? = null
)

@Serializable
private data class NotificationIdRow(val id: String)

class NotificationRepositoryImpl(
    private val authRepository: AuthRepository
) : NotificationRepository {
    private val supabase get() = SupabaseProvider.client

    override suspend fun list(): List<AppNotification> {
        val userId = authRepository.currentUserId() ?: return emptyList()
        val rows = supabase.from("notifications")
            .select { filter { eq("user_id", userId) } }
            .decodeList<NotificationRow>()
            // Mismo criterio que el resto del repo (RatingRepositoryImpl,
            // ProfessorFeedbackNote): ordenar más reciente primero se hace
            // en Kotlin, no con un .order() de Postgrest, para no depender
            // de una API del SDK que el resto del proyecto no usa todavía.
            .sortedByDescending { it.created_at }
        // Si algún día se agrega un tipo nuevo en Supabase antes de que la
        // app se actualice, una fila con un type desconocido simplemente se
        // omite en vez de tumbar toda la pantalla (mismo criterio que
        // ImprovementGoalRepositoryImpl.myGoal con categorías huérfanas).
        return rows.mapNotNull { row ->
            val type = AppNotificationType.entries.firstOrNull { it.key == row.type } ?: return@mapNotNull null
            AppNotification(
                id = row.id,
                type = type,
                title = row.title,
                body = row.body,
                createdAt = row.created_at,
                read = row.read_at != null
            )
        }
    }

    override suspend fun markAllRead() {
        authRepository.currentUserId() ?: return
        supabase.postgrest.rpc(function = "mark_all_notifications_read")
    }

    // Solo pide la columna id de hasta una fila no leída — no trae la lista
    // completa solo para encender el punto rojo de la campana.
    override suspend fun hasUnread(): Boolean {
        val userId = authRepository.currentUserId() ?: return false
        val rows = supabase.from("notifications")
            .select(columns = Columns.list("id")) {
                // supabase-kt no tiene un isNull() en el filter builder — el
                // equivalente a "IS NULL" es exact(columna, null).
                filter { eq("user_id", userId); exact("read_at", null) }
                limit(1)
            }
            .decodeList<NotificationIdRow>()
        return rows.isNotEmpty()
    }
}
