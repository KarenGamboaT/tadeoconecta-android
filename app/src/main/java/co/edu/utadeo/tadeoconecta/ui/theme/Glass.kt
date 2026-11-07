package co.edu.utadeo.tadeoconecta.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Sistema visual "glass" (vidrio esmerilado estilo Apple / Liquid Glass) para
// toda la app. No usa blur real: Modifier.blur/RenderEffect solo se ve bien
// desde Android 12, y minSdk aquí es 26 (ver app/build.gradle.kts). En vez de
// eso se simula el vidrio con superficies blancas semitransparentes + borde
// claro + sombra suave, flotando sobre un fondo con manchas de color
// difuminadas (GlassBackdropBrush + GlassBlobs). Se ve "esmerilado" en
// cualquier celular, sin librerías nuevas ni costo de rendimiento.

// Fondo base de toda pantalla: un degradado casi blanco con un tinte muy
// sutil de azul/aqua institucional, para que las tarjetas de vidrio (blancas
// semitransparentes) tengan algo de color debajo que "se note a través".
val GlassBackdropBrush = Brush.linearGradient(
    listOf(Color(0xFFF2F6FF), Color(0xFFF8FAFC), Color(0xFFF0FBF9))
)

// Tres manchas de color grandes y con borde difuso (radial, transparente
// hacia afuera) detrás del contenido — el equivalente simulado a los
// "blobs" de color que se alcanzan a ver a través del vidrio esmerilado de
// iOS. Van en la base de cada pantalla, antes del contenido real.
@Composable
fun GlassBlobs() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .size(260.dp)
                .align(Alignment.TopStart)
                .offset((-70).dp, (-50).dp)
                .background(Brush.radialGradient(listOf(TadeoBlue.copy(alpha = 0.16f), Color.Transparent)), CircleShape)
        )
        Box(
            Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(90.dp, (-40).dp)
                .background(Brush.radialGradient(listOf(TadeoAqua.copy(alpha = 0.16f), Color.Transparent)), CircleShape)
        )
        Box(
            Modifier
                .size(320.dp)
                .align(Alignment.BottomCenter)
                .offset(0.dp, 130.dp)
                .background(Brush.radialGradient(listOf(TadeoBlue.copy(alpha = 0.10f), Color.Transparent)), CircleShape)
        )
    }
}

// La "tarjeta de vidrio" en la que se apoya casi toda la interfaz: sombra
// suave y difusa, esquinas bien redondeadas, relleno blanco semitransparente
// en degradado (más opaco arriba-izquierda, como un reflejo de luz) y un
// borde muy claro que marca el filo del "vidrio". Se usa como modifier, así
// que cualquier Box/Column/Surface puede volverse una tarjeta de vidrio.
fun Modifier.glassPanel(shape: Shape = RoundedCornerShape(22.dp), tint: Color = Color.White): Modifier = this
    .shadow(16.dp, shape, ambientColor = TadeoBlue.copy(alpha = 0.16f), spotColor = TadeoBlue.copy(alpha = 0.16f))
    .clip(shape)
    .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.88f), tint.copy(alpha = 0.62f))))
    .border(1.dp, Color.White.copy(alpha = 0.75f), shape)

// Versión más sutil para elementos pequeños (chips, pastillas de búsqueda,
// campos de texto) donde una sombra tan marcada se vería pesada.
fun Modifier.glassPill(shape: Shape = CircleShape): Modifier = this
    .shadow(6.dp, shape, ambientColor = TadeoBlue.copy(alpha = 0.10f), spotColor = TadeoBlue.copy(alpha = 0.10f))
    .clip(shape)
    .background(Color.White.copy(alpha = 0.78f))
    .border(1.dp, Color.White.copy(alpha = 0.8f), shape)

// Anillo de carga con degradado girando — el equivalente "bonito" al
// CircularProgressIndicator plano que traía Material por defecto. Se usa en
// todos los estados de carga de la app (Módulos 02/03/05 y Acceso).
@Composable
fun GlassSpinner(modifier: Modifier = Modifier, size: Dp = 46.dp, strokeWidth: Dp = 4.dp) {
    val transition = rememberInfiniteTransition(label = "glassSpinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1100, easing = LinearEasing)),
        label = "glassSpinnerRotation"
    )
    val sweep = Brush.sweepGradient(listOf(Color.Transparent, TadeoAqua, TadeoBlue, Color.Transparent))
    Canvas(modifier = modifier.size(size).rotate(rotation)) {
        drawArc(
            brush = sweep,
            startAngle = 0f,
            sweepAngle = 300f,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}

// Degradado animado para los esqueletos ("skeleton") de carga de listas —
// en vez de una rueda girando sola, los resultados de búsqueda muestran de
// una vez la silueta de las tarjetas de profesor con un brillo que se
// desliza, igual que los loaders de apps de Apple.
@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(animation = tween(1100, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "shimmerTranslate"
    )
    return Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.35f)),
        start = Offset(translate - 220f, 0f),
        end = Offset(translate, 220f)
    )
}

// Un bloque rectangular con el brillo de rememberShimmerBrush() — el ladrillo
// básico para armar cualquier silueta de carga (línea de texto, círculo de
// avatar, etc.).
@Composable
fun ShimmerBlock(modifier: Modifier, shape: Shape = RoundedCornerShape(8.dp)) {
    val brush = rememberShimmerBrush()
    Box(
        modifier
            .clip(shape)
            .background(Color(0xFFE7ECF4))
            .background(brush)
    )
}
