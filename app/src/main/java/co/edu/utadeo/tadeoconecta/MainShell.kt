package co.edu.utadeo.tadeoconecta

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.utadeo.tadeoconecta.auth.UserRole
import co.edu.utadeo.tadeoconecta.ui.theme.*

private enum class StudentTab { Home, Search, Favorites, Profile }
private enum class ProfessorTab { Home, Results, Improve, Profile }
private enum class StudentOverlay { None, Notifications, Professor }
private enum class ProfessorOverlay { None, Notifications }

private data class ProfessorPreview(
    val initials: String,
    val name: String,
    val subject: String,
    val career: String,
    val rating: String,
    val reviews: String,
    val tags: List<String>,
    val bio: String = "Profesor de programación y arquitectura de software. Enfoque práctico y acompañamiento cercano."
)

private val professors = listOf(
    ProfessorPreview("CA","Carlos Andrés Ramírez","Programación","Ingeniería de Sistemas","4.7","89",listOf("Explica muy bien","Clases organizadas")),
    ProfessorPreview("LM","Laura Martínez","Programación","Ingeniería de Sistemas","4.8","120",listOf("Excelente docente","Clases dinámicas"),"Docente de desarrollo de software y fundamentos de programación."),
    ProfessorPreview("DT","Daniel Torres","Estructuras de Datos","Ingeniería de Sistemas","4.4","64",listOf("Buen feedback","Retador"),"Profesor de algoritmos y estructuras de datos.")
)

@Composable
fun MainAppShell(role: UserRole, onLogout: () -> Unit) {
    if (role == UserRole.Professor) ProfessorShell(onLogout) else StudentShell(onLogout)
}

@Composable
private fun StudentShell(onLogout: () -> Unit) {
    var tab by remember { mutableStateOf(StudentTab.Home) }
    var overlay by remember { mutableStateOf(StudentOverlay.None) }
    var selectedProfessor by remember { mutableStateOf(professors.first()) }
    var favorites by remember { mutableStateOf(setOf("LM")) }

    Surface(Modifier.fillMaxSize(), color = TadeoCanvas) {
        Box(Modifier.fillMaxSize()) {
            when (overlay) {
                StudentOverlay.Notifications -> NotificationsScreen(onBack = { overlay = StudentOverlay.None })
                StudentOverlay.Professor -> ProfessorDetailScreen(
                    professor = selectedProfessor,
                    favorite = favorites.contains(selectedProfessor.initials),
                    onToggleFavorite = {
                        favorites = if (favorites.contains(selectedProfessor.initials)) favorites - selectedProfessor.initials else favorites + selectedProfessor.initials
                    },
                    onBack = { overlay = StudentOverlay.None }
                )
                StudentOverlay.None -> {
                    when (tab) {
                        StudentTab.Home -> StudentHome(
                            onSearch = { tab = StudentTab.Search },
                            onFavorites = { tab = StudentTab.Favorites },
                            onNotifications = { overlay = StudentOverlay.Notifications },
                            onProfessor = { p -> selectedProfessor = p; overlay = StudentOverlay.Professor }
                        )
                        StudentTab.Search -> StudentSearch(
                            onBack = { tab = StudentTab.Home },
                            onNotifications = { overlay = StudentOverlay.Notifications },
                            onProfessor = { p -> selectedProfessor = p; overlay = StudentOverlay.Professor }
                        )
                        StudentTab.Favorites -> StudentFavorites(
                            favoriteProfessors = professors.filter { favorites.contains(it.initials) },
                            onBack = { tab = StudentTab.Home },
                            onNotifications = { overlay = StudentOverlay.Notifications },
                            onProfessor = { p -> selectedProfessor = p; overlay = StudentOverlay.Professor }
                        )
                        StudentTab.Profile -> StudentProfile(onNotifications = { overlay = StudentOverlay.Notifications }, onLogout = onLogout)
                    }
                    StudentBottomNav(active = tab, modifier = Modifier.align(Alignment.BottomCenter)) { tab = it }
                }
            }
        }
    }
}

