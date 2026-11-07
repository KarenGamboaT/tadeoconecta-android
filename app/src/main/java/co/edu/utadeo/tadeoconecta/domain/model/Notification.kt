package co.edu.utadeo.tadeoconecta.domain.model

// Módulo 07 — ver supabase/MODULO_07_NOTIFICACIONES.sql. type es el mismo
// catálogo cerrado que el check constraint de la tabla: texto plano, no un
// enum de Postgres, mismo criterio que RatingCategory.key.
enum class AppNotificationType(val key: String) {
    NEW_REVIEW("new_review"),
    REVIEW_SUBMITTED("review_submitted"),
    GOAL_COMPLETED("goal_completed")
}

data class AppNotification(
    val id: String,
    val type: AppNotificationType,
    val title: String,
    val body: String,
    val createdAt: String,
    val read: Boolean
)
