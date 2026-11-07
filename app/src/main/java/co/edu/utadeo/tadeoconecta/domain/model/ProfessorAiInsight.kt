package co.edu.utadeo.tadeoconecta.domain.model

import kotlinx.serialization.Serializable

// Puntos 1+2 de la lista de Juan — recomendaciones de mejora GENERADAS POR
// IA para cada profesor, a partir de SUS propios resultados (antes de esto,
// "Mejorar" solo mostraba una lista fija de acciones por categoría — ver
// ProfessorTips.actionsByCategory en MainShell.kt — la MISMA lista para
// cualquier profesor con la misma categoría débil, sin mirar lo que de
// verdad escribieron sus estudiantes).
//
// Esto viaja tal cual como JSON desde la Edge Function
// generate-improvement-recommendations (ver supabase/functions/), que es la
// única pieza que de verdad llama a un modelo de IA (Gemini) — la app nunca
// llama a la API de Gemini directo, para no exponer esa llave en el APK.
// Los nombres de campo están en español y DEBEN coincidir exactamente con
// lo que la función devuelve.
@Serializable
data class AiRecommendation(
    val categoria: String? = null,
    val titulo: String,
    val recomendacion: String
) {
    // `categoria` es el `key` de RatingCategory (ver Rating.kt) cuando la
    // recomendación es sobre una categoría puntual, o null cuando es un
    // consejo general que no encaja en ninguna ("sigue así", etc.).
    val category: RatingCategory? get() = RatingCategory.entries.find { it.key == categoria }
}

@Serializable
data class AiRecommendationsResponse(
    val recommendations: List<AiRecommendation> = emptyList()
)