@Composable
private fun ProfessorShell(onLogout: () -> Unit) {
    var tab by remember { mutableStateOf(ProfessorTab.Home) }
    var overlay by remember { mutableStateOf(ProfessorOverlay.None) }
    var goalActive by remember { mutableStateOf(false) }
    var goalDone by remember { mutableStateOf(listOf(false, false, false)) }

    Surface(Modifier.fillMaxSize(), color = TadeoCanvas) {
        Box(Modifier.fillMaxSize()) {
            if (overlay == ProfessorOverlay.Notifications) {
                NotificationsScreen(onBack = { overlay = ProfessorOverlay.None })
            } else {
                when (tab) {
                    ProfessorTab.Home -> ProfessorHome(
                        goalActive, goalDone,
                        onImprove = { tab = ProfessorTab.Improve },
                        onResults = { tab = ProfessorTab.Results },
                        onNotifications = { overlay = ProfessorOverlay.Notifications }
                    )
                    ProfessorTab.Results -> ProfessorResults(onBack = { tab = ProfessorTab.Home }, onNotifications = { overlay = ProfessorOverlay.Notifications })
                    ProfessorTab.Improve -> ProfessorImprove(
                        goalActive = goalActive,
                        goalDone = goalDone,
                        onBack = { tab = ProfessorTab.Home },
                        onToggleGoal = {
                            goalActive = !goalActive
                            if (!goalActive) goalDone = listOf(false, false, false)
                        },
                        onToggleAction = { idx -> goalDone = goalDone.mapIndexed { i, v -> if (i == idx) !v else v } }
                    )
                    ProfessorTab.Profile -> ProfessorProfile(onNotifications = { overlay = ProfessorOverlay.Notifications }, onLogout = onLogout)
                }
                ProfessorBottomNav(active = tab, goalActive = goalActive, modifier = Modifier.align(Alignment.BottomCenter)) { tab = it }
            }
        }
    }
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 96.dp),
        content = content
    )
}

@Composable
private fun HeaderBrand(onNotifications: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.utadeo_logo), "UTadeo", Modifier.width(152.dp).height(33.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.weight(1f))
        BellButton(onNotifications)
    }
}

@Composable
private fun BellButton(onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Color.White).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("◇", color = TadeoUTBlue, fontSize = 20.sp)
        Box(Modifier.size(7.dp).align(Alignment.TopEnd).offset((-7).dp, 7.dp).background(TadeoError, CircleShape).border(2.dp, Color.White, CircleShape))
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit, onNotifications: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Color.White).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Text("‹", color = TadeoInk, fontSize = 25.sp)
        }
        Text(title, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (onNotifications != null) BellButton(onNotifications) else Spacer(Modifier.size(40.dp))
    }
}

