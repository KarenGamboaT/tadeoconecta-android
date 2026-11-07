package co.edu.utadeo.tadeoconecta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import co.edu.utadeo.tadeoconecta.accessibility.AccessibilityPreferences
import co.edu.utadeo.tadeoconecta.auth.AuthApp
import co.edu.utadeo.tadeoconecta.ui.theme.TadeoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Se carga ANTES de setContent (no dentro): así la primera
        // composición ya arranca con el tamaño de texto guardado, sin un
        // parpadeo al tamaño normal mientras se lee SharedPreferences.
        AccessibilityPreferences.load(this)
        setContent {
            // Revisión general — Accesibilidad real: un único punto que
            // envuelve TODA la app. Compose escala automáticamente cualquier
            // texto en .sp según LocalDensity.fontScale, así que esto alcanza
            // para toda la app sin tocar cada Text() uno por uno (ver
            // AccessibilityPreferences.kt y AccessibilityScreen en MainShell.kt).
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(baseDensity.density, fontScale = AccessibilityPreferences.textScale)
            ) {
                TadeoTheme { AuthApp() }
            }
        }
    }
}
