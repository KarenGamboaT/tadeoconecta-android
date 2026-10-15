package co.edu.utadeo.tadeoconecta.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val TadeoBlue = Color(0xFF1565FF)
val TadeoUTBlue = Color(0xFF00558F)
val TadeoInk = Color(0xFF0F1E3A)
val TadeoMuted = Color(0xFF6B7280)
val TadeoSoft = Color(0xFFF2F4F7)
val TadeoCanvas = Color(0xFFF7F8FA)
val TadeoBg = TadeoCanvas
val TadeoBorder = Color(0xFFE4E7EC)
val TadeoAqua = Color(0xFF00A9A6)
val TadeoError = Color(0xFFF15B48)
val TadeoSuccess = Color(0xFF2FBE79)
val TadeoRating = Color(0xFFFFD33D)

private val scheme = lightColorScheme(
    primary = TadeoBlue,
    onPrimary = Color.White,
    background = TadeoCanvas,
    onBackground = TadeoInk,
    surface = Color.White,
    onSurface = TadeoInk,
    error = TadeoError
)

private val type = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, lineHeight = 31.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
)

@Composable
fun TadeoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