@Composable
private fun HeroCard(title: String, body: String, image: Int, eyebrow: String = "TADEO CONECTA") {
    Box(
        Modifier.fillMaxWidth().height(154.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFEEF5FF), Color(0xFFEAF5FF), Color(0xFFDCF6F3))))
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(TadeoBlue.copy(alpha = 0.045f), 80.dp.toPx(), Offset(size.width + 15.dp.toPx(), -12.dp.toPx()))
            drawCircle(TadeoBlue.copy(alpha = 0.045f), 50.dp.toPx(), Offset(size.width + 15.dp.toPx(), -12.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(20.dp.toPx()))
        }
        Row(Modifier.fillMaxSize().padding(start = 17.dp, top = 16.dp, end = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(eyebrow, color = TadeoAqua, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp)
                Text(title, color = TadeoInk, fontSize = 23.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
                Text(body, color = Color(0xFF43526B), fontSize = 12.5.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Image(painterResource(image), "Cabito", Modifier.width(104.dp).height(120.dp).align(Alignment.Bottom), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
private fun StudentHome(onSearch: () -> Unit, onFavorites: () -> Unit, onNotifications: () -> Unit, onProfessor: (ProfessorPreview) -> Unit) {
    ScreenColumn {
        HeaderBrand(onNotifications)
        Spacer(Modifier.height(16.dp))
        HeroCard("Hola, Karen 👋", "¿Qué profesor quieres consultar hoy?", R.drawable.cabito_welcome)
        SearchPill("Buscar profesor o asignatura…", onSearch)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            QuickCard("⌂", "Explorar carreras", Modifier.weight(1f), onSearch)
            QuickCard("▤", "Asignaturas", Modifier.weight(1f), onSearch)
            QuickCard("♡", "Mis favoritos", Modifier.weight(1f), onFavorites)
        }
        FacultyCard()
        SectionHeader("Consultado recientemente", "Ver más", onSearch)
        ProfessorCard(professors.first(), false) { onProfessor(professors.first()) }
    }
}

@Composable
private fun StudentSearch(onBack: () -> Unit, onNotifications: () -> Unit, onProfessor: (ProfessorPreview) -> Unit) {
    ScreenColumn {
        TopBar("Buscar", onBack = onBack, onNotifications = onNotifications)
        SearchBarVisual("Profesor, asignatura o fortaleza…")
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip("Filtros", false)
            FilterChip("⭐ 4+", false)
            FilterChip("50+ opiniones", false)
        }
        Spacer(Modifier.height(9.dp))
        Eyebrow("Explora o empieza a escribir")
        Text("3 resultados", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 9.dp, bottom = 5.dp))
        professors.forEachIndexed { i, p -> ProfessorCard(p, i == 1) { onProfessor(p) } }
    }
}

@Composable
private fun StudentFavorites(favoriteProfessors: List<ProfessorPreview>, onBack: () -> Unit, onNotifications: () -> Unit, onProfessor: (ProfessorPreview) -> Unit) {
    ScreenColumn {
        TopBar("Favoritos", onBack = onBack, onNotifications = onNotifications)
        Text("Profesores guardados", fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 7.dp))
        Text("Tu lista personal para volver rápidamente a los perfiles que te interesan.", color = TadeoMuted, fontSize = 12.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(8.dp))
        if (favoriteProfessors.isEmpty()) {
            GuideCard(R.drawable.cabito_confused, "Aún no tienes favoritos", "Usa el corazón en resultados o en el perfil de un profesor.")
        } else favoriteProfessors.forEach { p -> ProfessorCard(p, true) { onProfessor(p) } }
    }
}

@Composable
private fun StudentProfile(onNotifications: () -> Unit, onLogout: () -> Unit) {
    ScreenColumn {
        HeaderBrand(onNotifications)
        ProfileHeader("KG", "Karen Gamboa", "Estudiante · Ingeniería de Sistemas", "karen.gamboa@utadeo.edu.co")
        SettingsRows(
            listOf(
                "Datos personales" to "Nombre, foto e información básica",
                "Seguridad" to "Contraseña y sesiones activas",
                "Privacidad" to "Evaluaciones y datos personales",
                "Preferencias" to "Notificaciones y comunicaciones",
                "Accesibilidad" to "Texto y experiencia visual",
                "Ayuda y soporte" to "FAQ, reportes y políticas"
            )
        )
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD2D2))) {
            Text("Cerrar sesión", color = Color(0xFFC84242), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfessorHome(goalActive: Boolean, goalDone: List<Boolean>, onImprove: () -> Unit, onResults: () -> Unit, onNotifications: () -> Unit) {
    ScreenColumn {
        HeaderBrand(onNotifications)
        Spacer(Modifier.height(12.dp))
        Surface(color = Color.Transparent, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFFEEF5FF), Color(0xFFE4F8F5))), RoundedCornerShape(20.dp))) {
            Row(Modifier.padding(14.dp, 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("Panel docente")
                    Text("Hola, José", fontSize = 23.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Resumen de tus evaluaciones académicas.", color = TadeoMuted, fontSize = 11.sp)
                }
                Image(painterResource(R.drawable.cabito_encourage), "Cabito", Modifier.size(64.dp), contentScale = ContentScale.Fit)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Kpi("Valoración", "4.6", "⭐ +0.2 este periodo", Modifier.weight(1f))
            Kpi("Respuestas", "146", "+18 este mes", Modifier.weight(1f))
            Kpi("Participación", "76%", "de cursos evaluados", Modifier.weight(1f))
        }
        SectionHeader("Evolución reciente", "Detalle", onResults)
        MiniChart()
        Text("Categorías", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CategoryCard("Claridad", "4.8", Modifier.weight(1f), onResults)
            CategoryCard("Metodología", "4.5", Modifier.weight(1f), onResults)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CategoryCard("Organización", "4.6", Modifier.weight(1f), onResults)
            CategoryCard("Disponibilidad", "4.4", Modifier.weight(1f), onResults)
        }
        InsightCard(Color(0xFFE4F8F5), "Punto fuerte", "Claridad al explicar conceptos", "Los estudiantes destacan ejemplos concretos y explicaciones paso a paso.")
        InsightCard(Color(0xFFFFF7DB), "Punto por fortalecer", "Espacio para preguntas y acompañamiento", "Es la oportunidad que aparece con mayor recurrencia este periodo.", "Ver recomendaciones →", onImprove)
        if (goalActive) {
            val done = goalDone.count { it }
            CardSurface {
                Eyebrow("Mi objetivo activo")
                Text("Más espacio para preguntas", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Avance del plan", color = TadeoMuted, fontSize = 10.sp); Text("$done de 3", fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                LinearProgressIndicator(progress = { done / 3f }, modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(8.dp).clip(CircleShape), color = TadeoBlue, trackColor = Color(0xFFE7EBF0))
                TextButton(onClick = onImprove, contentPadding = PaddingValues(0.dp)) { Text("Abrir objetivo →", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun ProfessorResults(onBack: () -> Unit, onNotifications: () -> Unit) {
    ScreenColumn {
        TopBar("Mis resultados", onBack, onNotifications)
        Text("Evolución mensual", fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 7.dp))
        MiniChart()
        Text("Por categoría", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        listOf("Claridad" to "4.8", "Metodología" to "4.5", "Organización" to "4.6", "Disponibilidad" to "4.4").forEach { (label, value) ->
            CardSurface { Text(label, fontWeight = FontWeight.Bold); Text("$value / 5", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Text("Ver detalle y comentarios ›", color = TadeoMuted, fontSize = 11.sp) }
        }
    }
}

@Composable
private fun ProfessorImprove(goalActive: Boolean, goalDone: List<Boolean>, onBack: () -> Unit, onToggleGoal: () -> Unit, onToggleAction: (Int) -> Unit) {
    val actions = listOf(
        "Cerrar cada bloque con una pregunta de comprobación",
        "Habilitar un minuto de dudas anónimas antes del cierre",
        "Resumir las dudas frecuentes al inicio de la siguiente sesión"
    )
    val done = goalDone.count { it }
    ScreenColumn {
        TopBar("Plan de mejora", onBack)
        GuideCard(R.drawable.cabito_encourage, "Convierte resultados en acciones.", "Recomendaciones basadas en el punto por fortalecer del periodo.")
        CardSurface {
            Eyebrow("Prioridad actual")
            Text("Más espacio para preguntas", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp))
            Text("Prueba reservar los últimos 8–10 minutos de cada clase para dudas y cierre.", color = TadeoMuted, fontSize = 12.sp, lineHeight = 17.sp)
            if (goalActive) Surface(color = if (done == 3) Color(0xFFE4F8F5) else Color(0xFFE7F0FF), shape = CircleShape, modifier = Modifier.padding(top = 10.dp)) { Text(if (done == 3) "✓ Objetivo completado" else "● Objetivo activo", color = if (done == 3) Color(0xFF087A76) else TadeoBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) }
        }
        CardSurface {
            Text("Acciones recomendadas", fontWeight = FontWeight.Bold)
            if (goalActive) {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Progreso", color = TadeoMuted, fontSize = 10.sp); Text("$done de 3", fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                LinearProgressIndicator(progress = { done / 3f }, modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(8.dp).clip(CircleShape), color = TadeoBlue, trackColor = Color(0xFFE7EBF0))
            }
            actions.forEachIndexed { index, action ->
                if (goalActive) {
                    Row(Modifier.fillMaxWidth().clickable { onToggleAction(index) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = goalDone[index], onCheckedChange = { onToggleAction(index) })
                        Text(action, fontSize = 11.sp, lineHeight = 15.sp, color = if (goalDone[index]) TadeoMuted else TadeoInk)
                    }
                } else Text("${index + 1}. $action.", fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        CardSurface {
            Text("Seguimiento", fontWeight = FontWeight.Bold)
            Text("Compara Disponibilidad y Claridad en el próximo periodo para observar cambios.", color = TadeoMuted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(vertical = 7.dp))
            if (!goalActive) OutlinedButton(onClick = onToggleGoal, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Marcar como objetivo", fontWeight = FontWeight.Bold) }
            else {
                Surface(color = TadeoSoft, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(if (done == 3) "¡Plan completado!" else "Objetivo guardado en tu plan", fontWeight = FontWeight.Bold, fontSize = 11.sp); Text(if (done == 3) "Completaste las tres acciones. Podrás comparar el resultado en el próximo periodo." else "Marca cada acción a medida que la pongas en práctica.", color = TadeoMuted, fontSize = 10.sp) } }
                TextButton(onClick = onToggleGoal, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(if (done == 3) "Reiniciar objetivo" else "Quitar objetivo") }
            }
        }
    }
}

@Composable
private fun ProfessorProfile(onNotifications: () -> Unit, onLogout: () -> Unit) {
    ScreenColumn {
        HeaderBrand(onNotifications)
        ProfileHeader("JF", "José A. Franco C.", "Profesor · Facultad de Ciencias Naturales e Ingeniería", "josea.francoc@utadeo.edu.co")
        SettingsRows(
            listOf(
                "Perfil docente" to "Datos personales y asignaturas",
                "Seguridad" to "Contraseña y sesiones activas",
                "Privacidad y datos" to "Visibilidad y gestión de datos",
                "Preferencias" to "Alertas sobre resultados y comunicaciones",
                "Accesibilidad" to "Texto y reducción de movimiento",
                "Ayuda y soporte" to "Resultados, cuenta, reportes y políticas"
            )
        )
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD2D2))) { Text("Cerrar sesión", color = Color(0xFFC84242), fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun NotificationsScreen(onBack: () -> Unit) {
    ScreenColumn {
        TopBar("Notificaciones", onBack, null)
        GuideCard(R.drawable.cabito_point, "Todo al día.", "Aquí encontrarás novedades importantes.")
        NotificationCard("Evaluación enviada", "Tu opinión sobre Laura Martínez fue registrada.", "Hoy")
        NotificationCard("Nuevo periodo de evaluación", "Ya puedes evaluar tus asignaturas del semestre.", "Ayer")
    }
}

@Composable
private fun ProfessorDetailScreen(professor: ProfessorPreview, favorite: Boolean, onToggleFavorite: () -> Unit, onBack: () -> Unit) {
    ScreenColumn {
        TopBar("Perfil del profesor", onBack, null)
        Surface(color = Color.White, shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, TadeoBorder), modifier = Modifier.fillMaxWidth().padding(top = 9.dp)) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(professor.initials, 76)
                Text(professor.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                Text("${professor.subject} · ${professor.career}", color = TadeoMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                Text("⭐ ${professor.rating}", fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, modifier = Modifier.padding(top = 5.dp))
                Text("${professor.reviews} evaluaciones", color = TadeoMuted, fontSize = 12.sp)
                OutlinedButton(onClick = onToggleFavorite, modifier = Modifier.padding(top = 10.dp), shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD1D2))) { Text(if (favorite) "♥ En favoritos" else "♡ Añadir a favoritos", color = if (favorite) Color(0xFFFF686B) else Color(0xFF526078), fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            }
        }
        GuideCard(R.drawable.cabito_point, "Información para decidir con contexto.", "Revisa fortalezas, resultados y comentarios.")
        Text("Lo que destacan", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { professor.tags.forEach { Tag(it) } }
        Text("Resultados", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 3.dp))
        ScoreRow("Claridad", 4.8f); ScoreRow("Metodología", 4.6f); ScoreRow("Organización", 4.7f); ScoreRow("Disponibilidad", 4.5f)
        CardSurface { Text("Información académica", fontWeight = FontWeight.Bold); Text(professor.bio, color = TadeoMuted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 5.dp)) }
        Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = TadeoBlue)) { Text("Evaluar profesor", fontWeight = FontWeight.Bold) }
        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 8.dp), shape = RoundedCornerShape(14.dp)) { Text("Ver evaluaciones", fontWeight = FontWeight.Bold) }
    }
}

@Composable private fun FacultyCard() {
    Surface(color = Color(0xFFE4F8F5), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text("🌿", fontSize = 28.sp); Spacer(Modifier.width(10.dp)); Column { Text("Facultad de Ciencias Naturales e Ingeniería", fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("Explora profesores por área académica", color = TadeoMuted, fontSize = 11.sp) } }
    }
}

@Composable private fun SearchPill(text: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().padding(top = 13.dp).clickable(onClick = onClick), color = Color.White, shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCFE0FA)), shadowElevation = 4.dp) {
        Row(Modifier.heightIn(min = 50.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) { Text("⌕", color = TadeoBlue, fontSize = 22.sp); Spacer(Modifier.width(10.dp)); Text(text, color = TadeoMuted, fontSize = 13.sp) }
    }
}

@Composable private fun SearchBarVisual(text: String) {
    Surface(modifier = Modifier.fillMaxWidth().padding(top = 13.dp), color = Color.White, shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCFE0FA)), shadowElevation = 4.dp) {
        Row(Modifier.heightIn(min = 50.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) { Text("⌕", color = TadeoBlue, fontSize = 22.sp); Spacer(Modifier.width(10.dp)); Text(text, color = TadeoMuted, fontSize = 13.sp, modifier = Modifier.weight(1f)); Text("☷", color = TadeoBlue, fontSize = 17.sp) }
    }
}

@Composable private fun QuickCard(icon: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.heightIn(min = 76.dp).clickable(onClick = onClick), color = Color.White, shape = RoundedCornerShape(15.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEDF0F4)), shadowElevation = 2.dp) {
        Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(icon, color = TadeoBlue, fontSize = 22.sp); Spacer(Modifier.height(8.dp)); Text(label, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, lineHeight = 12.sp, textAlign = TextAlign.Center) }
    }
}

