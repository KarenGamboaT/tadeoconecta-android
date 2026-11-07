package co.edu.utadeo.tadeoconecta.accessibility

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

// Revisión general — Accesibilidad real (tamaño de texto), no decorativa.
// Un solo punto de verdad, leído en dos lugares: MainActivity.kt (envuelve
// TODA la app en un CompositionLocalProvider(LocalDensity ...) una sola vez,
// así el fontScale llega a cada Text() en .sp sin tocar ninguno de ellos
// uno por uno) y AccessibilityScreen (MainShell.kt), que lo cambia.
// Guardado en SharedPreferences para que sobreviva a cerrar la app, no solo
// a cambiar de pantalla — a diferencia de un simple var en memoria.
object AccessibilityPreferences {
    private const val PREFS_NAME = "accessibility_prefs"
    private const val KEY_TEXT_SCALE = "text_scale"

    const val SCALE_NORMAL = 1.0f
    const val SCALE_LARGE = 1.15f
    const val SCALE_EXTRA_LARGE = 1.3f

    // mutableFloatStateOf (no un var a secas): MainActivity.kt lee este valor
    // dentro de setContent {}, así que Compose necesita poder detectar el
    // cambio y recomponer toda la app cuando AccessibilityScreen lo actualice.
    var textScale by mutableFloatStateOf(SCALE_NORMAL)
        private set

    // Se llama una sola vez, en MainActivity.onCreate antes de setContent —
    // para que la app arranque ya con el tamaño de texto que el usuario
    // había elegido, en vez de un parpadeo al tamaño normal primero.
    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        textScale = prefs.getFloat(KEY_TEXT_SCALE, SCALE_NORMAL)
    }

    fun setTextScale(context: Context, scale: Float) {
        textScale = scale
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_TEXT_SCALE, scale)
            .apply()
    }
}
