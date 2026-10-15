package co.edu.utadeo.tadeoconecta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import co.edu.utadeo.tadeoconecta.auth.AuthApp
import co.edu.utadeo.tadeoconecta.ui.theme.TadeoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TadeoTheme { AuthApp() } }
    }
}