@Composable private fun SectionHeader(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 19.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f)); TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) { Text(action, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
}

@Composable private fun ProfessorCard(prof: ProfessorPreview, favorite: Boolean, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, TadeoBorder), shadowElevation = 1.dp) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(prof.initials, 54); Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(prof.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${prof.subject} · ${prof.career}", color = TadeoMuted, fontSize = 10.5.sp, lineHeight = 14.sp)
                Text("⭐ ${prof.rating}  (${prof.reviews})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 5.dp)) { prof.tags.take(2).forEach { Tag(it) } }
            }
            Text(if (favorite) "♥" else "♡", color = if (favorite) Color(0xFFFF686B) else Color(0xFF9AA3B0), fontSize = 24.sp)
        }
    }
}

@Composable private fun Avatar(initials: String, size: Int) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(if (size > 60) 22.dp else 15.dp)).background(Brush.linearGradient(listOf(Color(0xFFDCE9FF), Color(0xFFBDE7E5)))), contentAlignment = Alignment.Center) { Text(initials, color = TadeoUTBlue, fontWeight = FontWeight.ExtraBold, fontSize = if (size > 60) 22.sp else 14.sp) }
}

@Composable private fun Tag(text: String) { Surface(color = Color(0xFFE4F8F5), shape = CircleShape) { Text(text, color = Color(0xFF087A76), fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) } }
@Composable private fun FilterChip(text: String, selected: Boolean) { Surface(color = if (selected) Color(0xFFE7F0FF) else Color.White, shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Color(0xFFBDD3FF) else TadeoBorder)) { Text(text, color = if (selected) TadeoBlue else TadeoInk, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) } }
@Composable private fun Eyebrow(text: String) { Text(text.uppercase(), color = TadeoAqua, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.75.sp) }

@Composable private fun GuideCard(image: Int, title: String, body: String) {
    Surface(color = Color(0xFFEDF5FF), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(Modifier.padding(10.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) { Image(painterResource(image), "Cabito", Modifier.size(54.dp), contentScale = ContentScale.Fit); Spacer(Modifier.width(11.dp)); Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(body, color = TadeoMuted, fontSize = 11.sp, lineHeight = 16.sp) } }
    }
}

@Composable private fun ProfileHeader(initials: String, name: String, subtitle: String, email: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) { Avatar(initials, 72); Spacer(Modifier.width(14.dp)); Column { Text(name, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp); Text(subtitle, color = TadeoMuted, fontSize = 11.sp, lineHeight = 15.sp); Text(email, color = TadeoMuted, fontSize = 10.sp) } }
}

@Composable private fun SettingsRows(rows: List<Pair<String, String>>) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, TadeoBorder), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 4.dp)) {
            rows.forEachIndexed { i, (title, desc) ->
                if (i > 0) HorizontalDivider(color = Color(0xFFF0F2F5))
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(desc, color = TadeoMuted, fontSize = 11.sp, lineHeight = 15.sp) }; Text("›", color = TadeoMuted, fontSize = 20.sp) }
            }
        }
    }
}

@Composable private fun NotificationCard(title: String, body: String, time: String) { CardSurface { Text(title, fontWeight = FontWeight.Bold); Text(body, color = TadeoMuted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp)); Text(time, color = TadeoMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp)) } }

@Composable private fun ScoreRow(label: String, value: Float) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(92.dp)); Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(Color(0xFFEDF0F4))) { Box(Modifier.fillMaxWidth(value / 5f).fillMaxHeight().background(TadeoAqua)) }; Text(String.format("%.1f", value), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 10.dp)) }
}

@Composable private fun Kpi(label: String, value: String, note: String, modifier: Modifier) { Surface(modifier, shape = RoundedCornerShape(15.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, TadeoBorder)) { Column(Modifier.padding(horizontal = 9.dp, vertical = 11.dp)) { Text(label, color = TadeoMuted, fontSize = 8.5.sp); Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, modifier = Modifier.padding(vertical = 3.dp)); Text(note, color = TadeoMuted, fontSize = 8.sp, lineHeight = 10.sp) } } }

@Composable private fun MiniChart() { CardSurface { val values = listOf(0.84f,0.86f,0.90f,0.88f,0.92f,0.94f); Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Bottom) { values.forEach { v -> Box(Modifier.weight(1f).fillMaxHeight(v).clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp)).background(Brush.verticalGradient(listOf(TadeoBlue, Color(0xFF8BB4FF))))) } }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("Mar","Abr","May","Jun","Jul","Ago").forEach { Text(it, color = TadeoMuted, fontSize = 8.sp) } } } }

@Composable private fun CategoryCard(label: String, value: String, modifier: Modifier, onClick: () -> Unit) { Surface(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(15.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, TadeoBorder)) { Column(Modifier.padding(12.dp)) { Text(label, color = TadeoMuted, fontSize = 10.sp); Row(verticalAlignment = Alignment.Bottom) { Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp); Text(" / 5", color = TadeoMuted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 2.dp)) } } } }

@Composable private fun InsightCard(color: Color, eyebrow: String, title: String, body: String, action: String? = null, onClick: (() -> Unit)? = null) { Surface(color = color, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Column(Modifier.padding(13.dp)) { Eyebrow(eyebrow); Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)); Text(body, color = TadeoMuted, fontSize = 10.5.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 4.dp)); if (action != null && onClick != null) TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) { Text(action, fontSize = 11.sp, fontWeight = FontWeight.Bold) } } } }

@Composable private fun CardSurface(content: @Composable ColumnScope.() -> Unit) { Surface(color = Color.White, shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, TadeoBorder), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), shadowElevation = 1.dp) { Column(Modifier.padding(14.dp), content = content) } }

@Composable private fun StudentBottomNav(active: StudentTab, modifier: Modifier = Modifier, onSelect: (StudentTab) -> Unit) { BottomNav(listOf(Triple(StudentTab.Home.name,"⌂","Inicio"),Triple(StudentTab.Search.name,"⌕","Buscar"),Triple(StudentTab.Favorites.name,"♡","Favoritos"),Triple(StudentTab.Profile.name,"○","Perfil")), active.name, modifier) { onSelect(StudentTab.valueOf(it)) } }
@Composable private fun ProfessorBottomNav(active: ProfessorTab, goalActive: Boolean, modifier: Modifier = Modifier, onSelect: (ProfessorTab) -> Unit) { BottomNav(listOf(Triple(ProfessorTab.Home.name,"⌂","Inicio"),Triple(ProfessorTab.Results.name,"▥","Resultados"),Triple(ProfessorTab.Improve.name,"↗",if(goalActive)"Mejorar •" else "Mejorar"),Triple(ProfessorTab.Profile.name,"○","Perfil")), active.name, modifier) { onSelect(ProfessorTab.valueOf(it)) } }

@Composable private fun BottomNav(items: List<Triple<String,String,String>>, active: String, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    Surface(modifier = modifier.fillMaxWidth().navigationBarsPadding(), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, TadeoBorder)) {
        Row(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 8.dp, vertical = 7.dp)) {
            items.forEach { (id, icon, label) -> val selected = id == active; Column(Modifier.weight(1f).fillMaxHeight().clickable { onSelect(id) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(icon, color = if(selected) TadeoBlue else Color(0xFF637086), fontSize = 20.sp); Text(label, color = if(selected) TadeoBlue else Color(0xFF637086), fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold) } }
        }
    }
}
