package co.edu.utadeo.tadeoconecta

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.utadeo.tadeoconecta.accessibility.AccessibilityPreferences
import co.edu.utadeo.tadeoconecta.auth.UserRole
import co.edu.utadeo.tadeoconecta.catalog.FacultyCareerPicker
import co.edu.utadeo.tadeoconecta.catalog.FacultyCareerSubjectPicker
import co.edu.utadeo.tadeoconecta.domain.model.AiRecommendation
import co.edu.utadeo.tadeoconecta.domain.model.AiReportVerdict
import co.edu.utadeo.tadeoconecta.domain.model.AppRole
import co.edu.utadeo.tadeoconecta.domain.model.ImprovementPlanRow
import co.edu.utadeo.tadeoconecta.domain.model.MIN_NEW_REVIEWS_FOR_COMPARISON
import co.edu.utadeo.tadeoconecta.domain.model.IMPROVEMENT_THRESHOLD
import co.edu.utadeo.tadeoconecta.domain.model.PlanProgress
import co.edu.utadeo.tadeoconecta.domain.model.Career
import co.edu.utadeo.tadeoconecta.domain.model.CategoryBreakdown
import co.edu.utadeo.tadeoconecta.domain.model.Faculty
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorFeedbackNote
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorImprovementGoal
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorRatingSummary
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorSummary
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorTips
import co.edu.utadeo.tadeoconecta.domain.model.RatingCards
import co.edu.utadeo.tadeoconecta.domain.model.ReportReason
import co.edu.utadeo.tadeoconecta.domain.model.RatingCategory
import co.edu.utadeo.tadeoconecta.domain.model.RatingFacts
import co.edu.utadeo.tadeoconecta.domain.model.RecommendFor
import co.edu.utadeo.tadeoconecta.domain.model.Subject
import co.edu.utadeo.tadeoconecta.notifications.NotificationsError
import co.edu.utadeo.tadeoconecta.notifications.NotificationsScreen as NotificationsScreenState
import co.edu.utadeo.tadeoconecta.notifications.NotificationsViewModel
import co.edu.utadeo.tadeoconecta.professor.ProfessorDashboardError
import co.edu.utadeo.tadeoconecta.professor.ProfessorDashboardScreen
import co.edu.utadeo.tadeoconecta.professor.ProfessorDashboardUiState
import co.edu.utadeo.tadeoconecta.professor.ProfessorDashboardViewModel
import co.edu.utadeo.tadeoconecta.profile.ProfileError
import co.edu.utadeo.tadeoconecta.profile.ProfileScreen
import co.edu.utadeo.tadeoconecta.profile.ProfileViewModel
import co.edu.utadeo.tadeoconecta.rating.MIN_COMMENT_WORDS
import co.edu.utadeo.tadeoconecta.rating.MIN_RECOMMEND_REASON_WORDS
import co.edu.utadeo.tadeoconecta.rating.MIN_REVIEWS_TO_SHOW_COMMENTS
import co.edu.utadeo.tadeoconecta.rating.MIN_TIP_WORDS
import co.edu.utadeo.tadeoconecta.rating.RatingError
import co.edu.utadeo.tadeoconecta.rating.RatingFormState
import co.edu.utadeo.tadeoconecta.rating.RatingScreen
import co.edu.utadeo.tadeoconecta.rating.RatingUiState
import co.edu.utadeo.tadeoconecta.rating.RatingViewModel
import co.edu.utadeo.tadeoconecta.rating.StepModerationResult
import co.edu.utadeo.tadeoconecta.rating.TextQuality
import co.edu.utadeo.tadeoconecta.domain.model.AttendanceBands
import co.edu.utadeo.tadeoconecta.rating.isValidComment
import co.edu.utadeo.tadeoconecta.search.SearchError
import co.edu.utadeo.tadeoconecta.search.SearchScreen
import co.edu.utadeo.tadeoconecta.search.SearchViewModel
import co.edu.utadeo.tadeoconecta.ui.theme.*
import coil.compose.AsyncImage
import java.io.ByteArrayOutputStream

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
    val bio: String = "Este profesor aún no ha agregado información académica adicional.",
    // Id real de Supabase: lo usan favoritos (student_favorites) y "consultado
    // recientemente" para saber de qué profesor se trata.
    val id: String
)

// "Programación · Ingeniería de Sistemas" solo si hay carrera real que
// mostrar — evita el " · " colgado que quedaba cuando career venía vacío.
private fun ProfessorPreview.subjectCareerLabel(): String =
    if (career.isBlank()) subject else "$subject · $career"

// Módulo 03 — adapta un resultado real de búsqueda (Supabase) al modelo visual
// ProfessorPreview que ya usan ProfessorCard/ProfessorDetailScreen. La carrera
// ahora sale de verdad de subject_careers/careers (Módulo 12), no del viejo
// truco de "la segunda materia que dicta". rating/reviews ya vienen de
// professor_ratings (Módulo 05) vía ProfessorSummary.averageRating/reviewCount;
// "—" solo cuando de verdad no hay ninguna calificación todavía.
private fun ProfessorSummary.toPreview(): ProfessorPreview {
    val initials = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        .take(2).joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }
    return ProfessorPreview(
        initials = initials,
        name = fullName,
        subject = subjectNames.firstOrNull() ?: (professionalTitle ?: "Profesor"),
        career = careerNames.firstOrNull().orEmpty(),
        // Módulo 05 — promedio real de professor_ratings; "—" solo cuando de
        // verdad no hay ninguna calificación todavía, nunca un número fijo.
        rating = averageRating?.let { String.format("%.1f", it) } ?: "—",
        reviews = reviewCount.toString(),
        // Revisión general — antes quedaba siempre vacío. Acá van las materias
        // ADICIONALES a la que ya se muestra en subjectCareerLabel() (la
        // primera), para que ProfessorCard muestre todo lo que dicta, no solo
        // una. "Lo que destacan" en ProfessorOverviewContent usa otra fuente
        // (recommendForCounts) — no este campo.
        tags = subjectNames.drop(1),
        bio = bio?.takeIf { it.isNotBlank() } ?: "Este profesor aún no ha agregado información académica adicional.",
        id = id
    )
}

@Composable
fun MainAppShell(role: UserRole, onLogout: () -> Unit) {
    val profileVm: ProfileViewModel = viewModel()
    // viewModel() la resuelve por Activity, no por sesión: si alguien cierra
    // sesión y otra persona entra con otra cuenta sin matar la app, seguiría
    // siendo la misma instancia. MainAppShell se recompone desde cero en cada
    // login (viene de la rama Login/registro del AuthScreen), así que este
    // LaunchedEffect(Unit) sí se vuelve a disparar por cada sesión nueva y
    // trae el perfil correcto en vez del de quien entró antes.
    LaunchedEffect(Unit) { profileVm.load() }
    // Módulo 07 — una sola instancia compartida entre StudentShell y
    // ProfessorShell (mismo motivo que profileVm): la campana y la pantalla
    // de notificaciones son del usuario autenticado, no de un rol en
    // particular, así que no tiene sentido tener dos estados separados que
    // no se enteren entre sí.
    val notificationsVm: NotificationsViewModel = viewModel()
    LaunchedEffect(Unit) { notificationsVm.refreshBadgeIfNeeded() }
    if (role == UserRole.Professor) ProfessorShell(profileVm, notificationsVm, onLogout) else StudentShell(profileVm, notificationsVm, onLogout)
}

@Composable
private fun StudentShell(profileVm: ProfileViewModel, notificationsVm: NotificationsViewModel, onLogout: () -> Unit) {
    var tab by remember { mutableStateOf(StudentTab.Home) }
    var overlay by remember { mutableStateOf(StudentOverlay.None) }
    var selectedProfessor by remember { mutableStateOf<ProfessorPreview?>(null) }
    // Último profesor real que el estudiante abrió en esta sesión — ya no un
    // profesor de ejemplo fijo. Vive en memoria (no en Supabase): es "lo que
    // viste hace un momento", no un historial que deba sobrevivir a cerrar la app.
    var recentlyViewed by remember { mutableStateOf<ProfessorPreview?>(null) }
    // "Explorar carreras"/"Asignaturas" en Inicio llevan a Buscar y además
    // piden que se abra ya el diálogo de filtro correspondiente.
    var openCareerFilterOnEntry by remember { mutableStateOf(false) }
    var openSubjectFilterOnEntry by remember { mutableStateOf(false) }
    val searchVm: SearchViewModel = viewModel()
    val ratingVm: RatingViewModel = viewModel()
    // Mismo cuidado que con profileVm: esta instancia vive mientras viva la
    // Activity, así que la carga inicial se dispara aquí (una vez por sesión
    // nueva de StudentShell), no en el init del ViewModel.
    LaunchedEffect(Unit) { searchVm.start() }
    val profileTakeover = tab == StudentTab.Profile && profileVm.state.screen != ProfileScreen.Overview
    val onProfessor: (ProfessorPreview) -> Unit = { p ->
        selectedProfessor = p
        recentlyViewed = p
        overlay = StudentOverlay.Professor
    }

    // Revisión general: sin esto, el botón/gesto de volver del sistema no
    // cerraba el perfil de un profesor ni la campana — salía de la app
    // directo. Si el perfil del profesor tiene abierto el formulario de
    // evaluación o las reseñas, un primer "atrás" vuelve al resumen (igual
    // que la flechita de ProfessorDetailScreen) antes de cerrar la tarjeta
    // completa.
    BackHandler(enabled = overlay != StudentOverlay.None || profileTakeover) {
        when {
            overlay == StudentOverlay.Professor && ratingVm.state.screen != RatingScreen.Overview -> ratingVm.backToOverview()
            overlay != StudentOverlay.None -> overlay = StudentOverlay.None
            profileTakeover -> profileVm.backToOverview()
        }
    }

    Box(Modifier.fillMaxSize().background(GlassBackdropBrush)) {
        GlassBlobs()
        Box(Modifier.fillMaxSize()) {
            when (overlay) {
                StudentOverlay.Notifications -> NotificationsScreen(vm = notificationsVm, onBack = { overlay = StudentOverlay.None })
                StudentOverlay.Professor -> selectedProfessor?.let { prof ->
                    // Cada profesor distinto recarga su propio resumen de
                    // calificaciones (Módulo 05) — ratingVm es una sola
                    // instancia reutilizada, no una por profesor.
                    LaunchedEffect(prof.id) { ratingVm.load(prof.id) }
                    ProfessorDetailScreen(
                        professor = prof,
                        favorite = searchVm.isFavorite(prof.id),
                        onToggleFavorite = { searchVm.toggleFavorite(prof.id) },
                        ratingVm = ratingVm,
                        onBack = { overlay = StudentOverlay.None }
                    )
                }
                StudentOverlay.None -> {
                    when (tab) {
                        StudentTab.Home -> StudentHome(
                            studentName = profileVm.state.profile?.fullName,
                            recentlyViewed = recentlyViewed,
                            isFavorite = searchVm::isFavorite,
                            faculties = searchVm.state.faculties,
                            careers = searchVm.state.careers,
                            onSearch = { tab = StudentTab.Search },
                            onExploreCareers = { openCareerFilterOnEntry = true; tab = StudentTab.Search },
                            onExploreSubjects = { openSubjectFilterOnEntry = true; tab = StudentTab.Search },
                            onFavorites = { tab = StudentTab.Favorites },
                            onNotifications = { overlay = StudentOverlay.Notifications },
                            hasUnread = notificationsVm.state.hasUnread,
                            onProfessor = onProfessor
                        )
                        StudentTab.Search -> StudentSearch(
                            vm = searchVm,
                            isFavorite = searchVm::isFavorite,
                            openSubjectFilterOnEntry = openSubjectFilterOnEntry,
                            openCareerFilterOnEntry = openCareerFilterOnEntry,
                            onFilterPromptConsumed = { openSubjectFilterOnEntry = false; openCareerFilterOnEntry = false },
                            // Al volver a Inicio se limpia el filtro de materia/carrera
                            // de esta búsqueda — la próxima vez que se abra el buscador
                            // (por el ícono o por "Explorar carreras/Asignaturas") empieza
                            // en blanco, no con el filtro que quedó puesto.
                            onBack = { searchVm.resetFilters(); tab = StudentTab.Home },
                            onNotifications = { overlay = StudentOverlay.Notifications },
                            hasUnread = notificationsVm.state.hasUnread,
                            onProfessor = onProfessor
                        )
                        StudentTab.Favorites -> StudentFavorites(
                            favoriteProfessors = searchVm.state.favoriteProfessors.map { it.toPreview() },
                            // Mismo motivo que al volver de Buscar: cualquier camino de
                            // vuelta a Inicio debe dejar el buscador limpio.
                            onBack = { searchVm.resetFilters(); tab = StudentTab.Home },
                            onNotifications = { overlay = StudentOverlay.Notifications },
                            hasUnread = notificationsVm.state.hasUnread,
                            onProfessor = onProfessor
                        )
                        StudentTab.Profile -> StudentProfile(vm = profileVm, onNotifications = { overlay = StudentOverlay.Notifications }, hasUnread = notificationsVm.state.hasUnread, onLogout = onLogout)
                    }
                    if (!profileTakeover) StudentBottomNav(active = tab, modifier = Modifier.align(Alignment.BottomCenter)) { newTab ->
                        // Mismo motivo que el botón de volver de Buscar: tocar
                        // "Inicio" en la barra inferior también debe dejar el
                        // buscador limpio para la próxima vez.
                        if (newTab == StudentTab.Home) searchVm.resetFilters()
                        tab = newTab
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfessorShell(profileVm: ProfileViewModel, notificationsVm: NotificationsViewModel, onLogout: () -> Unit) {
    var tab by remember { mutableStateOf(ProfessorTab.Home) }
    var overlay by remember { mutableStateOf(ProfessorOverlay.None) }
    // Módulo 08/09 — un solo ViewModel para Inicio/Resultados/Mejorar: las
    // tres pestañas leen del mismo resumen+feedback real, solo cambia qué
    // parte de ese mismo estado se muestra. loadIfNeeded() solo pide datos
    // la primera vez que se entra al panel en esta sesión.
    val dashboardVm: ProfessorDashboardViewModel = viewModel()
    LaunchedEffect(Unit) { dashboardVm.loadIfNeeded() }
    val dashState = dashboardVm.state
    val profileTakeover = tab == ProfessorTab.Profile && profileVm.state.screen != ProfileScreen.Overview

    // Revisión general: mismo arreglo que en StudentShell — sin esto, el
    // botón/gesto de volver del sistema no cerraba la campana ni las
    // pantallas del perfil docente, salía de la app directo.
    BackHandler(enabled = overlay != ProfessorOverlay.None || profileTakeover) {
        when {
            overlay != ProfessorOverlay.None -> overlay = ProfessorOverlay.None
            profileTakeover -> profileVm.backToOverview()
        }
    }

    Box(Modifier.fillMaxSize().background(GlassBackdropBrush)) {
        GlassBlobs()
        Box(Modifier.fillMaxSize()) {
            if (overlay == ProfessorOverlay.Notifications) {
                NotificationsScreen(vm = notificationsVm, onBack = { overlay = ProfessorOverlay.None })
            } else {
                when (tab) {
                    ProfessorTab.Profile -> ProfessorProfile(vm = profileVm, onNotifications = { overlay = ProfessorOverlay.Notifications }, hasUnread = notificationsVm.state.hasUnread, onLogout = onLogout)
                    else -> when (dashState.screen) {
                        ProfessorDashboardScreen.Loading -> ProfessorDashboardLoadingState()
                        ProfessorDashboardScreen.Error -> ProfessorDashboardErrorState(dashState.error ?: ProfessorDashboardError.LoadFailed) { dashboardVm.retryFromError() }
                        else -> when (tab) {
                            ProfessorTab.Home -> ProfessorHome(
                                state = dashState,
                                onImprove = { tab = ProfessorTab.Improve },
                                onResults = { tab = ProfessorTab.Results },
                                onNotifications = { overlay = ProfessorOverlay.Notifications },
                                hasUnread = notificationsVm.state.hasUnread
                            )
                            ProfessorTab.Results -> ProfessorResults(
                                state = dashState,
                                onBack = { tab = ProfessorTab.Home },
                                onNotifications = { overlay = ProfessorOverlay.Notifications },
                                hasUnread = notificationsVm.state.hasUnread,
                                onOpenReportDialog = { ratingId -> dashboardVm.openReportDialog(ratingId) },
                                onCloseReportDialog = { dashboardVm.closeReportDialog() },
                                onSubmitReport = { ratingId, reason -> dashboardVm.submitReport(ratingId, reason) }
                            )
                            ProfessorTab.Improve -> ProfessorImprove(
                                state = dashState,
                                onBack = { tab = ProfessorTab.Home },
                                onEnter = { dashboardVm.ensureAiRecommendations() },
                                onGenerateAi = { dashboardVm.generateAiRecommendations() },
                                onAcceptPlan = { dashboardVm.acceptPlan() },
                                onDiscardDraft = { dashboardVm.discardDraft() },
                                onOpenUpdate = { dashboardVm.openUpdateDialog() },
                                onCloseUpdate = { dashboardVm.closeUpdateDialog() },
                                onUpdateWithReason = { reason -> dashboardVm.generateAiRecommendations(reason) },
                                onToggleApplied = { index -> dashboardVm.toggleApplied(index) },
                                onCloseCut = { dashboardVm.closeCut() }
                            )
                            else -> Unit
                        }
                    }
                }
                if (!profileTakeover) ProfessorBottomNav(active = tab, modifier = Modifier.align(Alignment.BottomCenter)) { tab = it }
            }
        }
    }
}

@Composable
private fun ProfessorDashboardLoadingState() {
    ScreenColumn {
        HeaderBrand(onNotifications = {})
        Column(Modifier.fillMaxWidth().padding(top = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GlassSpinner()
            Text("Cargando tu panel…", color = TadeoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun ProfessorDashboardErrorState(error: ProfessorDashboardError, onRetry: () -> Unit) {
    ScreenColumn {
        HeaderBrand(onNotifications = {})
        GuideCard(R.drawable.cabito_confused, error.title, error.message)
        Text(error.recovery, color = TadeoMuted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 10.dp))
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text("Reintentar", fontWeight = FontWeight.Bold)
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

// El logo ahora vive dentro de su propia pastilla de vidrio (glassPill),
// igual que la campana al lado — antes el PNG traía un fondo casi blanco
// propio que se notaba como un rectángulo suelto sobre el fondo con manchas
// de color; así queda "fundido" con el mismo lenguaje visual del resto.
@Composable
private fun HeaderBrand(onNotifications: () -> Unit, hasUnread: Boolean = false) {
    Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.height(42.dp).glassPill(RoundedCornerShape(14.dp)).padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(R.drawable.utadeo_logo), "UTadeo", Modifier.width(128.dp).height(27.dp), contentScale = ContentScale.Fit)
        }
        Spacer(Modifier.weight(1f))
        BellButton(onNotifications, hasUnread)
    }
}

// Módulo 07 — el punto rojo ahora refleja si de verdad hay notificaciones
// sin leer (antes estaba fijo, siempre encendido, sin relación con nada).
@Composable
private fun BellButton(onClick: () -> Unit, hasUnread: Boolean = false) {
    Box(
        Modifier.size(40.dp).glassPill(RoundedCornerShape(13.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones", tint = TadeoUTBlue, modifier = Modifier.size(19.dp))
        if (hasUnread) {
            Box(Modifier.size(7.dp).align(Alignment.TopEnd).offset((-7).dp, 7.dp).background(TadeoError, CircleShape).border(2.dp, Color.White, CircleShape))
        }
    }
}

// Barra superior "flotante": el botón de volver y la campana ya no son
// cuadritos blancos planos, son pastillas de vidrio (glassPill) que se ven
// apoyadas sobre el fondo con manchas de color (GlassBlobs).
@Composable
private fun TopBar(title: String, onBack: () -> Unit, onNotifications: (() -> Unit)? = null, hasUnread: Boolean = false) {
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).glassPill(RoundedCornerShape(13.dp)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TadeoInk, modifier = Modifier.size(19.dp))
        }
        Text(title, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (onNotifications != null) BellButton(onNotifications, hasUnread) else Spacer(Modifier.size(40.dp))
    }
}

@Composable
private fun HeroCard(title: String, body: String, image: Int, eyebrow: String = "TADEO CONECTA") {
    val heroShape = RoundedCornerShape(24.dp)
    Box(
        Modifier.fillMaxWidth().height(154.dp)
            .shadow(14.dp, heroShape, ambientColor = TadeoBlue.copy(alpha = 0.18f), spotColor = TadeoBlue.copy(alpha = 0.18f))
            .clip(heroShape)
            .background(Brush.linearGradient(listOf(Color(0xFFEEF5FF), Color(0xFFEAF5FF), Color(0xFFDCF6F3))))
            .border(1.dp, Color.White.copy(alpha = 0.8f), heroShape)
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
private fun StudentHome(
    studentName: String?,
    recentlyViewed: ProfessorPreview?,
    isFavorite: (String) -> Boolean,
    faculties: List<Faculty>,
    careers: List<Career>,
    onSearch: () -> Unit,
    onExploreCareers: () -> Unit,
    onExploreSubjects: () -> Unit,
    onFavorites: () -> Unit,
    onNotifications: () -> Unit,
    hasUnread: Boolean,
    onProfessor: (ProfessorPreview) -> Unit
) {
    ScreenColumn {
        HeaderBrand(onNotifications, hasUnread)
        Spacer(Modifier.height(16.dp))
        HeroCard(greetingFor(studentName), "¿Qué profesor quieres consultar hoy?", R.drawable.cabito_welcome)
        SearchPill("Buscar profesor o asignatura…", onSearch)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
            QuickCard(Icons.Filled.School, "Explorar carreras", Modifier.weight(1f), onExploreCareers)
            QuickCard(Icons.Filled.Subject, "Asignaturas", Modifier.weight(1f), onExploreSubjects)
            QuickCard(Icons.Filled.FavoriteBorder, "Mis favoritos", Modifier.weight(1f), onFavorites)
        }
        FacultyCard(facultyCount = faculties.size, careerCount = careers.size, onClick = onExploreCareers)
        SectionHeader("Consultado recientemente", "Ver más", onSearch)
        if (recentlyViewed != null) {
            ProfessorCard(recentlyViewed, isFavorite(recentlyViewed.id)) { onProfessor(recentlyViewed) }
        } else {
            GuideCard(R.drawable.cabito_confused, "Aún no has consultado a ningún profesor", "Busca por nombre o materia y aparecerá aquí para que vuelvas rápido.")
        }
    }
}

// Saludo real con el nombre del estudiante (primer nombre, para que no se
// corte en la tarjeta) en vez del "Hola, Karen" fijo que quedaba del mockup.
// Mientras el perfil todavía está cargando (profile == null un instante)
// cae en un saludo genérico en vez de inventar un nombre.
private fun greetingFor(fullName: String?): String {
    val first = fullName?.trim()?.split(Regex("\\s+"))?.firstOrNull { it.isNotBlank() }
    return if (first.isNullOrBlank()) "Hola 👋" else "Hola, $first 👋"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentSearch(
    vm: SearchViewModel,
    isFavorite: (String) -> Boolean,
    openSubjectFilterOnEntry: Boolean,
    openCareerFilterOnEntry: Boolean,
    onFilterPromptConsumed: () -> Unit,
    onBack: () -> Unit,
    onNotifications: () -> Unit,
    hasUnread: Boolean,
    onProfessor: (ProfessorPreview) -> Unit
) {
    var showSubjectDialog by remember { mutableStateOf(false) }
    var showCareerDialog by remember { mutableStateOf(false) }
    val state = vm.state

    // "Explorar carreras"/"Asignaturas" en Inicio piden abrir el diálogo
    // correspondiente apenas se entra a esta pantalla.
    LaunchedEffect(openSubjectFilterOnEntry, openCareerFilterOnEntry) {
        if (openSubjectFilterOnEntry) {
            showSubjectDialog = true
            onFilterPromptConsumed()
        }
        if (openCareerFilterOnEntry) {
            showCareerDialog = true
            onFilterPromptConsumed()
        }
    }

    if (showSubjectDialog) {
        SubjectFilterDialog(
            subjects = state.subjectOptions,
            selected = state.subjectFilter,
            onSelect = { subject -> vm.setSubjectFilter(subject); showSubjectDialog = false },
            onDismiss = { showSubjectDialog = false }
        )
    }
    if (showCareerDialog) {
        CareerFilterDialog(
            faculties = state.faculties,
            careers = state.careers,
            selected = state.careerFilter,
            onSelect = { career -> vm.setCareerFilter(career); showCareerDialog = false },
            onDismiss = { showCareerDialog = false }
        )
    }

    // Revisión general — antes toda la pantalla (TopBar, buscador, chips Y
    // resultados) era un solo ScreenColumn con verticalScroll, lo que no
    // deja lugar para un LazyColumn real ni para PullToRefreshBox (altura
    // infinita dentro de algo que ya se desplaza). Acá TopBar/buscador/chips
    // quedan fijos arriba y solo la lista de resultados se desplaza.
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 22.dp)
    ) {
        TopBar("Buscar", onBack = onBack, onNotifications = onNotifications, hasUnread = hasUnread)
        SearchBarInput(text = state.query, onTextChange = vm::setQuery, onFilterClick = { showSubjectDialog = true })
        // Módulo 03 — scroll horizontal porque ahora son 5 chips: el nombre de
        // una materia o carrera larga ya alcanzaba a apretar la fila con solo
        // 3, y los nuevos ("4+"/"50+ opiniones"/"Mejor calificados") no deben
        // empujarla fuera de la pantalla en un celular angosto.
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 11.dp)
        ) {
            FilterChip(state.subjectFilter?.name ?: "Materia", state.subjectFilter != null) { showSubjectDialog = true }
            FilterChip(state.careerFilter?.name ?: "Carrera", state.careerFilter != null) { showCareerDialog = true }
            // Filtran en memoria la misma lista ya traída (ver
            // SearchUiState.visibleResults) — promedio y conteo de reseñas
            // reales desde professor_ratings (Módulo 05), ya no un chip
            // decorativo sin acción.
            FilterChip("4+", state.minRatingFilter, icon = Icons.Filled.Star) { vm.toggleMinRatingFilter() }
            FilterChip("50+ opiniones", state.minReviewsFilter) { vm.toggleMinReviewsFilter() }
            // Revisión general — orden en memoria, mismo mecanismo que los
            // chips de arriba.
            FilterChip("Mejor calificados", state.sortByRating, icon = Icons.Filled.Star) { vm.toggleSortByRating() }
        }
        // Revisión general — antes no había forma de volver de un tirón al
        // estado sin filtros salvo apagando cada chip a mano. activeFilterCount
        // ya excluye el texto de búsqueda (ver SearchUiState), así que este
        // botón solo aparece cuando de verdad hay algo que limpiar.
        if (state.activeFilterCount > 0) {
            TextButton(onClick = vm::resetFilters, contentPadding = PaddingValues(0.dp), modifier = Modifier.padding(top = 4.dp)) {
                Text("Limpiar filtros (${state.activeFilterCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(11.dp))
        when (state.screen) {
            SearchScreen.Loading -> Column(Modifier.verticalScroll(rememberScrollState())) { SearchLoadingState() }
            SearchScreen.Error -> Column(Modifier.verticalScroll(rememberScrollState())) { SearchErrorState(state.error, onRetry = vm::retry) }
            SearchScreen.Empty -> Column(Modifier.verticalScroll(rememberScrollState())) { SearchEmptyState() }
            SearchScreen.Results -> {
                val visible = state.visibleResults
                // Revisión general — pull-to-refresh (isRefreshing, no
                // screen == Loading, para no tapar la lista actual con el
                // estado de carga inicial) + LazyColumn en vez de
                // Column+forEach: con muchos profesores en Supabase, antes
                // se componían TODAS las tarjetas de una, visibles o no.
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { vm.refresh() },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                        item {
                            Eyebrow("Explora o empieza a escribir")
                            Text("${visible.size} resultados", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 9.dp, bottom = 5.dp))
                        }
                        if (visible.isEmpty()) {
                            item { GuideCard(R.drawable.cabito_confused, "Ningún profesor cumple estos filtros", "Prueba quitando el chip de \"4+\" o \"50+ opiniones\".") }
                        } else {
                            items(visible, key = { it.id }) { summary ->
                                val preview = summary.toPreview()
                                ProfessorCard(preview, isFavorite(preview.id)) { onProfessor(preview) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentFavorites(favoriteProfessors: List<ProfessorPreview>, onBack: () -> Unit, onNotifications: () -> Unit, hasUnread: Boolean, onProfessor: (ProfessorPreview) -> Unit) {
    ScreenColumn {
        TopBar("Favoritos", onBack = onBack, onNotifications = onNotifications, hasUnread = hasUnread)
        Text("Profesores guardados", fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 7.dp))
        Text("Tu lista personal para volver rápidamente a los perfiles que te interesan.", color = TadeoMuted, fontSize = 12.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(8.dp))
        if (favoriteProfessors.isEmpty()) {
            GuideCard(R.drawable.cabito_confused, "Aún no tienes favoritos", "Usa el corazón en resultados o en el perfil de un profesor.")
        } else favoriteProfessors.forEach { p -> ProfessorCard(p, true) { onProfessor(p) } }
    }
}

@Composable
private fun StudentProfile(vm: ProfileViewModel, onNotifications: () -> Unit, hasUnread: Boolean, onLogout: () -> Unit) {
    val draft = remember { ProfileFormDrafts() }
    val context = LocalContext.current
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bytes = compressAvatarImage(context, uri)
            if (bytes != null) vm.uploadAvatar(bytes) else vm.avatarPrepFailed()
        }
    }
    LaunchedEffect(vm.state.profile) {
        val p = vm.state.profile
        if (p != null && !draft.initialized) {
            draft.fullName = p.fullName
            draft.studentCode = vm.state.studentDetails.student_code.orEmpty()
            draft.semester = vm.state.studentDetails.semester?.toString().orEmpty()
            draft.initialized = true
        }
    }
    LaunchedEffect(vm.state.screen) {
        if (vm.state.screen == ProfileScreen.PasswordUpdated) {
            draft.currentPassword = ""; draft.newPassword = ""; draft.newPasswordConfirm = ""
        }
    }
    when (vm.state.screen) {
        ProfileScreen.Loading -> ProfileLoadingScreen(vm.state.loadingMessage)
        ProfileScreen.Error -> ProfileErrorScreen(vm.state.error ?: ProfileError.LoadFailed) { vm.retryFromError() }
        ProfileScreen.EditPersonal -> PersonalDataScreen(vm, AppRole.STUDENT, draft)
        ProfileScreen.ChangePassword -> SecurityScreen(vm, draft)
        ProfileScreen.PasswordUpdated -> PasswordUpdatedScreen { vm.backToOverview() }
        ProfileScreen.EditSubjects -> EditSubjectsScreen(vm) // no aplica a estudiantes; nunca se dispara desde aquí
        ProfileScreen.EditCareers -> EditCareersScreen(vm)
        ProfileScreen.Privacy -> PrivacyScreen(AppRole.STUDENT) { vm.backToOverview() }
        ProfileScreen.Accessibility -> AccessibilityScreen { vm.backToOverview() }
        ProfileScreen.Help -> HelpScreen(AppRole.STUDENT) { vm.backToOverview() }
        ProfileScreen.Overview -> {
            val p = vm.state.profile
            val semester = vm.state.studentDetails.semester
            var showPhotoViewer by remember { mutableStateOf(false) }
            ScreenColumn {
                HeaderBrand(onNotifications, hasUnread)
                ProfileHeader(
                    initialsOf(p?.fullName ?: "Estudiante"),
                    p?.fullName ?: "Estudiante UTadeo",
                    "Estudiante" + (semester?.let { " · Semestre $it" } ?: ""),
                    p?.email ?: "",
                    avatarUrl = p?.avatarUrl,
                    onEditPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onViewPhoto = if (p?.avatarUrl != null) ({ showPhotoViewer = true }) else null
                )
                SettingsRows(
                    listOf(
                        SettingsRow("Datos personales", "Nombre, código y semestre") { vm.goToEditPersonal() },
                        SettingsRow("Tu carrera", "Carreras que estás cursando") { vm.goToEditCareers() },
                        SettingsRow("Seguridad", "Cambiar tu contraseña") { vm.goToChangePassword() },
                        SettingsRow("Privacidad", "Evaluaciones y datos personales") { vm.goToPrivacy() },
                        SettingsRow("Accesibilidad", "Tamaño del texto") { vm.goToAccessibility() },
                        SettingsRow("Ayuda y soporte", "Preguntas frecuentes y reportes") { vm.goToHelp() }
                    )
                )
                OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD2D2))) {
                    Text("Cerrar sesión", color = Color(0xFFC84242), fontWeight = FontWeight.Bold)
                }
            }
            if (showPhotoViewer && p?.avatarUrl != null) {
                AvatarPhotoViewer(avatarUrl = p.avatarUrl, onDismiss = { showPhotoViewer = false })
            }
        }
    }
}

// Módulo 08 — antes esta pantalla (y Resultados/Mejorar) era contenido de
// muestra fijo ("4.6", "146", "José", categorías inventadas tipo
// "Metodología" que ni siquiera son una de las 6 reales de Rating.kt). Todo
// sale ahora de ProfessorDashboardUiState, construido con las mismas
// evaluaciones que llenan el perfil público del profesor (Módulo 04/05) más
// lo que solo él puede ver (Módulo 09 lo cubre abajo).
@Composable
private fun ProfessorHome(state: ProfessorDashboardUiState, onImprove: () -> Unit, onResults: () -> Unit, onNotifications: () -> Unit, hasUnread: Boolean) {
    val summary = state.summary
    // Revisión general — a pedido de Juan ("el inicio está muy feo"): el
    // encabezado ahora muestra de una vez si el profesor va mejorando o no
    // (mismo delta que la gráfica de Resultados, ver TrendDeltaBadge) y los
    // 3 Kpi de abajo pasan a tener una insignia de color propia por ícono en
    // vez de un glifo suelto flotando junto a la nota, para que se lean como
    // tarjetas de estado y no como texto plano con un ícono de adorno.
    val trend = remember(state.feedback) { monthlyTrend(state.feedback) }
    val homeTrendDelta = if (trend.size >= 2) trend[trend.size - 1].second - trend[trend.size - 2].second else null
    ScreenColumn {
        HeaderBrand(onNotifications, hasUnread)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().glassPanel(RoundedCornerShape(20.dp), tint = Color(0xFFEEF5FF))) {
            Row(Modifier.padding(14.dp, 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Eyebrow("Panel docente")
                        TrendDeltaBadge(homeTrendDelta)
                    }
                    Text("Hola", fontSize = 23.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Resumen de tus evaluaciones académicas.", color = TadeoMuted, fontSize = 11.sp)
                }
                Image(painterResource(R.drawable.cabito_encourage), "Cabito", Modifier.size(64.dp), contentScale = ContentScale.Fit)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Kpi("Valoración", summary.averageOverall?.let { String.format("%.1f", it) } ?: "—", "${summary.reviewCount} evaluaciones", Modifier.weight(1f), icon = Icons.Filled.Star, accent = Color(0xFFFFB400))
            Kpi("Respuestas", summary.reviewCount.toString(), "en total", Modifier.weight(1f), icon = Icons.Filled.Groups, accent = TadeoBlue)
            Kpi("Te recomiendan", summary.recommendPercent?.let { "$it%" } ?: "—", "de tus estudiantes", Modifier.weight(1f), icon = Icons.Filled.Favorite, accent = Color(0xFFE0245E))
        }
        // Revisión general — a pedido de Juan: Inicio mostraba la MISMA
        // gráfica mensual y la MISMA grilla de categorías que ya están en
        // "Mis resultados" (ProfessorResults, un tab más allá), así que
        // Inicio terminaba siendo un resumen redundante en vez de un punto
        // de partida propio. Ahora Inicio se queda solo con lo que tiene
        // sentido como vista rápida y no se repite en ningún otro lado: los
        // 3 KPIs de arriba, lo que destacan tus estudiantes (ver abajo),
        // tu fortaleza/oportunidad del periodo y tu objetivo activo — y un
        // solo botón al final que lleva al detalle completo (gráfica mes a
        // mes + categoría por categoría) en Resultados, en vez de repetirlo
        // acá.
        //
        // "Lo que destacan" ya existía como dato (recommend_for, Módulo 05)
        // y ya se mostraba al perfil PÚBLICO que ven otros estudiantes (ver
        // ProfessorOverviewContent) — pero el propio profesor nunca podía
        // verlo. Mismo cálculo (las 3 razones más marcadas) y mismo umbral
        // que el resto de insights para que el agregado ya sea representativo.
        val highlights = if (state.enoughForInsights) {
            summary.recommendForCounts.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { RecommendFor.labels[it.key] ?: it.key }
        } else emptyList()
        if (highlights.isNotEmpty()) {
            // Revisión general — a pedido de Juan ("se sigue viendo fea"): un
            // Row normal no reparte en varias líneas, así que cuando las 3
            // etiquetas no cabían en el ancho de la pantalla, Compose las
            // seguía poniendo en una sola fila y el texto de la última
            // terminaba apretado/partido en varias líneas DENTRO de su propia
            // pastilla. FlowRow sí reparte las pastillas en tantas líneas
            // como hagan falta, cada una con su ancho natural.
            Text("Lo que destacan tus estudiantes", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                highlights.forEach { Tag(it) }
            }
        }
        when {
            !state.enoughForInsights -> InsightCard(
                Color(0xFFFFF7DB), "Todavía reuniendo datos", "Pocas evaluaciones por ahora",
                "Cuando tengas al menos $MIN_REVIEWS_TO_SHOW_COMMENTS evaluaciones vas a ver aquí tu punto fuerte, tu oportunidad de mejora y lo que más destacan tus estudiantes."
            )
            else -> {
                state.strongestCategory?.let { strong ->
                    val avg = summary.categoryAverages.getValue(strong).average
                    InsightCard(Color(0xFFE4F8F5), "Punto fuerte", strong.shortLabel, String.format("Tus estudiantes la calificaron en %.1f / 5, la mejor de este periodo.", avg))
                }
                state.weakestCategory?.takeIf { it != state.strongestCategory }?.let { weak ->
                    InsightCard(Color(0xFFFFF7DB), "Punto por fortalecer", weak.shortLabel, ProfessorTips.summaryByCategory.getValue(weak), "Ver recomendaciones →", onImprove)
                }
            }
        }
        // A pedido de Juan, "Mejorar" ya no tiene el plan fijo con
        // "Marcar como objetivo" (las recomendaciones con IA lo reemplazan),
        // así que la tarjeta "Mi objetivo activo" que había aquí se quitó:
        // llevaba a una sección que ya no existe. Las metas que ya estaban
        // guardadas en Supabase no se borran, solo dejan de mostrarse.
        CardSurface {
            Eyebrow("Detalle completo")
            Text("Gráfica mes a mes y tu nota en cada categoría", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            Text("Revisa la evolución y el detalle por categoría en Resultados.", color = TadeoMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
            TextButton(onClick = onResults, contentPadding = PaddingValues(0.dp), modifier = Modifier.padding(top = 4.dp)) { Text("Ver resultados completos →", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ProfessorResults(
    state: ProfessorDashboardUiState,
    onBack: () -> Unit,
    onNotifications: () -> Unit,
    hasUnread: Boolean,
    onOpenReportDialog: (String) -> Unit,
    onCloseReportDialog: () -> Unit,
    onSubmitReport: (String, ReportReason) -> Unit
) {
    val summary = state.summary
    // Rediseño a pedido de Juan ("este dashboard tampoco me gusta... esas
    // gráficas tan feas y esa desorganización"): el MiniChart original
    // escalaba cada barra sobre 0-5 fijo, así que dos meses con promedios
    // parecidos (p.ej. 4.2 y 4.3) se veían como barras casi idénticas, sin
    // número ni contexto — exactamente lo que se ve en su captura. Ahora
    // hay una tarjeta "hero" con el número grande + el delta vs. el mes
    // anterior, y TrendChart (ver componentes compartidos) usa una escala
    // dinámica sobre el rango real de los datos para que SÍ se note la
    // tendencia, con el valor exacto rotulado sobre cada barra.
    val trend = monthlyTrend(state.feedback)
    val trendDelta = if (trend.size >= 2) trend[trend.size - 1].second - trend[trend.size - 2].second else null
    ScreenColumn {
        TopBar("Mis resultados", onBack, onNotifications, hasUnread)
        HeroScoreCard(summary, trendDelta)
        Text("Evolución mensual", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        TrendChart(trend)
        Text("Por categoría", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        RatingCategory.entries.forEach { category ->
            CategoryScoreCard(category, summary.categoryAverages[category], summary.reviewCount)
        }
        Text("Lo que te escribieron", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 2.dp))
        Text("Solo tú puedes ver esto — nunca se muestra a otros estudiantes.", color = TadeoMuted, fontSize = 10.5.sp, lineHeight = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
        // Módulo 11 — una reseña con veredicto bajado=true ya no se
        // muestra: reutiliza professor_ratings.reported, que
        // get_professor_feedback() ya filtra, así que al recargar el panel
        // tampoco volvería a aparecer — esto solo evita el salto visual de
        // esperar a esa recarga.
        val visibleFeedback = state.feedback.filter { state.reportVerdicts[it.ratingId]?.bajado != true }
        if (visibleFeedback.isEmpty()) {
            GuideCard(R.drawable.cabito_confused, "Todavía no hay evaluaciones", "Cuando tus estudiantes te evalúen, vas a ver aquí lo que escribieron.")
        } else {
            visibleFeedback.forEach { note ->
                ProfessorFeedbackCard(
                    note = note,
                    verdict = state.reportVerdicts[note.ratingId],
                    onReport = { onOpenReportDialog(note.ratingId) }
                )
            }
        }
    }

    val reportDialogFor = state.reportDialogFor
    if (reportDialogFor != null) {
        ReportReasonDialog(
            submitting = state.reportSubmitting,
            error = state.reportError,
            onConfirm = { reason -> onSubmitReport(reportDialogFor, reason) },
            onDismiss = onCloseReportDialog
        )
    }
}

@Composable
private fun ProfessorImprove(
    state: ProfessorDashboardUiState,
    onBack: () -> Unit,
    onEnter: () -> Unit,
    onGenerateAi: () -> Unit,
    onAcceptPlan: () -> Unit,
    onDiscardDraft: () -> Unit,
    onOpenUpdate: () -> Unit,
    onCloseUpdate: () -> Unit,
    onUpdateWithReason: (String) -> Unit,
    onToggleApplied: (Int) -> Unit,
    onCloseCut: () -> Unit
) {
    // A pedido de Juan: el plan se propone solo apenas el profesor entra a
    // "Mejorar", sin tener que tocar un botón. ensureAiRecommendations() no
    // genera nada si ya hay un plan aceptado, ni si ya hay una propuesta en
    // esta sesión (ni si ya se intentó y falló: ahí usa "Reintentar").
    LaunchedEffect(Unit) { onEnter() }
    ScreenColumn {
        TopBar("Plan de mejora", onBack)
        GuideCard(R.drawable.cabito_encourage, "Convierte resultados en acciones.", "Un plan a partir de lo que tus estudiantes dijeron. Acéptalo y en el próximo corte vemos si mejoraste.")
        // Módulo 12 — el plan se PROPONE, el profesor lo ACEPTA y queda como su
        // objetivo del corte (con las evaluaciones nuevas se compara si de
        // verdad mejoró). Al actualizarlo debe decir por qué. Todo plan
        // aceptado, cerrado o reemplazado queda en el historial.
        AiRecommendationsPanel(
            state = state,
            onRetry = onGenerateAi,
            onAccept = onAcceptPlan,
            onDiscard = onDiscardDraft,
            onOpenUpdate = onOpenUpdate,
            onToggleApplied = onToggleApplied,
            onCloseCut = onCloseCut
        )
        PlanHistoryCard(state.planHistory)
    }
    if (state.updateDialogOpen) {
        PlanUpdateDialog(onConfirm = onUpdateWithReason, onDismiss = onCloseUpdate)
    }
}

// Lo que un estudiante escribió, completo — ESTA es la única pantalla de
// toda la app que llega a mostrar category_notes/recommend_reason (ver el
// comentario de ProfessorFeedbackNote en Rating.kt). El estudiante nunca se
// identifica más allá de lo que ya se podía ver en ReviewsScreen.
@Composable
private fun ProfessorFeedbackCard(note: ProfessorFeedbackNote, verdict: AiReportVerdict?, onReport: () -> Unit) {
    // Módulo 11 — verdict solo llega no-nulo acá cuando ya se reportó Y la
    // IA dijo que el reporte no era válido (bajado=true se filtra antes de
    // llegar a esta tarjeta — ver visibleFeedback en ProfessorResults). Un
    // reporte ya resuelto no se puede volver a reportar: una sola decisión
    // de la IA por reseña, no un reintento hasta que "funcione".
    val alreadyReported = verdict != null
    CardSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFB400), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(String.format("%.1f", note.overall), fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(9.dp))
            Icon(
                if (note.wouldRecommend) Icons.Filled.Favorite else Icons.Filled.Close,
                contentDescription = null,
                tint = if (note.wouldRecommend) Color(0xFF2FBE79) else Color(0xFFBD4136),
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(if (note.wouldRecommend) "Lo recomienda" else "No lo recomienda", color = TadeoMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            // Módulo 11 — ya no es un umbral de 3 reportes: la IA decide de
            // inmediato con el motivo que elijas (ver ReportReasonDialog).
            TextButton(onClick = onReport, enabled = !alreadyReported, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                Text(
                    if (alreadyReported) "Reporte no válido" else "Reportar",
                    color = if (alreadyReported) TadeoMuted else Color(0xFFBD4136),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        // Módulo 12 — contexto para leer el comentario: cuánta clase vio quien
        // lo escribió (la reseña ya pesa en el promedio según esto).
        note.attendanceBand?.let { band ->
            AttendanceBands.labels[band]?.let { label ->
                Text("Asistencia declarada: $label", color = TadeoMuted, fontSize = 10.5.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
        if (verdict != null) {
            Surface(color = Color(0xFFFFF7DB), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(10.dp)) {
                    Text("Revisamos tu reporte", color = Color(0xFF8A6D00), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(verdict.justificacion, color = TadeoInk, fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        Text(note.tipComment, color = TadeoInk, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 7.dp))
        Text(note.hardestPartComment, color = TadeoMuted, fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 5.dp))
        Text(if (note.wouldRecommend) "Por qué sí te recomienda:" else "Por qué no te recomienda:", color = TadeoMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Text(note.recommendReason, color = TadeoInk, fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
        note.criticalNotes.forEach { (category, text) ->
            Surface(color = Color(0xFFFFF0F0), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(10.dp)) {
                    Text(category.shortLabel, color = Color(0xFFBD4136), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(text, color = TadeoInk, fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        // Revisión general — notas de tarjetas BUENAS (opcionales): caja
        // verde distinta de la roja de arriba, para que un elogio no se vea
        // como una alerta.
        note.positiveNotes.forEach { (category, text) ->
            Surface(color = Color(0xFFEAF7F0), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(10.dp)) {
                    Text(category.shortLabel, color = Color(0xFF1F8F5A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(text, color = TadeoInk, fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}

// Módulo 11 — diálogo "como el de Meta cuando uno reporta una cuenta"
// (pedido explícito de Juan): el profesor elige UN motivo de una lista
// fija (ReportReason) y confirma; mientras submitting es true no se puede
// ni elegir otro motivo ni cerrar el diálogo (evita mandar dos reportes de
// la misma reseña mientras el primero sigue esperando a Gemini). error
// viene de ProfessorDashboardViewModel.submitReport() cuando el intento
// falló por una razón técnica (nunca por un veredicto real de la IA).
@Composable
private fun ReportReasonDialog(submitting: Boolean, error: String?, onConfirm: (ReportReason) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf<ReportReason?>(null) }
    Dialog(onDismissRequest = { if (!submitting) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth(0.92f).glassPanel(RoundedCornerShape(26.dp))) {
            Column(Modifier.padding(18.dp)) {
                Text("Reportar esta reseña", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Elige el motivo que mejor describe el problema. Revisaremos el texto de la reseña y te daremos una respuesta de inmediato.",
                    color = TadeoMuted, fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )
                ReportReason.entries.forEach { reason ->
                    FilterRadioRow(reason.label, selected == reason) { if (!submitting) selected = reason }
                }
                if (error != null) {
                    ProfileInlineError(error)
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (submitting) {
                        GlassSpinner(size = 24.dp, strokeWidth = 3.dp)
                        Spacer(Modifier.width(10.dp))
                    }
                    TextButton(onClick = onDismiss, enabled = !submitting) { Text("Cancelar", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { selected?.let(onConfirm) }, enabled = !submitting && selected != null) {
                        Text("Reportar", color = Color(0xFFBD4136), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Agrupa el feedback del profesor por mes (según created_at, "yyyy-MM-...")
// y promedia overallWeight de cada evaluación dentro de ese mes — hasta los
// últimos 6 meses con al menos una evaluación. Alimenta MiniChart con una
// tendencia real en vez de los 6 valores de muestra que traía antes.
private val MONTH_LABELS = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

private fun monthlyTrend(feedback: List<ProfessorFeedbackNote>): List<Pair<String, Float>> =
    feedback
        .mapNotNull { note -> note.createdAt?.take(7)?.let { it to note.overall } }
        .groupBy({ it.first }, { it.second })
        .toSortedMap()
        .entries
        .toList()
        .takeLast(6)
        .map { (key, values) ->
            val label = key.substringAfter("-").toIntOrNull()?.let { MONTH_LABELS.getOrNull(it - 1) } ?: key
            label to values.average().toFloat()
        }

@Composable
private fun ProfessorProfile(vm: ProfileViewModel, onNotifications: () -> Unit, hasUnread: Boolean, onLogout: () -> Unit) {
    val draft = remember { ProfileFormDrafts() }
    val context = LocalContext.current
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bytes = compressAvatarImage(context, uri)
            if (bytes != null) vm.uploadAvatar(bytes) else vm.avatarPrepFailed()
        }
    }
    LaunchedEffect(vm.state.profile) {
        val p = vm.state.profile
        if (p != null && !draft.initialized) {
            draft.fullName = p.fullName
            draft.professionalTitle = vm.state.professorDetails.professional_title.orEmpty()
            draft.bio = vm.state.professorDetails.bio.orEmpty()
            draft.initialized = true
        }
    }
    LaunchedEffect(vm.state.screen) {
        if (vm.state.screen == ProfileScreen.PasswordUpdated) {
            draft.currentPassword = ""; draft.newPassword = ""; draft.newPasswordConfirm = ""
        }
    }
    when (vm.state.screen) {
        ProfileScreen.Loading -> ProfileLoadingScreen(vm.state.loadingMessage)
        ProfileScreen.Error -> ProfileErrorScreen(vm.state.error ?: ProfileError.LoadFailed) { vm.retryFromError() }
        ProfileScreen.EditPersonal -> PersonalDataScreen(vm, AppRole.PROFESSOR, draft)
        ProfileScreen.ChangePassword -> SecurityScreen(vm, draft)
        ProfileScreen.PasswordUpdated -> PasswordUpdatedScreen { vm.backToOverview() }
        ProfileScreen.EditSubjects -> EditSubjectsScreen(vm)
        ProfileScreen.EditCareers -> EditCareersScreen(vm) // no aplica a profesores; nunca se dispara desde aquí
        ProfileScreen.Privacy -> PrivacyScreen(AppRole.PROFESSOR) { vm.backToOverview() }
        ProfileScreen.Accessibility -> AccessibilityScreen { vm.backToOverview() }
        ProfileScreen.Help -> HelpScreen(AppRole.PROFESSOR) { vm.backToOverview() }
        ProfileScreen.Overview -> {
            val p = vm.state.profile
            val title = vm.state.professorDetails.professional_title
            var showPhotoViewer by remember { mutableStateOf(false) }
            ScreenColumn {
                HeaderBrand(onNotifications, hasUnread)
                ProfileHeader(
                    initialsOf(p?.fullName ?: "Profesor"),
                    p?.fullName ?: "Profesor UTadeo",
                    title?.takeIf { it.isNotBlank() }?.let { "Profesor · $it" } ?: "Profesor UTadeo",
                    p?.email ?: "",
                    avatarUrl = p?.avatarUrl,
                    onEditPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onViewPhoto = if (p?.avatarUrl != null) ({ showPhotoViewer = true }) else null
                )
                SettingsRows(
                    listOf(
                        SettingsRow("Perfil docente", "Nombre, título profesional y biografía") { vm.goToEditPersonal() },
                        SettingsRow("Materias que dictas", "Edita las asignaturas de este periodo") { vm.goToEditSubjects() },
                        SettingsRow("Seguridad", "Cambiar tu contraseña") { vm.goToChangePassword() },
                        SettingsRow("Privacidad y datos", "Anonimato y reportes de evaluaciones") { vm.goToPrivacy() },
                        SettingsRow("Accesibilidad", "Tamaño del texto") { vm.goToAccessibility() },
                        SettingsRow("Ayuda y soporte", "Preguntas frecuentes y reportes") { vm.goToHelp() }
                    )
                )
                OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD2D2))) { Text("Cerrar sesión", color = Color(0xFFC84242), fontWeight = FontWeight.Bold) }
            }
            if (showPhotoViewer && p?.avatarUrl != null) {
                AvatarPhotoViewer(avatarUrl = p.avatarUrl, onDismiss = { showPhotoViewer = false })
            }
        }
    }
}

// Módulo 07 — antes esta pantalla mostraba dos tarjetas de muestra fijas,
// sin relación con nada que pasara en la cuenta. Ahora pide la lista real al
// entrar (vm.open(), que de paso marca todo como leído) y cubre
// carga/éxito/error/reintento igual que el resto de pantallas del proyecto.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsScreen(vm: NotificationsViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) { vm.open() }
    val state = vm.state
    // Revisión general — antes esta pantalla usaba ScreenColumn (todo el
    // contenido metido en un solo verticalScroll), lo que no deja lugar
    // para un LazyColumn de verdad (altura infinita dentro de algo que ya
    // se desplaza) ni para PullToRefreshBox. Acá el TopBar queda fijo y
    // cada rama del estado maneja su propio scroll.
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 22.dp)
    ) {
        TopBar("Notificaciones", onBack, null)
        when (state.screen) {
            NotificationsScreenState.Loading -> Column(Modifier.fillMaxWidth().padding(top = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                GlassSpinner()
                Text("Cargando tus notificaciones…", color = TadeoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            }
            NotificationsScreenState.Error -> Column(Modifier.verticalScroll(rememberScrollState())) {
                val error = state.error ?: NotificationsError.LoadFailed
                GuideCard(R.drawable.cabito_confused, error.title, error.message)
                Text(error.recovery, color = TadeoMuted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 10.dp))
                OutlinedButton(onClick = { vm.retryFromError() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("Reintentar", fontWeight = FontWeight.Bold)
                }
            }
            NotificationsScreenState.Content -> {
                // Revisión general — antes no había forma de refrescar sin
                // salir y volver a entrar a esta pantalla. isRefreshing (no
                // screen == Loading) deja la lista actual visible debajo del
                // indicador mientras se vuelve a consultar. La lista ya no
                // es un Column+forEach dentro de un scroll sin límite: con
                // potencialmente muchas notificaciones a lo largo del
                // tiempo, LazyColumn solo compone lo que está en pantalla.
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { vm.refresh() },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    if (state.notifications.isEmpty()) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            GuideCard(R.drawable.cabito_point, "Todo al día.", "Aquí encontrarás novedades importantes.")
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                            items(state.notifications, key = { it.id }) { notification ->
                                NotificationCard(notification.title, notification.body, relativeTime(notification.createdAt))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Mismo criterio de "cercano en el tiempo, sin hora exacta" que el resto de
// la app usa para fechas (p.ej. MONTH_LABELS de monthlyTrend): hoy/ayer/hace
// N días, y a partir de una semana la fecha corta. minSdk 26 permite usar
// java.time.* directamente, sin necesitar desugaring.
private fun relativeTime(iso: String): String {
    // Antes devolvía "" ante un ISO raro/no parseable, dejando un hueco en
    // blanco donde debería ir una fecha — mejor un texto explícito.
    val createdAt = runCatching { java.time.Instant.parse(iso) }.getOrNull() ?: return "Fecha desconocida"
    val createdDate = createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val today = java.time.LocalDate.now()
    val days = java.time.temporal.ChronoUnit.DAYS.between(createdDate, today)
    return when {
        days <= 0L -> "Hoy"
        days == 1L -> "Ayer"
        days < 7L -> "Hace $days días"
        else -> createdDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale("es", "CO")))
    }
}

// Módulo 04 + 05 — el perfil público ahora despacha según el estado real de
// RatingViewModel (Overview/Form/Reviews/Loading/Error), en vez de mostrar
// siempre la misma tarjeta con números fijos.
@Composable
private fun ProfessorDetailScreen(professor: ProfessorPreview, favorite: Boolean, onToggleFavorite: () -> Unit, ratingVm: RatingViewModel, onBack: () -> Unit) {
    val state = ratingVm.state
    when (state.screen) {
        RatingScreen.Loading -> RatingLoadingState(onBack)
        RatingScreen.Error -> RatingErrorState(state.error ?: RatingError.LoadFailed, onBack) { ratingVm.retryFromError() }
        RatingScreen.Overview -> ProfessorOverviewContent(
            professor = professor,
            favorite = favorite,
            onToggleFavorite = onToggleFavorite,
            state = state,
            onBack = onBack,
            onEvaluate = { ratingVm.goToForm() },
            onSeeReviews = { ratingVm.goToReviews() }
        )
        RatingScreen.Form -> RatingFormScreen(professor = professor, state = state, vm = ratingVm, onBack = { ratingVm.backToOverview() })
        RatingScreen.Reviews -> ReviewsScreen(professor = professor, state = state, vm = ratingVm, onBack = { ratingVm.backToOverview() })
    }
}

@Composable
private fun RatingLoadingState(onBack: () -> Unit) {
    ScreenColumn {
        TopBar("Perfil del profesor", onBack, null)
        Column(Modifier.fillMaxWidth().padding(top = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GlassSpinner()
            Text("Cargando evaluaciones…", color = TadeoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun RatingErrorState(error: RatingError, onBack: () -> Unit, onRetry: () -> Unit) {
    ScreenColumn {
        TopBar("Perfil del profesor", onBack, null)
        GuideCard(R.drawable.cabito_confused, error.title, error.message)
        Text(error.recovery, color = TadeoMuted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 10.dp))
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text("Reintentar", fontWeight = FontWeight.Bold)
        }
    }
}

// Lo que antes era el cuerpo fijo de ProfessorDetailScreen, ahora alimentado
// con el resumen real de professor_ratings (state.summary) en vez de
// ScoreRow(..., 4.8f) idénticos para todos los profesores.
@Composable
private fun ProfessorOverviewContent(
    professor: ProfessorPreview,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    state: RatingUiState,
    onBack: () -> Unit,
    onEvaluate: () -> Unit,
    onSeeReviews: () -> Unit
) {
    val summary = state.summary
    ScreenColumn {
        TopBar("Perfil del profesor", onBack, null)
        Box(Modifier.fillMaxWidth().padding(top = 9.dp).glassPanel(RoundedCornerShape(22.dp))) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(professor.initials, 76)
                Text(professor.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                Text(professor.subjectCareerLabel(), color = TadeoMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                if (summary.reviewCount == 0) {
                    Text("Sin evaluaciones todavía", color = TadeoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
                } else {
                    // El dato "titular" ya no es un promedio de estrellas (eso es
                    // justo lo que termina midiendo "qué tan fácil es sacar buena
                    // nota" en vez de "qué tan buen profesor es") — es el % que
                    // dice que sí lo recomendaría, que resiste mucho mejor ese sesgo.
                    Text("${summary.recommendPercent ?: 0}%", color = TadeoBlue, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, modifier = Modifier.padding(top = 5.dp))
                    Text("de los estudiantes lo recomienda", color = TadeoMuted, fontSize = 11.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFB400), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${String.format("%.1f", summary.averageOverall ?: 0.0)} · ${summary.reviewCount} evaluaciones", color = TadeoMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(onClick = onToggleFavorite, modifier = Modifier.padding(top = 10.dp), shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD1D2))) {
                    Icon(
                        if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (favorite) Color(0xFFFF686B) else Color(0xFF526078),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(if (favorite) "En favoritos" else "Añadir a favoritos", color = if (favorite) Color(0xFFFF686B) else Color(0xFF526078), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
        GuideCard(R.drawable.cabito_point, "Información para decidir con contexto.", "Revisa fortalezas, resultados y comentarios.")
        // Revisión general — esta sección nunca se mostraba: professor.tags
        // estaba fijo en emptyList() (ver toPreview()), así que "Lo que
        // destacan" quedaba siempre oculto. Las razones de recomendación
        // (recommend_for, Módulo 05) son justo lo que esta tarjeta debería
        // mostrar — no las materias, que van en ProfessorCard. Mismo umbral
        // que el resto de la app para que el agregado ya sea representativo.
        val highlights = if (summary.reviewCount >= MIN_REVIEWS_TO_SHOW_COMMENTS) {
            summary.recommendForCounts.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { RecommendFor.labels[it.key] ?: it.key }
        } else emptyList()
        if (highlights.isNotEmpty()) {
            Text("Lo que destacan", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { highlights.forEach { Tag(it) } }
        }
        Text("Resultados", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 3.dp))
        if (summary.reviewCount == 0) {
            GuideCard(R.drawable.cabito_confused, "Todavía no hay calificaciones", "Sé el primero en evaluar a este profesor para que otros estudiantes vean resultados reales.")
        } else {
            RatingCategory.entries.forEach { category ->
                ScoreRow(category.shortLabel, (summary.categoryAverages[category]?.average ?: 0.0).toFloat())
            }
            Spacer(Modifier.height(10.dp))
            FactsSummaryCard(summary)
        }
        CardSurface { Text("Información académica", fontWeight = FontWeight.Bold); Text(professor.bio, color = TadeoMuted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 5.dp)) }
        Button(onClick = onEvaluate, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = TadeoBlue)) {
            Text(if (state.myRating != null) "Editar tu evaluación" else "Evaluar profesor", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(onClick = onSeeReviews, modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 8.dp), shape = RoundedCornerShape(14.dp)) {
            Text("Ver evaluaciones (${summary.reviewCount})", fontWeight = FontWeight.Bold)
        }
    }
}

// ---- Módulo 05 v2 — evaluación por tarjetas (no por estrellas) ----
//
// El estudiante nunca toca un número: para cada pregunta elige la tarjeta
// que mejor describe lo que vivió. AnswerCard es la pieza que se reutiliza
// en TODOS los tipos de pregunta de este formulario (las 6 categorías, el
// "sí/no lo recomiendo", "para quién lo recomendarías" y los datos rápidos)
// — la única diferencia entre selección única y múltiple es qué hace el
// `onClick` que le pasa el que la llama.

@Composable
private fun AnswerCard(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.fillMaxWidth().padding(bottom = 7.dp)
            .glassPanel(shape, tint = if (selected) Color(0xFFE7F0FF) else Color.White)
            .then(if (selected) Modifier.border(1.5.dp, TadeoBlue, shape) else Modifier)
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(20.dp).clip(CircleShape)
                    .background(if (selected) TadeoBlue else Color.Transparent)
                    .border(1.5.dp, if (selected) TadeoBlue else Color(0xFFC7CFDA), CircleShape),
                contentAlignment = Alignment.Center
            ) { if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp)) }
            Spacer(Modifier.width(10.dp))
            Text(text, fontSize = 12.5.sp, lineHeight = 17.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
        }
    }
}

// Campo de texto obligatorio con mínimo (y tope) de PALABRAS. El contador es
// "estilo X": en vez de un "25/25" seco, hay un anillo que se llena hacia el
// tope y un mensaje que cambia según el estado —
//   · rojo   → todavía no llegas al mínimo ("Escribe mínimo N palabras…").
//   · verde  → ya cumples el mínimo; "puedes escribir hasta M palabras".
//   · ámbar  → te estás acercando al tope ("te quedan K palabras").
//   · rojo   → llegaste al tope; no se acepta más texto.
// El tope se aplica al escribir/pegar: si un cambio lo pasaría, se ignora.
// Cada campo pide un mínimo distinto (minWords) — ver MIN_COMMENT_WORDS /
// MIN_RECOMMEND_REASON_WORDS / MIN_TIP_WORDS en RatingViewModel. maxWords
// por defecto es 4 veces el mínimo (un comentario largo pero no un ensayo).
@Composable
private fun RequiredMultilineField(
    value: String,
    onValue: (String) -> Unit,
    hint: String,
    showValidation: Boolean,
    minWords: Int = MIN_COMMENT_WORDS,
    maxWords: Int = minWords * 4
) {
    val trimmedValue = value.trim()
    val wordCount = TextQuality.wordCount(trimmedValue)
    val tooShort = wordCount < minWords
    val atLimit = wordCount >= maxWords
    val nearLimit = !atLimit && wordCount >= (maxWords * 0.85).toInt()
    // Solo se evalúa cuando YA se pasó el mínimo, para no mostrar dos avisos
    // a la vez mientras la persona todavía está escribiendo.
    val notMeaningful = !tooShort && !TextQuality.isMeaningful(trimmedValue)
    val invalid = tooShort || notMeaningful
    val showHint = showValidation || value.isNotEmpty()
    val red = Color(0xFFE05B4E)
    val green = Color(0xFF2FBE79)
    val amber = Color(0xFFE8A317)
    val statusColor = when {
        atLimit -> red
        tooShort -> red
        nearLimit -> amber
        else -> green
    }
    val statusText = when {
        atLimit -> "Llegaste al límite de $maxWords palabras."
        tooShort && wordCount == 0 -> "Siempre debes escribir mínimo $minWords palabras."
        tooShort -> "Mínimo $minWords palabras · te faltan ${minWords - wordCount}."
        nearLimit -> "Te quedan ${maxWords - wordCount} palabras."
        else -> "Listo, ya cumples el mínimo. Puedes escribir hasta $maxWords palabras."
    }
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).glassPill(shape)
                .then(if (showHint && invalid) Modifier.border(1.5.dp, red, shape) else Modifier)
                .padding(horizontal = 13.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = { new ->
                    // Se acepta el cambio si no pasa el tope, o si el texto
                    // se está acortando (borrar siempre debe poder).
                    if (TextQuality.wordCount(new) <= maxWords || new.length < value.length) onValue(new)
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TadeoInk, fontSize = 12.5.sp),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner -> if (value.isEmpty()) Text(hint, color = TadeoMuted, fontSize = 12.5.sp); inner() }
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                statusText,
                color = statusColor,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            WordCountRing(count = wordCount, minWords = minWords, maxWords = maxWords, color = statusColor)
        }
        if (showHint && notMeaningful) ProfileInlineError("Esto no parece un comentario real — cuéntanos con tus palabras qué pasó.")
    }
}

// Anillo de progreso del contador de palabras: se llena de 0 al tope; una
// marquita sobre el aro señala dónde queda el mínimo, y dentro del anillo se
// muestra cuántas palabras faltan para el mínimo (o cuántas quedan del tope).
@Composable
private fun WordCountRing(count: Int, minWords: Int, maxWords: Int, color: Color) {
    val progress = (count.toFloat() / maxWords).coerceIn(0f, 1f)
    val minFraction = (minWords.toFloat() / maxWords).coerceIn(0f, 1f)
    val track = Color(0xFFD9DEE7)
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * progress, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            // Marca del mínimo.
            val angle = Math.toRadians((-90.0 + 360.0 * minFraction))
            val r = size.width / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f
            val inner = r - stroke - 1.dp.toPx()
            val outer = r + 0.5.dp.toPx()
            drawLine(
                Color(0xFF5B6575),
                Offset(cx + (inner * Math.cos(angle)).toFloat(), cy + (inner * Math.sin(angle)).toFloat()),
                Offset(cx + (outer * Math.cos(angle)).toFloat(), cy + (outer * Math.sin(angle)).toFloat()),
                strokeWidth = 1.5.dp.toPx()
            )
        }
        val label = if (count < minWords) (minWords - count).toString() else if (count >= maxWords) "0" else (maxWords - count).toString()
        Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

// Revisión general — mismo campo que RequiredMultilineField pero SIN mínimo
// de caracteres ni chequeo de contenido: se usa en las dos tarjetas BUENAS
// de cada categoría (ver CardQuestion), que ahora también abren una caja de
// texto pero opcional. No tiene sentido exigirle "que suene real" a un
// campo que de por sí puede quedar vacío — nadie está obligado a escribir
// nada para poder continuar.
@Composable
private fun OptionalMultilineField(value: String, onValue: (String) -> Unit, hint: String) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).glassPill(shape)
            .padding(horizontal = 13.dp, vertical = 10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValue,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TadeoInk, fontSize = 12.5.sp),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner -> if (value.isEmpty()) Text(hint, color = TadeoMuted, fontSize = 12.5.sp); inner() }
        )
    }
}

// Una de las 6 preguntas de tarjeta: el enunciado, las 4 tarjetas de
// RatingCards, y — solo si se eligió una de las dos más críticas — el campo
// obligatorio para contar qué pasó.
@Composable
private fun CardQuestion(
    category: RatingCategory,
    selectedIndex: Int,
    note: String,
    showValidation: Boolean,
    onSelect: (Int) -> Unit,
    onNoteChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Text(category.question, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        RatingCards.byCategory.getValue(category).forEach { card ->
            AnswerCard(card.text, selected = card.index == selectedIndex) { onSelect(card.index) }
        }
        if (showValidation && selectedIndex < 0) ProfileInlineError("Elige la tarjeta que más se parezca a tu experiencia.")
        // Revisión general — a pedido de Juan: ahora las CUATRO tarjetas
        // abren una caja de texto, no solo las dos críticas. La diferencia
        // es si es obligatoria (las dos malas, con mínimo de caracteres y
        // chequeo de contenido) u opcional (las dos buenas, libre).
        if (selectedIndex >= 0) {
            val card = RatingCards.byCategory.getValue(category)[selectedIndex]
            if (card.requiresNote) {
                RequiredMultilineField(note, onNoteChange, RatingCards.noteHint.getValue(category), showValidation)
            } else {
                OptionalMultilineField(note, onNoteChange, RatingCards.goodNoteHint.getValue(category))
            }
        }
    }
}

// Pregunta de dato rápido con una sola respuesta posible (carga de trabajo,
// de qué depende la nota, estilo de clase).
@Composable
private fun FactPicker(question: String, options: Map<String, String>, selected: String?, showValidation: Boolean, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        FieldLabel(question)
        Spacer(Modifier.height(6.dp))
        options.forEach { (key, label) -> AnswerCard(label, selected == key) { onSelect(key) } }
        if (showValidation && selected == null) ProfileInlineError("Selecciona una opción.")
    }
}

// Mismo FactPicker pero para los datos rápidos que son sí/no.
@Composable
private fun BoolFactPicker(question: String, selected: Boolean?, showValidation: Boolean, onSelect: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        FieldLabel(question)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AnswerCard("Sí", selected == true, Modifier.weight(1f)) { onSelect(true) }
            AnswerCard("No", selected == false, Modifier.weight(1f)) { onSelect(false) }
        }
        if (showValidation && selected == null) ProfileInlineError("Selecciona una opción.")
    }
}

// Resumen de los datos rápidos en el perfil del profesor: % de reseñas que
// marcaron cada hecho, y la opción más marcada para las de selección única.
@Composable
private fun FactsSummaryCard(summary: ProfessorRatingSummary) {
    val n = summary.reviewCount
    fun pct(count: Int) = if (n == 0) 0 else (count * 100 / n)
    CardSurface {
        Text("Datos rápidos del curso", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        FactLine("Asistencia obligatoria", "${pct(summary.attendanceMandatoryCount)}% de las reseñas")
        FactLine("Permite revisar parciales", "${pct(summary.allowsReviewOfExamsCount)}% de las reseñas")
        summary.workloadCounts.maxByOrNull { it.value }?.key?.let { FactLine("Carga de trabajo", RatingFacts.workloadLabels[it].orEmpty()) }
        summary.gradingBasisCounts.maxByOrNull { it.value }?.key?.let { FactLine("La nota depende más de", RatingFacts.gradingBasisLabels[it].orEmpty()) }
        summary.classStyleCounts.maxByOrNull { it.value }?.key?.let { FactLine("Estilo de clase", RatingFacts.classStyleLabels[it].orEmpty()) }
    }
}

@Composable private fun FactLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TadeoMuted, fontSize = 11.5.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
    }
}

// Módulo 05 v2 — una "pregunta por pantalla" estilo Duolingo, en vez de un
// formulario largo con scroll. Cada paso es UNA tarjeta de intro de sección
// o UNA pregunta; arriba va la barra de progreso (como las lecciones de
// Duolingo) y abajo el botón "Continuar", deshabilitado hasta que ese paso
// específico esté completo — así nunca se avanza con algo a medias. La
// lista de pasos es dinámica: después de "¿Lo recomendarías?" se inserta el
// paso del motivo (si dijo que no) o el de etiquetas (si dijo que sí), así
// que el total de pasos cambia según lo que la persona responda.
private sealed class RatingStep {
    data class Intro(val title: String, val subtitle: String) : RatingStep()
    data class Category(val category: RatingCategory) : RatingStep()
    object Recommend : RatingStep()
    object RecommendReason : RatingStep()
    object RecommendFor : RatingStep()
    object Workload : RatingStep()
    object GradingBasis : RatingStep()
    object ClassStyle : RatingStep()
    object Attendance : RatingStep()
    // Módulo 12 — asistencia declarada (rangos); va al principio.
    object AttendanceLevel : RatingStep()
    object ExamReview : RatingStep()
    object TipComment : RatingStep()
    object HardestPart : RatingStep()
}

private fun buildRatingSteps(professorName: String, form: RatingFormState): List<RatingStep> = buildList {
    add(RatingStep.Intro(
        "Evaluemos a $professorName",
        "Para cada pregunta, elige la tarjeta que más se parezca a tu experiencia — solo vas a escribir donde de verdad importa."
    ))
    add(RatingStep.AttendanceLevel)
    add(RatingStep.Category(RatingCategory.CLARITY))
    add(RatingStep.Category(RatingCategory.ORGANIZATION))
    add(RatingStep.Category(RatingCategory.FAIRNESS))
    add(RatingStep.Category(RatingCategory.AVAILABILITY))
    add(RatingStep.Intro("Como persona", "Ahora sobre el trato hacia los estudiantes y el ambiente en su clase."))
    add(RatingStep.Category(RatingCategory.RESPECT))
    add(RatingStep.Category(RatingCategory.ATMOSPHERE))
    add(RatingStep.Intro("La pregunta más importante", "¿Volverías a tomar clase con este profesor?"))
    add(RatingStep.Recommend)
    // Ahora el motivo se pide siempre, sea "sí" o "no" — ya no depende de
    // la respuesta. Solo "para quién lo recomendarías" sigue siendo
    // exclusivo del "sí".
    add(RatingStep.RecommendReason)
    if (form.wouldRecommend == true) add(RatingStep.RecommendFor)
    add(RatingStep.Intro("Datos rápidos del curso", "Hechos concretos sobre el curso — nada de opiniones."))
    add(RatingStep.Workload)
    add(RatingStep.Attendance)
    add(RatingStep.GradingBasis)
    add(RatingStep.ExamReview)
    add(RatingStep.ClassStyle)
    add(RatingStep.Intro("Para cerrar", "Dos preguntas abiertas y terminamos."))
    add(RatingStep.TipComment)
    add(RatingStep.HardestPart)
}

// Si el paso no exige nada (una intro) siempre deja avanzar; si es una
// pregunta, exige exactamente lo mismo que RatingFormState.isValid() exige
// para esa misma pregunta — este chequeo es lo que prende/apaga "Continuar".
private fun isRatingStepComplete(step: RatingStep, form: RatingFormState): Boolean = when (step) {
    is RatingStep.Intro -> true
    is RatingStep.Category -> {
        val index = form.categoryAnswers.get(step.category)
        index >= 0 && (
            !RatingCards.byCategory.getValue(step.category)[index].requiresNote ||
                isValidComment(form.categoryNotes.get(step.category).orEmpty(), MIN_COMMENT_WORDS)
            )
    }
    RatingStep.Recommend -> form.wouldRecommend != null
    RatingStep.RecommendReason -> isValidComment(form.recommendReason, MIN_RECOMMEND_REASON_WORDS)
    RatingStep.RecommendFor -> true
    RatingStep.Workload -> form.workload != null
    RatingStep.GradingBasis -> form.gradingBasis != null
    RatingStep.ClassStyle -> form.classStyle != null
    RatingStep.Attendance -> form.attendanceMandatory != null
    RatingStep.AttendanceLevel -> form.attendanceBand != null
    RatingStep.ExamReview -> form.allowsReviewOfExams != null
    RatingStep.TipComment -> isValidComment(form.tipComment, MIN_TIP_WORDS)
    RatingStep.HardestPart -> isValidComment(form.hardestPartComment, MIN_COMMENT_WORDS)
}

// Tarjeta de intro de sección — el "Cabito" grande centrado, como las
// pantallas de unidad nueva de Duolingo antes de que empiecen las preguntas.
@Composable
private fun RatingIntroCard(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.cabito_point), "Cabito", Modifier.size(96.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.height(18.dp))
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, lineHeight = 26.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = TadeoMuted, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RatingStepContent(step: RatingStep, form: RatingFormState, vm: RatingViewModel) {
    when (step) {
        is RatingStep.Intro -> RatingIntroCard(step.title, step.subtitle)
        is RatingStep.Category -> CardQuestion(
            step.category, form.categoryAnswers.get(step.category), form.categoryNotes.get(step.category).orEmpty(), form.showValidation,
            onSelect = { vm.setCategoryAnswer(step.category, it) },
            onNoteChange = { vm.setCategoryNote(step.category, it) }
        )
        RatingStep.Recommend -> Column(Modifier.fillMaxWidth()) {
            Text("¿Volverías a tomar clase con este profesor?", fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(12.dp))
            AnswerCard("Sí, lo recomiendo", form.wouldRecommend == true) { vm.setWouldRecommend(true) }
            AnswerCard("No lo recomendaría", form.wouldRecommend == false) { vm.setWouldRecommend(false) }
            if (form.showValidation && form.wouldRecommend == null) ProfileInlineError("Elige una de las dos.")
        }
        RatingStep.RecommendReason -> Column(Modifier.fillMaxWidth()) {
            Text(
                if (form.wouldRecommend == true) "¿Por qué sí lo recomendarías?" else "¿Por qué no lo recomendarías?",
                fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp
            )
            Spacer(Modifier.height(12.dp))
            RequiredMultilineField(
                form.recommendReason, { vm.setRecommendReason(it) },
                if (form.wouldRecommend == true) "Cuéntanos qué te gustó o qué te sirvió de su clase."
                else "Cuéntanos brevemente por qué no lo recomendarías.",
                form.showValidation, minWords = MIN_RECOMMEND_REASON_WORDS
            )
        }
        RatingStep.RecommendFor -> Column(Modifier.fillMaxWidth()) {
            Text("¿Para quién lo recomendarías?", fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp)
            Text("Opcional, puedes marcar varias.", color = TadeoMuted, fontSize = 11.5.sp, modifier = Modifier.padding(top = 3.dp, bottom = 10.dp))
            RecommendFor.labels.forEach { (tag, label) -> AnswerCard(label, tag in form.recommendFor) { vm.toggleRecommendFor(tag) } }
        }
        RatingStep.Workload -> FactPicker("Carga de trabajo fuera de clase", RatingFacts.workloadLabels, form.workload, form.showValidation) { vm.setWorkload(it) }
        RatingStep.GradingBasis -> FactPicker("¿La nota depende más de…?", RatingFacts.gradingBasisLabels, form.gradingBasis, form.showValidation) { vm.setGradingBasis(it) }
        RatingStep.ClassStyle -> FactPicker("Estilo de clase", RatingFacts.classStyleLabels, form.classStyle, form.showValidation) { vm.setClassStyle(it) }
        RatingStep.Attendance -> BoolFactPicker("¿Asistencia obligatoria?", form.attendanceMandatory, form.showValidation) { vm.setAttendanceMandatory(it) }
        RatingStep.AttendanceLevel -> Column(Modifier.fillMaxWidth()) {
            Text(
                "Cuéntanos cuál fue tu porcentaje total de asistencia a clase",
                fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp
            )
            Text(
                "Responde con sinceridad: así cada evaluación cuenta según cuánta clase vio quien la escribe, y las opiniones de quienes sí asisten tienen más peso.",
                color = TadeoMuted, fontSize = 11.5.sp, lineHeight = 16.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            AttendanceBands.labels.forEach { (key, label) ->
                AnswerCard(label, form.attendanceBand == key) { vm.setAttendanceBand(key) }
            }
            if (form.showValidation && form.attendanceBand == null) ProfileInlineError("Elige una opción.")
            if (form.attendanceBand == "minima") {
                Surface(color = Color(0xFFFFF7E6), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text(
                        "Con menos del 40 % de asistencia puedes dejar tu evaluación y la guardamos, pero no cuenta en el promedio del profesor ni se muestra su comentario, porque no viste suficiente clase para juzgarla.",
                        color = TadeoInk, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(11.dp)
                    )
                }
            }
        }
        RatingStep.ExamReview -> BoolFactPicker("¿Permite revisar o reclamar parciales?", form.allowsReviewOfExams, form.showValidation) { vm.setAllowsReviewOfExams(it) }
        RatingStep.TipComment -> Column(Modifier.fillMaxWidth()) {
            Text("¿Qué deberías saber antes de tomar esta clase?", fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(12.dp))
            RequiredMultilineField(form.tipComment, { vm.setTipComment(it) }, "Un tip práctico para quien esté por inscribirse…", form.showValidation, minWords = MIN_TIP_WORDS)
        }
        RatingStep.HardestPart -> Column(Modifier.fillMaxWidth()) {
            Text("¿Qué fue lo más difícil del curso?", fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(12.dp))
            RequiredMultilineField(form.hardestPartComment, { vm.setHardestPartComment(it) }, "Lo que más esfuerzo te costó…", form.showValidation)
        }
    }
}

// submitRating hace upsert por (student_id, professor_id), así que "Enviar"
// y "Actualizar" son el mismo flujo: la diferencia es solo de etiqueta
// según myRating. vm.submit() valida todo antes de llamar a Supabase; si
// por algún motivo algo quedó a medias, prende form.showValidation y el
// LaunchedEffect de abajo salta directo al primer paso incompleto.
@Composable
private fun RatingFormScreen(professor: ProfessorPreview, state: RatingUiState, vm: RatingViewModel, onBack: () -> Unit) {
    val editing = state.myRating != null
    val form = state.form
    val steps = buildRatingSteps(professor.name, form)
    var stepIndex by remember { mutableStateOf(0) }
    val clamped = stepIndex.coerceIn(0, steps.lastIndex)
    val step = steps[clamped]
    val canContinue = isRatingStepComplete(step, form)
    val scope = rememberCoroutineScope()
    // Módulo 11 (ajuste a pedido de Juan): antes la IA solo revisaba todo
    // el texto hasta el botón final "Enviar evaluación" — si alguien
    // escribía una grosería en el primer paso, se enteraba hasta haber
    // llenado TODO el formulario, lo que invita a no volver a intentarlo.
    // Ahora cada paso con caja de texto se revisa apenas se intenta
    // avanzar: `stepBlockedReason` se resetea solo con `remember(clamped)`
    // cada vez que el paso cambia, así el aviso de un paso no se queda
    // pegado al entrar a otro.
    var stepChecking by remember { mutableStateOf(false) }
    var stepBlockedReason by remember(clamped) { mutableStateOf<String?>(null) }

    LaunchedEffect(form.showValidation) {
        if (form.showValidation) {
            val firstIncomplete = steps.indexOfFirst { !isRatingStepComplete(it, form) }
            if (firstIncomplete >= 0) stepIndex = firstIncomplete
        }
    }

    // Texto libre del paso actual, si tiene uno — null en los pasos sin
    // caja de texto (selección de tarjetas, sí/no, etc.), que nunca
    // necesitan pasar por la IA.
    val stepText: String? = when (step) {
        is RatingStep.Category -> form.categoryNotes.get(step.category)?.takeIf { it.isNotBlank() }
        RatingStep.RecommendReason -> form.recommendReason.takeIf { it.isNotBlank() }
        RatingStep.TipComment -> form.tipComment.takeIf { it.isNotBlank() }
        RatingStep.HardestPart -> form.hardestPartComment.takeIf { it.isNotBlank() }
        else -> null
    }
    // Módulo 12 — la pregunta exacta que el estudiante está contestando en
    // este paso: la IA la usa para juzgar si la respuesta tiene sentido.
    val stepQuestion: String? = when (step) {
        is RatingStep.Category -> "Justifica tu respuesta a esta pregunta sobre el profesor: ${step.category.question}"
        RatingStep.RecommendReason ->
            if (form.wouldRecommend == true) "¿Por qué sí lo recomendarías?" else "¿Por qué no lo recomendarías?"
        RatingStep.TipComment -> "¿Qué deberías saber antes de tomar esta clase?"
        RatingStep.HardestPart -> "¿Qué fue lo más difícil del curso?"
        else -> null
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).glassPill(RoundedCornerShape(13.dp))
                    .clickable { if (clamped == 0) onBack() else stepIndex = clamped - 1 },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TadeoInk, modifier = Modifier.size(19.dp)) }
            Spacer(Modifier.width(10.dp))
            LinearProgressIndicator(
                progress = { (clamped + 1f) / steps.size },
                modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                color = TadeoBlue,
                trackColor = Color(0xFFE7EBF0)
            )
        }
        // Módulo 11 — no nulo cuando la IA detectó lenguaje ofensivo en el
        // último intento de enviar: submitRating() nunca se llegó a llamar,
        // así que nada se guardó. Se muestra arriba de todo (sin tener que
        // hacer scroll) porque el estudiante puede estar en cualquier paso
        // del wizard cuando intentó enviar.
        if (state.moderationBlocked != null) {
            Surface(color = Color(0xFFFFF0F0), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("No pudimos enviar tu evaluación", color = Color(0xFFBD4136), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(state.moderationBlocked, color = TadeoInk, fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        // Bloqueo de ESTE paso en particular (ver stepText / moderateStepText).
        if (stepBlockedReason != null) {
            Surface(color = Color(0xFFFFF0F0), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Revisa lo que escribiste aquí", color = Color(0xFFBD4136), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(stepBlockedReason.orEmpty(), color = TadeoInk, fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) { RatingStepContent(step, form, vm) }
        Spacer(Modifier.height(10.dp))
        ProfilePrimary(
            text = when {
                step is RatingStep.Intro -> "Empezar"
                clamped == steps.lastIndex -> if (editing) "Actualizar evaluación" else "Enviar evaluación"
                stepChecking -> "Revisando…"
                else -> "Continuar"
            },
            enabled = canContinue && !stepChecking
        ) {
            when {
                clamped == steps.lastIndex -> vm.submit()
                stepText != null -> {
                    stepBlockedReason = null
                    stepChecking = true
                    scope.launch {
                        when (val result = vm.moderateStepText(stepText, stepQuestion, form.attendanceBand?.let { AttendanceBands.labels[it] })) {
                            is StepModerationResult.Clean -> { stepChecking = false; stepIndex = clamped + 1 }
                            is StepModerationResult.Blocked -> { stepChecking = false; stepBlockedReason = result.reason }
                            // Fail-open: un error técnico (sin red, Gemini caído) no
                            // traba al estudiante. El detalle queda en Logcat
                            // (tag RatingViewModel), ya no se muestra en pantalla.
                            is StepModerationResult.Failed -> { stepChecking = false; stepIndex = clamped + 1 }
                        }
                    }
                }
                else -> stepIndex = clamped + 1
            }
        }
    }
}

// Módulo 05 v2 — lista de reseñas, con dos protecciones para que ningún
// profesor quede expuesto a un "juicio sin justificación" frente a TODOS
// los estudiantes (decisión de Juan, pensando en que esto es su proyecto
// de grado):
//
// 1) Umbral mínimo de reseñas (MIN_REVIEWS_TO_SHOW_COMMENTS) antes de
//    mostrar CUALQUIER comentario abierto. Antes de eso, un solo
//    estudiante molesto sería la única voz pública sobre ese profesor —
//    así que mientras no haya suficientes, solo se ven los promedios y
//    datos rápidos (que ya están en el perfil del profesor).
// 2) Las notas de justificación de categorías mal calificadas y el motivo
//    de la recomendación NUNCA se muestran acá, pasado el umbral o no —
//    ProfessorReview ni siquiera trae esos campos (ver Rating.kt). Lo
//    único que se llega a ver es el tip y "qué fue lo más difícil", que
//    son sobre el CURSO, no un juicio directo a la persona.
//
// "Reportar" sigue existiendo como defensa adicional para esos dos
// comentarios que sí se muestran — no hay panel de administración en esta
// app todavía, así que reportar oculta la reseña de inmediato.
//
// MIN_REVIEWS_TO_SHOW_COMMENTS vive en RatingViewModel.kt (no acá) porque
// Módulo 08/09 (panel del profesor) también la usa para decidir cuándo ya
// hay suficiente muestra como para señalar una categoría débil.

@Composable
private fun ReviewsScreen(professor: ProfessorPreview, state: RatingUiState, vm: RatingViewModel, onBack: () -> Unit) {
    val reviews = state.summary.reviews
    val enoughForComments = state.summary.reviewCount >= MIN_REVIEWS_TO_SHOW_COMMENTS
    ScreenColumn {
        TopBar("Evaluaciones de ${professor.name}", onBack, null)
        when {
            reviews.isEmpty() ->
                GuideCard(R.drawable.cabito_confused, "Todavía no hay evaluaciones", "Sé el primero en evaluar a este profesor.")
            !enoughForComments ->
                GuideCard(
                    R.drawable.cabito_point,
                    "Todavía pocas evaluaciones para mostrar comentarios",
                    "Cuando este profesor llegue a $MIN_REVIEWS_TO_SHOW_COMMENTS evaluaciones vas a poder leer acá los comentarios de otros estudiantes. Mientras tanto, revisa los promedios y los datos rápidos en su perfil."
                )
            else -> reviews.forEach { review ->
                CardSurface {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFB400), modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(String.format("%.1f", review.overall), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(9.dp))
                        Icon(
                            if (review.wouldRecommend) Icons.Filled.Favorite else Icons.Filled.Close,
                            contentDescription = null,
                            tint = if (review.wouldRecommend) Color(0xFF2FBE79) else Color(0xFFBD4136),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(if (review.wouldRecommend) "Lo recomienda" else "No lo recomienda", color = TadeoMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(review.tipComment, color = TadeoInk, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 7.dp))
                    Text(review.hardestPartComment, color = TadeoMuted, fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 5.dp, bottom = 6.dp))
                    TextButton(onClick = { vm.report(review.ratingId) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.padding(top = 6.dp)) {
                        Text("Reportar", color = Color(0xFFBD4136), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Antes mostraba un nombre de facultad fijo ("Facultad de Ciencias Naturales
// e Ingeniería") que no venía de ningún lado y no hacía nada al tocarlo.
// Ahora usa los conteos reales de faculties/careers (Módulo 12) y abre el
// filtro de carrera en Buscar.
@Composable private fun FacultyCard(facultyCount: Int, careerCount: Int, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 12.dp).glassPanel(RoundedCornerShape(18.dp), tint = Color(0xFFE4F8F5)).clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🌿", fontSize = 28.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Explora por facultad y carrera", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    if (facultyCount > 0) "$facultyCount facultades · $careerCount carreras disponibles" else "Cargando catálogo académico…",
                    color = TadeoMuted, fontSize = 11.sp
                )
            }
        }
    }
}

@Composable private fun SearchPill(text: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 13.dp).glassPill().clickable(onClick = onClick)) {
        Row(Modifier.heightIn(min = 50.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = TadeoBlue, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp)); Text(text, color = TadeoMuted, fontSize = 13.sp)
        }
    }
}

// Módulo 03 — versión editable de lo que antes era SearchBarVisual (texto fijo
// de mockup). El ícono de filtro ahora abre el diálogo de filtro por materia.
@Composable private fun SearchBarInput(text: String, onTextChange: (String) -> Unit, onFilterClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 13.dp).glassPill()) {
        Row(Modifier.heightIn(min = 50.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = TadeoBlue, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TadeoInk, fontSize = 13.sp),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner -> if (text.isEmpty()) Text("Profesor, asignatura o fortaleza…", color = TadeoMuted, fontSize = 13.sp); inner() }
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(26.dp).clickable(onClick = onFilterClick), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Tune, contentDescription = "Filtrar", tint = TadeoBlue, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// Fila "○ texto" / "● texto" reutilizada por los dos diálogos de filtro de
// abajo — ahora con un RadioButton de Material de verdad en vez de glifos de
// texto "●"/"○" (que no quedaban centrados igual en todos los teléfonos).
@Composable
private fun FilterRadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = TadeoBlue))
        Text(label, fontSize = 13.sp, modifier = Modifier.padding(start = 2.dp))
    }
}

// Diálogo de filtro por materia: lista las materias activas que trajo
// SearchViewModel.start() y deja elegir una sola (o "Todas").
//
// Antes el Column de la lista usaba heightIn(max = ...) + verticalScroll
// dentro de un Dialog de ancho/alto "por defecto" (usePlatformDefaultWidth),
// y en teléfonos con poca altura disponible el gesto de scroll no alcanzaba
// a moverse — la lista quedaba cortada por el borde de la pantalla sin forma
// de bajar más. Ahora el diálogo ocupa un alto fijo (82% de la pantalla) con
// encabezado y botón "Cerrar" fuera del área que se desplaza, así el scroll
// del medio siempre tiene un alto real y predecible para funcionar.
@Composable
private fun SubjectFilterDialog(subjects: List<Subject>, selected: Subject?, onSelect: (Subject?) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.78f).glassPanel(RoundedCornerShape(26.dp))) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Text("Filtrar por materia", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    FilterRadioRow("Todas las materias", selected == null) { onSelect(null) }
                    if (subjects.isEmpty()) {
                        Text("No hay materias disponibles para filtrar por ahora.", color = TadeoMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        subjects.forEach { subject -> FilterRadioRow(subject.name, selected?.id == subject.id) { onSelect(subject) } }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(top = 4.dp)) {
                    Text("Cerrar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Diálogo de filtro por carrera: mismo FacultyCareerPicker del registro/perfil
// (facultad → carrera), pero en modo de selección única — nunca se le pasa
// más de un id marcado, así que "marcar otra" reemplaza en vez de sumar.
// Mismo arreglo de alto fijo que SubjectFilterDialog para que el scroll
// siempre pueda bajar hasta el final de la lista de carreras.
@Composable
private fun CareerFilterDialog(faculties: List<Faculty>, careers: List<Career>, selected: Career?, onSelect: (Career?) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.82f).glassPanel(RoundedCornerShape(26.dp))) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Text("Filtrar por carrera", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    FilterRadioRow("Todas las carreras", selected == null) { onSelect(null) }
                    FacultyCareerPicker(
                        faculties = faculties,
                        careers = careers,
                        selectedCareerIds = selected?.let { setOf(it.id) } ?: emptySet(),
                        onToggleCareer = { id -> onSelect(if (selected?.id == id) null else careers.find { it.id == id }) }
                    )
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(top = 4.dp)) {
                    Text("Cerrar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// En vez de una rueda sola con la pantalla vacía debajo, mientras llega la
// respuesta de Supabase ya se ve la silueta de las tarjetas de resultado con
// un brillo que se desliza (shimmer) — se nota de inmediato que hay algo
// cargando y dónde va a aparecer, como los loaders nativos de iOS.
@Composable private fun SearchLoadingState() {
    Column(Modifier.fillMaxWidth()) {
        repeat(4) { ProfessorSkeletonCard() }
    }
}

@Composable private fun ProfessorSkeletonCard() {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).glassPanel(RoundedCornerShape(18.dp))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ShimmerBlock(Modifier.size(54.dp), RoundedCornerShape(15.dp))
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                ShimmerBlock(Modifier.fillMaxWidth(0.55f).height(14.dp))
                Spacer(Modifier.height(8.dp))
                ShimmerBlock(Modifier.fillMaxWidth(0.75f).height(11.dp))
                Spacer(Modifier.height(8.dp))
                ShimmerBlock(Modifier.fillMaxWidth(0.3f).height(11.dp))
            }
        }
    }
}

@Composable private fun SearchEmptyState() {
    GuideCard(R.drawable.cabito_confused, "No encontramos coincidencias", "Prueba con otro nombre de profesor o materia, o quita el filtro de materia activo.")
}

@Composable private fun SearchErrorState(error: SearchError?, onRetry: () -> Unit) {
    GuideCard(R.drawable.cabito_confused, error?.title ?: "No pudimos buscar profesores", error?.message ?: "Ocurrió un problema al consultar Supabase.")
    OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(14.dp)) {
        Text("Reintentar", fontWeight = FontWeight.Bold)
    }
}

// Cada ícono ahora vive en su propia "placa" circular con degradado azul→aqua
// muy suave detrás — más aire alrededor (antes solo 6dp de padding) y el
// ícono real de Material en vez de un glifo de texto (⌂/▤/♡) que se veía
// chico y descentrado según la fuente del teléfono.
@Composable private fun QuickCard(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.heightIn(min = 92.dp).glassPanel(RoundedCornerShape(18.dp)).clickable(onClick = onClick)) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                Modifier.size(38.dp).background(Brush.linearGradient(listOf(TadeoBlue.copy(alpha = 0.14f), TadeoAqua.copy(alpha = 0.14f))), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = TadeoBlue, modifier = Modifier.size(19.dp)) }
            Spacer(Modifier.height(8.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, lineHeight = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable private fun SectionHeader(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 19.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f)); TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) { Text(action, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
}

@Composable private fun ProfessorCard(prof: ProfessorPreview, favorite: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).glassPanel(RoundedCornerShape(18.dp)).clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(prof.initials, 54); Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(prof.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(prof.subjectCareerLabel(), color = TadeoMuted, fontSize = 10.5.sp, lineHeight = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFB400), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("${prof.rating}  (${prof.reviews})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 5.dp)) { prof.tags.take(2).forEach { Tag(it) } }
            }
            Icon(
                if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (favorite) "En favoritos" else "Añadir a favoritos",
                tint = if (favorite) Color(0xFFFF686B) else Color(0xFF9AA3B0),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable private fun Avatar(initials: String, size: Int) {
    val shape = RoundedCornerShape(if (size > 60) 22.dp else 15.dp)
    Box(Modifier.size(size.dp).clip(shape).background(Brush.linearGradient(listOf(Color(0xFFDCE9FF), Color(0xFFBDE7E5)))).border(2.dp, Color.White.copy(alpha = 0.85f), shape), contentAlignment = Alignment.Center) { Text(initials, color = TadeoUTBlue, fontWeight = FontWeight.ExtraBold, fontSize = if (size > 60) 22.sp else 14.sp) }
}

@Composable private fun Tag(text: String) { Box(Modifier.glassPill().background(Color(0xFFE4F8F5).copy(alpha = 0.55f), CircleShape)) { Text(text, color = Color(0xFF087A76), fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) } }
@Composable private fun FilterChip(text: String, selected: Boolean, icon: ImageVector? = null, onClick: (() -> Unit)? = null) {
    Box((if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).glassPill().background(if (selected) Color(0xFFE7F0FF).copy(alpha = 0.7f) else Color.Transparent, CircleShape)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color(0xFFFFB400), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text, color = if (selected) TadeoBlue else TadeoInk, fontSize = 10.sp)
        }
    }
}
@Composable private fun Eyebrow(text: String) { Text(text.uppercase(), color = TadeoAqua, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.75.sp) }

// La tarjeta "Cabito" que acompaña casi toda pantalla — ahora es vidrio con
// un tinte celeste muy suave en vez de un bloque plano de color.
@Composable private fun GuideCard(image: Int, title: String, body: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp).glassPanel(RoundedCornerShape(20.dp), tint = Color(0xFFEDF5FF))) {
        Row(Modifier.padding(10.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) { Image(painterResource(image), "Cabito", Modifier.size(54.dp), contentScale = ContentScale.Fit); Spacer(Modifier.width(11.dp)); Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(body, color = TadeoMuted, fontSize = 11.sp, lineHeight = 16.sp) } }
    }
}

@Composable private fun ProfileHeader(initials: String, name: String, subtitle: String, email: String, avatarUrl: String? = null, onEditPhoto: (() -> Unit)? = null, onViewPhoto: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(22.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(22.dp))
                        .let { if (onViewPhoto != null) it.clickable(onClick = onViewPhoto) else it }
                )
            } else {
                Avatar(initials, 72)
            }
            // El badge va encima (se dibuja después en el Box) así que se queda
            // con el toque incluso donde se superpone con la foto.
            if (onEditPhoto != null) {
                Surface(
                    color = TadeoBlue,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(30.dp).align(Alignment.BottomEnd).clickable(onClick = onEditPhoto)
                ) {
                    Box(contentAlignment = Alignment.Center) { EditPencilIcon(Modifier.size(15.dp)) }
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column { Text(name, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp); Text(subtitle, color = TadeoMuted, fontSize = 11.sp, lineHeight = 15.sp); Text(email, color = TadeoMuted, fontSize = 10.sp) }
    }
}

// Lápiz dibujado a mano con Canvas en vez de un glifo Unicode ("✎"): ese
// carácter se ve distinto (a veces casi invisible) según la fuente de cada
// teléfono. Así se ve igual de claro en cualquier dispositivo.
@Composable
private fun EditPencilIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.16f
        val tailStart = Offset(size.width * 0.20f, size.height * 0.80f)
        val tipEnd = Offset(size.width * 0.80f, size.height * 0.20f)
        drawLine(
            color = color,
            start = tailStart,
            end = tipEnd,
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        // Punta del lápiz.
        drawCircle(color = color, radius = stroke * 0.62f, center = tipEnd)
        // Borrador.
        drawCircle(color = color, radius = stroke * 0.7f, center = tailStart)
    }
}

// Pantalla completa para ver la foto de perfil en grande al tocarla
// (distinto de tocar el lápiz, que abre el selector para cambiarla).
@Composable
private fun AvatarPhotoViewer(avatarUrl: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(0.88f).aspectRatio(1f).clip(RoundedCornerShape(20.dp))
            )
            Surface(
                color = Color.White.copy(alpha = 0.18f),
                shape = CircleShape,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp).size(40.dp).clickable(onClick = onDismiss)
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

// Redimensiona a máx. 512px de lado y recomprime a JPEG antes de subir: mantiene
// las fotos livianas sin depender de que el usuario ya las suba optimizadas.
// Revisión general — dos arreglos sobre la versión original:
// 1) Antes se decodificaba la foto COMPLETA a Bitmap (BitmapFactory.decodeStream
//    sin opciones) y solo DESPUÉS se reducía — una foto de cámara moderna
//    (varios miles de píxeles por lado) puede tirar un OutOfMemoryError al
//    decodificar, que ni siquiera el catch de abajo detecta (Error no es
//    Exception, así que el crash no se convierte en avatarPrepFailed()).
//    Ahora primero se mide (inJustDecodeBounds) y se decodifica ya
//    reducida (inSampleSize), nunca a tamaño completo.
// 2) La orientación EXIF se ignoraba por completo: muchas cámaras guardan
//    el archivo "de lado" y solo anotan la rotación correcta en metadata,
//    así que la foto subía y se veía girada/al revés. Se usa
//    android.media.ExifInterface (parte de Android, sin dependencia
//    nueva) para rotarla antes de subirla.
private fun compressAvatarImage(context: android.content.Context, uri: android.net.Uri): ByteArray? {
    return try {
        val resolver = context.contentResolver
        val maxDim = 512

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        val largestSide = maxOf(bounds.outWidth, bounds.outHeight)
        var inSampleSize = 1
        while (largestSide / (inSampleSize * 2) >= maxDim) inSampleSize *= 2

        val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            ?: return null

        val rotationDegrees = resolver.openInputStream(uri)?.use { stream ->
            when (android.media.ExifInterface(stream).getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
        val oriented = if (rotationDegrees != 0) {
            val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        } else decoded

        val largestSideNow = maxOf(oriented.width, oriented.height)
        val scaled = if (largestSideNow > maxDim) {
            val scale = maxDim.toFloat() / largestSideNow
            Bitmap.createScaledBitmap(
                oriented,
                (oriented.width * scale).toInt().coerceAtLeast(1),
                (oriented.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else oriented
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, output)
        output.toByteArray()
    } catch (e: Exception) {
        null
    }
}

private data class SettingsRow(val title: String, val desc: String, val onClick: (() -> Unit)? = null)

@Composable private fun SettingsRows(rows: List<SettingsRow>) {
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).glassPanel(RoundedCornerShape(18.dp))) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 4.dp)) {
            rows.forEachIndexed { i, row ->
                if (i > 0) HorizontalDivider(color = Color(0xFFF0F2F5))
                Row(
                    (if (row.onClick != null) Modifier.fillMaxWidth().clickable(onClick = row.onClick) else Modifier.fillMaxWidth()).padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) { Column(Modifier.weight(1f)) { Text(row.title, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(row.desc, color = TadeoMuted, fontSize = 11.sp, lineHeight = 15.sp) }; Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TadeoMuted, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

// ---- Módulo 02 — Perfil de usuario: formularios y estados de carga/error ----

private class ProfileFormDrafts {
    var initialized by mutableStateOf(false)
    var fullName by mutableStateOf("")
    var studentCode by mutableStateOf("")
    var semester by mutableStateOf("")
    var professionalTitle by mutableStateOf("")
    var bio by mutableStateOf("")
    var currentPassword by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var newPasswordConfirm by mutableStateOf("")
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

@Composable private fun FieldLabel(text: String) { Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TadeoInk) }
@Composable private fun ProfileInlineError(text: String) { Text(text, color = Color(0xFFBD4136), fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)) }

// Botón primario en pastilla con degradado (azul→aqua institucional) y
// sombra suave — el "pill button" con el que Apple remplazó los botones
// cuadrados. Button pinta su propio fondo con containerColor, así que acá
// se deja transparente y el degradado real lo pone el modifier de abajo.
@Composable private fun ProfilePrimary(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(50.dp)
            .shadow(if (enabled) 10.dp else 0.dp, shape, ambientColor = TadeoBlue.copy(alpha = 0.25f), spotColor = TadeoBlue.copy(alpha = 0.25f))
            .clip(shape)
            .background(
                if (enabled) Brush.horizontalGradient(listOf(TadeoBlue, TadeoAqua))
                else Brush.horizontalGradient(listOf(Color(0xFFC7CFDA), Color(0xFFC7CFDA)))
            ),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White
        )
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
private fun TextInput(value: String, onValue: (String) -> Unit, placeholder: String, keyboardType: KeyboardType = KeyboardType.Text) {
    Box(Modifier.fillMaxWidth().height(48.dp).glassPill(RoundedCornerShape(14.dp)).padding(horizontal = 13.dp), contentAlignment = Alignment.CenterStart) {
        BasicTextField(
            value = value,
            onValueChange = onValue,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TadeoInk),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner -> if (value.isEmpty()) Text(placeholder, color = Color(0xFF9AA3B0), fontSize = 14.sp); inner() }
        )
    }
}

@Composable
private fun MultilineInput(value: String, onValue: (String) -> Unit, placeholder: String) {
    Box(Modifier.fillMaxWidth().heightIn(min = 96.dp).glassPill(RoundedCornerShape(14.dp)).padding(horizontal = 13.dp, vertical = 10.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValue,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TadeoInk),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner -> if (value.isEmpty()) Text(placeholder, color = Color(0xFF9AA3B0), fontSize = 14.sp); inner() }
        )
    }
}

@Composable
private fun PasswordInput(value: String, onValue: (String) -> Unit, placeholder: String) {
    // Revisión general — a pedido de Juan: mismo "ojito" que en los campos
    // de contraseña de AuthScreens.kt (login/registro/recuperación), acá
    // para Seguridad → cambiar contraseña.
    var visible by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().height(48.dp).glassPill(RoundedCornerShape(14.dp)).padding(horizontal = 13.dp), contentAlignment = Alignment.CenterStart) {
        BasicTextField(
            value = value,
            onValueChange = onValue,
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TadeoInk),
            modifier = Modifier.fillMaxWidth().padding(end = 26.dp),
            decorationBox = { inner -> if (value.isEmpty()) Text(placeholder, color = Color(0xFF9AA3B0), fontSize = 14.sp); inner() }
        )
        Icon(
            if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
            tint = Color(0xFF9AA3B0),
            modifier = Modifier.align(Alignment.CenterEnd).size(19.dp).clickable { visible = !visible }
        )
    }
}

@Composable
private fun ProfileLoadingScreen(message: String) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Image(painterResource(R.drawable.cabito_loading), "Cabito", Modifier.size(170.dp), contentScale = ContentScale.Fit)
        Text(message, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(14.dp))
        GlassSpinner()
    }
}

@Composable
private fun ProfileErrorScreen(error: ProfileError, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Image(painterResource(R.drawable.cabito_confused), "Cabito", Modifier.size(170.dp), contentScale = ContentScale.Fit)
        Text(error.title, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        Text(error.message, color = TadeoMuted, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 280.dp).padding(top = 6.dp))
        Text(error.recovery, color = TadeoMuted, fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 280.dp).padding(top = 4.dp))
        Spacer(Modifier.height(16.dp))
        Box(Modifier.widthIn(max = 280.dp)) { ProfilePrimary("Intentar de nuevo", onClick = onRetry) }
    }
}

@Composable
private fun PasswordUpdatedScreen(onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Image(painterResource(R.drawable.cabito_success), "Cabito", Modifier.size(170.dp), contentScale = ContentScale.Fit)
        Text("Contraseña actualizada", fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        Text("Tu nueva contraseña ya quedó activa.", color = TadeoMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(16.dp))
        Box(Modifier.widthIn(max = 280.dp)) { ProfilePrimary("Volver a mi perfil", onClick = onDone) }
    }
}

@Composable
private fun PersonalDataScreen(vm: ProfileViewModel, role: AppRole, draft: ProfileFormDrafts) {
    var inline by remember { mutableStateOf<String?>(null) }
    ScreenColumn {
        TopBar("Datos personales", onBack = { vm.backToOverview() })
        FieldLabel("Nombre completo"); Spacer(Modifier.height(6.dp))
        TextInput(draft.fullName, { draft.fullName = it; inline = null }, "Nombre y apellido")
        Spacer(Modifier.height(13.dp))
        if (role == AppRole.STUDENT) {
            FieldLabel("Código estudiantil"); Spacer(Modifier.height(6.dp))
            TextInput(draft.studentCode, { draft.studentCode = it; inline = null }, "Ej. 20231234")
            Spacer(Modifier.height(13.dp))
            FieldLabel("Semestre"); Spacer(Modifier.height(6.dp))
            TextInput(draft.semester, { draft.semester = it.filter(Char::isDigit); inline = null }, "Ej. 6", KeyboardType.Number)
        } else {
            FieldLabel("Título profesional"); Spacer(Modifier.height(6.dp))
            TextInput(draft.professionalTitle, { draft.professionalTitle = it; inline = null }, "Ej. Magíster en Ingeniería de Software")
            Spacer(Modifier.height(13.dp))
            FieldLabel("Biografía"); Spacer(Modifier.height(6.dp))
            MultilineInput(draft.bio, { draft.bio = it; inline = null }, "Cuéntales a tus estudiantes sobre ti")
        }
        inline?.let { ProfileInlineError(it) }
        Spacer(Modifier.height(18.dp))
        ProfilePrimary("Guardar cambios") {
            inline = vm.savePersonalData(draft.fullName, draft.studentCode, draft.semester, draft.professionalTitle, draft.bio)
        }
    }
}

@Composable
private fun SecurityScreen(vm: ProfileViewModel, draft: ProfileFormDrafts) {
    var inline by remember { mutableStateOf<String?>(null) }
    ScreenColumn {
        TopBar("Seguridad", onBack = { vm.backToOverview() })
        Text("Cambiar contraseña", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp))
        // Se pide la contraseña actual a propósito: sin esto, cualquiera con
        // el celular desbloqueado y la sesión abierta podía cambiarle la
        // contraseña al dueño real sin saberla.
        FieldLabel("Contraseña actual"); Spacer(Modifier.height(6.dp))
        PasswordInput(draft.currentPassword, { draft.currentPassword = it; inline = null }, "Tu contraseña actual")
        Spacer(Modifier.height(13.dp))
        FieldLabel("Nueva contraseña"); Spacer(Modifier.height(6.dp))
        PasswordInput(draft.newPassword, { draft.newPassword = it; inline = null }, "Mínimo 8 caracteres")
        Spacer(Modifier.height(13.dp))
        FieldLabel("Confirmar contraseña"); Spacer(Modifier.height(6.dp))
        PasswordInput(draft.newPasswordConfirm, { draft.newPasswordConfirm = it; inline = null }, "Repite tu nueva contraseña")
        inline?.let { ProfileInlineError(it) }
        Spacer(Modifier.height(18.dp))
        ProfilePrimary("Guardar nueva contraseña") {
            inline = vm.changePassword(draft.currentPassword, draft.newPassword, draft.newPasswordConfirm)
        }
    }
}

// Mismo catálogo y mismas reglas que el selector de asignaturas del registro
// docente (Módulo 01, ver AuthScreens.kt), pero editable desde el perfil en
// cualquier momento en vez de una sola vez al registrarse.
@Composable
private fun EditSubjectsScreen(vm: ProfileViewModel) {
    var inline by remember { mutableStateOf<String?>(null) }
    ScreenColumn {
        TopBar("Materias que dictas", onBack = { vm.backToOverview() })
        Text(
            "Elige la facultad y la carrera donde dictas clase, y marca tus materias ahí. Puedes dictar en más de una carrera.",
            color = TadeoMuted, fontSize = 12.sp, lineHeight = 17.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
        )
        if (vm.state.subjects.isEmpty()) {
            Surface(
                color = Color(0xFFFFF7DB),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1DE9A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Aún no hay asignaturas cargadas", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "Supabase no tiene asignaturas activas en la tabla subjects todavía.",
                        color = TadeoMuted, fontSize = 11.sp, lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            FacultyCareerSubjectPicker(
                faculties = vm.state.faculties,
                careers = vm.state.careers,
                subjects = vm.state.subjects,
                subjectCareerLinks = vm.state.subjectCareerLinks,
                selectedSubjectIds = vm.state.selectedSubjectIds,
                onToggleSubject = { vm.toggleSubject(it); inline = null }
            )
        }
        inline?.let { ProfileInlineError(it) }
        Spacer(Modifier.height(10.dp))
        ProfilePrimary("Guardar cambios") {
            inline = vm.saveSubjects()
        }
    }
}

// Mismo catálogo y mismas reglas que el selector de carrera del registro
// estudiantil (ver AuthScreens.kt: StudentCareersScreen), pero editable desde
// el perfil en cualquier momento. Permite más de una carrera (doble
// titulación), igual que al registrarse.
@Composable
private fun EditCareersScreen(vm: ProfileViewModel) {
    var inline by remember { mutableStateOf<String?>(null) }
    ScreenColumn {
        TopBar("Tu carrera", onBack = { vm.backToOverview() })
        Text(
            "Elige tu facultad y tu carrera. Si tienes doble titulación, puedes marcar más de una.",
            color = TadeoMuted, fontSize = 12.sp, lineHeight = 17.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
        )
        if (vm.state.faculties.isEmpty()) {
            Surface(
                color = Color(0xFFFFF7DB),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1DE9A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Aún no hay carreras cargadas", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "Supabase no tiene facultades o carreras cargadas todavía.",
                        color = TadeoMuted, fontSize = 11.sp, lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            FacultyCareerPicker(
                faculties = vm.state.faculties,
                careers = vm.state.careers,
                selectedCareerIds = vm.state.selectedCareerIds,
                onToggleCareer = { vm.toggleCareer(it); inline = null }
            )
        }
        inline?.let { ProfileInlineError(it) }
        Spacer(Modifier.height(10.dp))
        ProfilePrimary("Guardar cambios") {
            inline = vm.saveCareers()
        }
    }
}

// Revisión general — "Privacidad"/"Privacidad y datos" ya no eran botones
// decorativos: esta pantalla cuenta, con cómo de verdad funciona la app (no
// texto legal inventado), qué tan anónima es una evaluación, cuándo se
// oculta una reseña reportada y qué datos guarda la cuenta. Ver
// RatingRepositoryImpl.reportReview / report_professor_rating y
// MIN_REVIEWS_TO_SHOW_COMMENTS.
@Composable
private fun PrivacyScreen(role: AppRole, onBack: () -> Unit) {
    ScreenColumn {
        TopBar(if (role == AppRole.STUDENT) "Privacidad" else "Privacidad y datos", onBack = onBack)
        PrivacyPoint(
            "Tus evaluaciones son anónimas",
            if (role == AppRole.STUDENT)
                "Cuando evalúas a un profesor, él nunca ve tu nombre, tu código estudiantil ni tu usuario — solo el contenido de la evaluación."
            else
                "No puedes ver quién escribió cada evaluación que recibes, ni desde tu panel ni en tu perfil público. Se guarda esa relación para evitar abusos, pero la app nunca te la muestra."
        )
        PrivacyPoint(
            "Los comentarios necesitan evaluaciones suficientes",
            "Un comentario abierto solo aparece en el perfil público de un profesor cuando ya tiene al menos $MIN_REVIEWS_TO_SHOW_COMMENTS evaluaciones. Antes de eso, un solo comentario sería la única voz sobre ese profesor, así que se mantiene oculto."
        )
        PrivacyPoint(
            "Reportar evaluaciones abusivas",
            if (role == AppRole.STUDENT)
                "Si una evaluación de otro estudiante te parece abusiva o falsa, puedes reportarla desde la pantalla de evaluaciones del profesor. Un solo reporte no la oculta: se revisa cuando varios estudiantes distintos la reportan."
            else
                "Si un comentario que recibiste te parece abusivo, puedes reportarlo desde tu panel, en \"Lo que te dicen\". Un solo reporte no lo oculta: se revisa cuando varios estudiantes distintos lo reportan."
        )
        PrivacyPoint(
            "Qué guardamos de tu cuenta",
            if (role == AppRole.STUDENT)
                "Nombre, correo institucional, foto de perfil, código estudiantil, semestre y tus carreras — nada más. Puedes editarlo desde \"Datos personales\" y \"Tu carrera\"."
            else
                "Nombre, correo institucional, foto de perfil, título profesional, biografía y las materias que dictas — nada más. Puedes editarlo desde \"Perfil docente\" y \"Materias que dictas\"."
        )
    }
}

@Composable private fun PrivacyPoint(title: String, body: String) {
    CardSurface {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(body, color = TadeoMuted, fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

// Revisión general — "Accesibilidad" ya no era decorativo: el tamaño de
// texto es el único control que de verdad hace algo en el código actual
// (Compose escala cualquier Text en .sp vía LocalDensity.fontScale — ver
// AccessibilityPreferences.kt y MainActivity.kt). A propósito no se agrega
// un interruptor de "reducir movimiento" ni nada similar que no tendría
// ningún efecto todavía: mismo criterio que quitar "Preferencias".
@Composable
private fun AccessibilityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    ScreenColumn {
        TopBar("Accesibilidad", onBack = onBack)
        Text("Tamaño del texto", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
        Text(
            "Se aplica a toda la app de inmediato, incluyendo lo que ya tienes en pantalla.",
            color = TadeoMuted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccessibilityScaleChip("Normal", AccessibilityPreferences.SCALE_NORMAL, context)
            AccessibilityScaleChip("Grande", AccessibilityPreferences.SCALE_LARGE, context)
            AccessibilityScaleChip("Muy grande", AccessibilityPreferences.SCALE_EXTRA_LARGE, context)
        }
    }
}

@Composable
private fun AccessibilityScaleChip(label: String, scale: Float, context: android.content.Context) {
    FilterChip(
        selected = AccessibilityPreferences.textScale == scale,
        onClick = { AccessibilityPreferences.setTextScale(context, scale) },
        label = { Text(label) }
    )
}

// Revisión general — "Ayuda y soporte" con preguntas reales sobre cómo
// funciona ESTA app. A propósito no incluye un correo o dirección de
// contacto: ese canal no existe todavía en el código, y este proyecto evita
// fingir datos de contacto que no son reales (mismo criterio que el resto
// de la app con el contenido de muestra).
@Composable
private fun HelpScreen(role: AppRole, onBack: () -> Unit) {
    ScreenColumn {
        TopBar("Ayuda y soporte", onBack = onBack)
        if (role == AppRole.STUDENT) {
            HelpItem("¿El profesor sabe que yo escribí una evaluación?", "No. Las evaluaciones son anónimas: el profesor nunca ve tu nombre ni tu usuario, solo el contenido.")
            HelpItem("¿Por qué no veo comentarios en algunos profesores?", "Los comentarios abiertos solo se muestran cuando el profesor ya tiene al menos $MIN_REVIEWS_TO_SHOW_COMMENTS evaluaciones, para que nunca sea la voz de un solo estudiante.")
            HelpItem("Vi una evaluación abusiva o falsa, ¿qué hago?", "Ábrela desde las evaluaciones del profesor y usa el botón de reportar. Se revisa cuando varios estudiantes distintos la reportan.")
            HelpItem("¿Cómo cambio mi carrera o mis materias?", "Desde tu perfil, en \"Tu carrera\". Puedes marcar más de una si tienes doble titulación.")
        } else {
            HelpItem("¿Puedo saber quién escribió una evaluación que recibí?", "No. Son anónimas: ni tu panel ni tu perfil público muestran esa información.")
            HelpItem("Recibí un comentario que me parece abusivo, ¿qué hago?", "Desde tu panel, en \"Lo que te dicen\", puedes reportarlo. Se revisa cuando varios estudiantes distintos lo reportan.")
            HelpItem("¿Cómo actualizo las materias que dicto?", "Desde tu perfil, en \"Materias que dictas\". Puedes dictar en más de una carrera.")
            HelpItem("¿Por qué no veo mis puntos fuertes o débiles todavía?", "Necesitas al menos $MIN_REVIEWS_TO_SHOW_COMMENTS evaluaciones para que el promedio por categoría sea representativo.")
        }
        HelpItem("Olvidé mi contraseña", "Desde tu perfil, en \"Seguridad\", puedes cambiarla confirmando primero tu contraseña actual. Si no la recuerdas, usa la recuperación por código desde la pantalla de inicio de sesión.")
    }
}

@Composable private fun HelpItem(question: String, answer: String) {
    CardSurface {
        Text(question, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, lineHeight = 17.sp)
        Text(answer, color = TadeoMuted, fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable private fun NotificationCard(title: String, body: String, time: String) { CardSurface { Text(title, fontWeight = FontWeight.Bold); Text(body, color = TadeoMuted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp)); Text(time, color = TadeoMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp)) } }

@Composable private fun ScoreRow(label: String, value: Float) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(92.dp)); Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)).border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)) { Box(Modifier.fillMaxWidth(value / 5f).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(TadeoBlue, TadeoAqua)))) }; Text(String.format("%.1f", value), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 10.dp)) }
}

// Revisión general — a pedido de Juan ("el inicio está muy feo"): el ícono
// pasa de ser un glifo suelto junto a la nota a una insignia circular de
// color propio por Kpi (mismo patrón que CategoryScoreCard), para que cada
// tarjeta se lea como un indicador de estado y no como texto plano con un
// adorno.
@Composable private fun Kpi(label: String, value: String, note: String, modifier: Modifier, icon: ImageVector? = null, accent: Color = TadeoBlue) {
    Box(modifier.glassPanel(RoundedCornerShape(16.dp))) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(9.dp))
                    }
                    Spacer(Modifier.width(5.dp))
                }
                Text(label, color = TadeoMuted, fontSize = 8.5.sp)
            }
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, modifier = Modifier.padding(vertical = 3.dp))
            Text(note, color = TadeoMuted, fontSize = 8.sp, lineHeight = 10.sp)
        }
    }
}

// Módulo 08 — antes dibujaba 6 barras de muestra fijas; ahora recibe la
// tendencia real (mes → promedio de ese mes) calculada por monthlyTrend().
// Sin evaluaciones todavía, muestra un estado vacío en vez de "inventar"
// una forma de gráfico que no corresponde a nada.
@Composable private fun MiniChart(data: List<Pair<String, Float>>) {
    CardSurface {
        if (data.isEmpty()) {
            Text("Todavía no hay suficientes evaluaciones para mostrar una tendencia.", color = TadeoMuted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(vertical = 18.dp))
        } else {
            Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Bottom) {
                data.forEach { (_, value) ->
                    Box(Modifier.weight(1f).fillMaxHeight((value / 5f).coerceIn(0.08f, 1f)).clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp)).background(Brush.verticalGradient(listOf(TadeoBlue, Color(0xFF8BB4FF)))))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                data.forEach { (label, _) -> Text(label, color = TadeoMuted, fontSize = 8.sp) }
            }
        }
    }
}

@Composable private fun CategoryCard(label: String, value: String, modifier: Modifier, onClick: () -> Unit) { Box(modifier.glassPanel(RoundedCornerShape(16.dp)).clickable(onClick = onClick)) { Column(Modifier.padding(12.dp)) { Text(label, color = TadeoMuted, fontSize = 10.sp); Row(verticalAlignment = Alignment.Bottom) { Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp); Text(" / 5", color = TadeoMuted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 2.dp)) } } } }

@Composable private fun InsightCard(color: Color, eyebrow: String, title: String, body: String, action: String? = null, onClick: (() -> Unit)? = null) { Box(Modifier.fillMaxWidth().padding(top = 8.dp).glassPanel(RoundedCornerShape(18.dp), tint = color)) { Column(Modifier.padding(13.dp)) { Eyebrow(eyebrow); Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)); Text(body, color = TadeoMuted, fontSize = 10.5.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 4.dp)); if (action != null && onClick != null) TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) { Text(action, fontSize = 11.sp, fontWeight = FontWeight.Bold) } } } }

// La "tarjeta" base que usan Notificaciones, KPIs secundarios, etc. — ahora
// es vidrio (glassPanel) en vez de una tarjeta blanca plana con borde gris.
@Composable private fun CardSurface(content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxWidth().padding(vertical = 5.dp).glassPanel(RoundedCornerShape(18.dp)).padding(14.dp), content = content) }

// ---------------------------------------------------------------------------
// Rediseño de "Mis resultados" y "Plan de mejora" (Juan, nov. 2026): el
// MiniChart y las tarjetas de categoría originales no mostraban números ni
// contexto, así que dos meses con promedios parecidos se veían como barras
// idénticas, y la tarjeta de IA era indistinguible de cualquier otra tarjeta
// blanca de la pantalla. Todo lo de abajo reutiliza la misma paleta de
// Theme.kt y el mismo sistema "glass" de Glass.kt — ningún color nuevo fuera
// de los que ya usa el resto de la app.
// ---------------------------------------------------------------------------

// Franja de color + texto que resume qué tan bien va una categoría — mismos
// tonos que ya usa el resto de la app para "bien"/"atención"/"mal" (ver las
// cajas roja/verde de ProfessorFeedbackCard y el historial de metas de
// ProfessorImprove), así que un profesor que ya vio esos colores en otra
// pantalla reconoce el significado sin tener que aprenderlo de nuevo.
private fun scoreBand(average: Double?): Triple<String, Color, Color> = when {
    average == null -> Triple("Sin datos", TadeoMuted, TadeoSoft)
    average >= 4.5 -> Triple("Excelente", Color(0xFF087A76), Color(0xFFE4F8F5))
    average >= 3.5 -> Triple("Bueno", TadeoBlue, Color(0xFFE7F0FF))
    average >= 2.5 -> Triple("A mejorar", Color(0xFF8A6D00), Color(0xFFFFF7DB))
    else -> Triple("Prioridad", Color(0xFFBD4136), Color(0xFFFFF0F0))
}

private val RatingCategory.icon: ImageVector get() = when (this) {
    RatingCategory.CLARITY -> Icons.Filled.Lightbulb
    RatingCategory.ORGANIZATION -> Icons.Filled.EventNote
    RatingCategory.FAIRNESS -> Icons.Filled.Balance
    RatingCategory.AVAILABILITY -> Icons.Filled.Schedule
    RatingCategory.RESPECT -> Icons.Filled.Favorite
    RatingCategory.ATMOSPHERE -> Icons.Filled.Groups
}

// Pastilla pequeña con ícono — contexto rápido junto al número grande de
// HeroScoreCard, sin repetir los 3 Kpi que ya están en Inicio.
@Composable private fun StatChip(icon: ImageVector, text: String, tint: Color = TadeoUTBlue) {
    Box(Modifier.glassPill(RoundedCornerShape(12.dp))) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
            Text(text, color = TadeoInk, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 5.dp))
        }
    }
}

private data class TrendDelta(val icon: ImageVector, val color: Color, val bg: Color, val label: String)

// Flecha + delta contra la medición anterior — el dato que de verdad
// responde "¿voy mejorando?", que la gráfica de barras vieja no mostraba
// porque dos barras casi iguales no dejan ver un +0.1/-0.1 real. No dibuja
// nada si todavía no hay al menos 2 meses con datos para comparar.
@Composable private fun TrendDeltaBadge(delta: Float?) {
    if (delta == null) return
    val d = when {
        delta >= 0.05f -> TrendDelta(Icons.Filled.TrendingUp, Color(0xFF087A76), Color(0xFFE4F8F5), String.format("%+.1f vs. mes anterior", delta))
        delta <= -0.05f -> TrendDelta(Icons.Filled.TrendingDown, Color(0xFFBD4136), Color(0xFFFFF0F0), String.format("%+.1f vs. mes anterior", delta))
        else -> TrendDelta(Icons.Filled.TrendingFlat, TadeoMuted, TadeoSoft, "Estable vs. mes anterior")
    }
    Surface(color = d.bg, shape = CircleShape) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(d.icon, contentDescription = null, tint = d.color, modifier = Modifier.size(13.dp))
            Text(d.label, color = d.color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

// Tarjeta "hero" de Resultados: el número grande que resume el periodo +
// el delta vs. el mes anterior + contexto (evaluaciones, % que recomienda)
// en una sola fila de pastillas, en vez de repetir los 3 Kpi de Inicio.
@Composable private fun HeroScoreCard(summary: ProfessorRatingSummary, trendDelta: Float?) {
    Box(Modifier.fillMaxWidth().glassPanel(RoundedCornerShape(22.dp), tint = Color(0xFFEEF5FF))) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Eyebrow("Tu valoración general")
                TrendDeltaBadge(trendDelta)
            }
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                Text(summary.averageOverall?.let { String.format("%.1f", it) } ?: "—", fontSize = 42.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold, color = TadeoInk)
                Text(" / 5", color = TadeoMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 3.dp, bottom = 6.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                StatChip(Icons.Filled.Groups, "${summary.reviewCount} evaluaciones")
                summary.recommendPercent?.let { StatChip(Icons.Filled.Favorite, "$it% te recomienda", tint = Color(0xFFE0245E)) }
            }
        }
    }
}

// Gráfica de tendencia mensual — reemplaza a MiniChart. La diferencia clave:
// MiniChart escalaba cada barra sobre 0-5 fijo, así que 4.2 y 4.3 se veían
// como barras casi idénticas; esta usa una escala dinámica sobre el rango
// real de los datos (con un mínimo de 1 punto de rango para no exagerar
// variaciones mínimas) y rotula el valor exacto sobre cada barra, así que la
// tendencia real siempre se nota aunque los promedios estén muy juntos.
@Composable private fun TrendChart(data: List<Pair<String, Float>>) {
    CardSurface {
        if (data.isEmpty()) {
            Text("Todavía no hay suficientes evaluaciones para mostrar una tendencia.", color = TadeoMuted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(vertical = 18.dp))
        } else {
            val minV = data.minOf { it.second }
            val maxV = data.maxOf { it.second }
            val lower = (kotlin.math.floor((minV - 0.4f) * 2f) / 2f).coerceIn(0f, 4f)
            val upper = (kotlin.math.ceil((maxV + 0.4f) * 2f) / 2f).coerceAtMost(5f).coerceAtLeast(lower + 1f)
            val span = (upper - lower).coerceAtLeast(0.5f)
            Box(Modifier.fillMaxWidth().height(118.dp)) {
                Canvas(Modifier.matchParentSize()) {
                    val dash = PathEffect.dashPathEffect(floatArrayOf(5f, 6f), 0f)
                    listOf(0.18f, 0.5f, 0.82f).forEach { frac ->
                        val y = size.height * (1f - frac)
                        drawLine(TadeoBorder, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = dash)
                    }
                }
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Bottom) {
                    data.forEachIndexed { index, pair ->
                        val value = pair.second
                        val fraction = ((value - lower) / span).coerceIn(0.1f, 1f)
                        val isLast = index == data.lastIndex
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                String.format("%.1f", value),
                                color = if (isLast) TadeoInk else TadeoMuted,
                                fontSize = 9.5.sp,
                                fontWeight = if (isLast) FontWeight.ExtraBold else FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth(0.55f)
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                    .background(
                                        if (isLast) Brush.verticalGradient(listOf(TadeoBlue, TadeoAqua))
                                        else Brush.verticalGradient(listOf(Color(0xFFCBDCFF), Color(0xFFE4ECFF)))
                                    )
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                data.forEach { pair -> Text(pair.first, color = TadeoMuted, fontSize = 8.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
            }
        }
    }
}

// Tarjeta de categoría rediseñada: ícono propio por categoría + franja de
// color según qué tan bien va (scoreBand) + barra de progreso — en vez de
// solo texto plano con el número, para que de un vistazo se note cuál
// categoría necesita atención sin tener que leer cada número.
@Composable private fun CategoryScoreCard(category: RatingCategory, breakdown: CategoryBreakdown?, totalReviews: Int) {
    val band = scoreBand(breakdown?.average)
    val bandLabel = band.first; val bandColor = band.second; val bandBg = band.third
    CardSurface {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(bandBg), contentAlignment = Alignment.Center) {
                Icon(category.icon, contentDescription = null, tint = bandColor, modifier = Modifier.size(17.dp))
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(category.shortLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Surface(color = bandBg, shape = CircleShape, modifier = Modifier.padding(top = 2.dp)) {
                    Text(bandLabel, color = bandColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                }
            }
            Text(
                breakdown?.let { String.format("%.1f", it.average) } ?: "—",
                fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TadeoInk
            )
        }
        Box(Modifier.fillMaxWidth().padding(top = 10.dp).height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)).border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)) {
            Box(Modifier.fillMaxWidth((((breakdown?.average ?: 0.0) / 5.0).toFloat()).coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(TadeoBlue, TadeoAqua))))
        }
        val critical = breakdown?.cardCounts?.let { it.getOrElse(2) { 0 } + it.getOrElse(3) { 0 } } ?: 0
        Text(
            if (breakdown == null) "Nadie te ha calificado todavía en esta categoría."
            else if (critical > 0) "$critical de $totalReviews marcaron una tarjeta crítica aquí."
            else "Ninguna tarjeta crítica marcada en esta categoría.",
            color = TadeoMuted, fontSize = 10.5.sp, modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// Tarjeta del plan de mejora — a propósito NO usa CardSurface (el mismo
// fondo de vidrio blanco que cualquier otra tarjeta): lleva su propio fondo
// en degradado azul→aqua para que se lea como sección aparte desde el primer
// vistazo. Tiene tres modos: propuesta (borrador por aceptar), plan aceptado
// (con avance del corte) y cargando/error.
@Composable private fun AiRecommendationsPanel(
    state: ProfessorDashboardUiState,
    onRetry: () -> Unit,
    onAccept: () -> Unit,
    onDiscard: () -> Unit,
    onOpenUpdate: () -> Unit,
    onToggleApplied: (Int) -> Unit,
    onCloseCut: () -> Unit
) {
    val plan = state.activePlan
    val draft = state.aiRecommendations
    var confirmClose by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .shadow(18.dp, RoundedCornerShape(20.dp), ambientColor = TadeoBlue.copy(alpha = 0.25f), spotColor = TadeoBlue.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(TadeoBlue, Color(0xFF1E88FF), TadeoAqua)))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
                Text("TU PLAN DE MEJORA", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.75.sp, modifier = Modifier.padding(start = 8.dp))
            }
            val subtitle = when {
                state.aiLoading -> "Basado en lo que dijeron tus estudiantes"
                draft.isNotEmpty() && state.aiSufficient -> "Propuesta de plan: revísala y acéptala"
                draft.isNotEmpty() -> "Basado en lo que dijeron tus estudiantes"
                plan != null -> "Plan aceptado el ${shortDate(plan.accepted_at)}"
                else -> "Basado en lo que dijeron tus estudiantes"
            }
            Text(subtitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
            when {
                state.aiLoading -> Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassSpinner(size = 28.dp, strokeWidth = 3.dp)
                    Text("Preparando tu plan de mejora…", color = Color.White.copy(alpha = 0.9f), fontSize = 11.5.sp, modifier = Modifier.padding(start = 10.dp))
                }
                // --- Propuesta (borrador): todavía NO está guardada ---
                draft.isNotEmpty() -> {
                    state.aiUpdateReason?.let {
                        Text("Actualizado porque: “$it”", color = Color.White.copy(alpha = 0.85f), fontSize = 10.5.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                    draft.forEachIndexed { index, rec -> PlanRecCard(index, rec, applied = null, onToggle = null) }
                    if (state.aiSufficient) {
                        Text(
                            "Al aceptar, este plan queda guardado como tu objetivo para el próximo corte y lo comparamos con las evaluaciones nuevas.",
                            color = Color.White.copy(alpha = 0.9f), fontSize = 10.5.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                        )
                        PlanPrimaryButton(
                            text = when {
                                state.planSaving -> "Guardando…"
                                plan != null -> "Aceptar y reemplazar mi plan actual"
                                else -> "Aceptar este plan"
                            },
                            enabled = !state.planSaving,
                            onClick = onAccept
                        )
                        Row(Modifier.padding(top = 2.dp)) {
                            TextButton(onClick = onOpenUpdate, enabled = !state.planSaving, contentPadding = PaddingValues(0.dp)) {
                                Text("Actualizar plan →", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                            if (plan != null) {
                                Spacer(Modifier.width(16.dp))
                                TextButton(onClick = onDiscard, enabled = !state.planSaving, contentPadding = PaddingValues(0.dp)) {
                                    Text("Descartar", color = Color.White.copy(alpha = 0.85f), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    state.planError?.let { Text(it, color = Color.White, fontSize = 10.5.sp, modifier = Modifier.padding(top = 4.dp)) }
                    state.aiError?.let { Text(it, color = Color.White, fontSize = 10.5.sp, modifier = Modifier.padding(top = 4.dp)) }
                }
                // --- Plan aceptado: avance del corte ---
                plan != null -> {
                    plan.update_reason?.let {
                        Text("Actualizado porque: “$it”", color = Color.White.copy(alpha = 0.85f), fontSize = 10.5.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                    plan.recommendations.forEachIndexed { index, rec ->
                        PlanRecCard(index, rec, applied = plan.applied.getOrElse(index) { false }, onToggle = { onToggleApplied(index) })
                    }
                    PlanProgressBlock(state.planProgress)
                    Row(Modifier.padding(top = 10.dp)) {
                        TextButton(onClick = onOpenUpdate, enabled = !state.planSaving, contentPadding = PaddingValues(0.dp)) {
                            Text("Actualizar plan →", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    PlanPrimaryButton(
                        text = if (state.planSaving) "Guardando…" else "Cerrar corte y guardar resultado",
                        enabled = !state.planSaving,
                        onClick = { confirmClose = true }
                    )
                    state.planError?.let { Text(it, color = Color.White, fontSize = 10.5.sp, modifier = Modifier.padding(top = 4.dp)) }
                    state.aiError?.let { Text(it, color = Color.White, fontSize = 10.5.sp, modifier = Modifier.padding(top = 4.dp)) }
                }
                // Ya no hay botón de "Generar": si no hay propuesta ni plan y no
                // está cargando, es porque falló (error + "Reintentar") o porque
                // la carga automática está por arrancar en este mismo instante.
                state.aiError != null -> {
                    Text(state.aiError, color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                    Surface(
                        color = Color.White, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onRetry)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = TadeoBlue, modifier = Modifier.size(16.dp))
                            Text("Reintentar", color = TadeoBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(start = 7.dp))
                        }
                    }
                }
                else -> Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassSpinner(size = 28.dp, strokeWidth = 3.dp)
                    Text("Preparando tu plan de mejora…", color = Color.White.copy(alpha = 0.9f), fontSize = 11.5.sp, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
    }
    if (confirmClose) {
        AlertDialog(
            onDismissRequest = { confirmClose = false },
            title = { Text("¿Cerrar este corte?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    "Guardamos en tu historial el resultado de este plan (qué acciones aplicaste y cómo cambiaron tus promedios con las evaluaciones nuevas) y te proponemos un plan nuevo para el siguiente corte. El historial no se borra.",
                    fontSize = 12.sp, lineHeight = 17.sp
                )
            },
            confirmButton = { TextButton(onClick = { confirmClose = false; onCloseCut() }) { Text("Cerrar corte", fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { confirmClose = false }) { Text("Cancelar") } }
        )
    }
}

// "9 nov 2026" a partir de un timestamp ISO ("2026-11-09T…").
private fun shortDate(iso: String?): String {
    val parts = iso?.take(10)?.split("-") ?: return "—"
    val year = parts.getOrNull(0) ?: return "—"
    val month = parts.getOrNull(1)?.toIntOrNull()?.let { MONTH_LABELS.getOrNull(it - 1) } ?: return "—"
    val day = parts.getOrNull(2)?.toIntOrNull() ?: return "—"
    return "$day ${month.lowercase()} $year"
}

@Composable private fun PlanField(label: String, text: String) {
    Column(Modifier.padding(top = 6.dp)) {
        Text(label.uppercase(), color = Color.White.copy(alpha = 0.7f), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        Text(text, color = Color.White.copy(alpha = 0.94f), fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 1.dp))
    }
}

// Una acción del plan: categoría, título, QUÉ mejorar, QUÉ hacer y CÓMO se va
// a medir. En el plan aceptado además lleva "Ya lo apliqué" (applied != null).
@Composable private fun PlanRecCard(index: Int, rec: AiRecommendation, applied: Boolean?, onToggle: (() -> Unit)?) {
    Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(Modifier.padding(12.dp)) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                Text("${index + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                rec.category?.let {
                    Text(it.shortLabel.uppercase(), color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                Text(rec.titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                if (rec.objetivo.isNotBlank()) PlanField("Qué vamos a mejorar", rec.objetivo)
                PlanField("Qué hacer", rec.recomendacion)
                if (rec.indicador.isNotBlank()) PlanField("Cómo lo vamos a medir", rec.indicador)
                if (applied != null && onToggle != null) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 6.dp).clickable(onClick = onToggle),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = applied,
                            onCheckedChange = { onToggle() },
                            colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color.White, checkmarkColor = TadeoBlue)
                        )
                        Text("Ya lo apliqué", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// Cómo van los promedios de las categorías del plan con las evaluaciones que
// llegaron DESPUÉS de aceptarlo (el "siguiente corte").
@Composable private fun PlanProgressBlock(progress: PlanProgress?) {
    if (progress == null) return
    Surface(color = Color.White.copy(alpha = 0.18f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("AVANCE DE ESTE CORTE", color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
            Text(
                "Evaluaciones nuevas desde que aceptaste el plan: ${progress.newReviews}",
                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp)
            )
            if (!progress.enough) {
                Text(
                    "Cuando lleguen al menos $MIN_NEW_REVIEWS_FOR_COMPARISON evaluaciones nuevas vas a ver aquí si mejoraste en lo que te propusiste.",
                    color = Color.White.copy(alpha = 0.88f), fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                progress.deltas.forEach { delta ->
                    val change = delta.change
                    val mark = when {
                        change == null -> "—"
                        change >= IMPROVEMENT_THRESHOLD -> "▲ mejoró"
                        change <= -IMPROVEMENT_THRESHOLD -> "▼ bajó"
                        else -> "= igual"
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(delta.category.shortLabel, color = Color.White, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                        Text(
                            if (delta.before != null && delta.after != null) String.format("%.1f → %.1f  %s", delta.before, delta.after, mark) else mark,
                            color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable private fun PlanPrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (enabled) Color.White else Color.White.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = TadeoBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// Pide el MOTIVO para actualizar el plan: eso (y el plan anterior) se le manda
// a la IA para que el nuevo plan atienda lo que le faltó al profesor.
@Composable private fun PlanUpdateDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    val trimmed = reason.trim()
    val valid = trimmed.length >= 15
    val suggestions = listOf(
        "Ya apliqué estas acciones y quiero nuevas",
        "Las acciones no fueron lo bastante claras",
        "No se ajustan a mi curso o a mi forma de dar clase",
        "Quiero un enfoque distinto"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Por qué quieres actualizar el plan?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column {
                Text(
                    "Cuéntanos qué te faltó o qué no te sirvió del plan actual. Con eso preparamos uno nuevo que lo tenga en cuenta.",
                    color = TadeoMuted, fontSize = 11.5.sp, lineHeight = 16.sp
                )
                Spacer(Modifier.height(8.dp))
                suggestions.forEach { suggestion ->
                    Surface(
                        color = Color(0xFFEAF1FF), shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(bottom = 5.dp).clickable { reason = if (reason.isBlank()) suggestion else "$reason. $suggestion".take(400) }
                    ) {
                        Text(suggestion, color = TadeoBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= 400) reason = it },
                    placeholder = { Text("Escribe aquí tu razón…", fontSize = 12.sp) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
                Text(
                    "${trimmed.length}/400 · escribe al menos 15 caracteres",
                    color = if (valid) Color(0xFF2FBE79) else Color(0xFFE05B4E),
                    fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(trimmed) }, enabled = valid) { Text("Generar plan nuevo", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// Historial permanente de planes: nada de lo que el profesor aceptó se
// pierde — ni los cerrados ni los que reemplazó por uno nuevo.
@Composable private fun PlanHistoryCard(history: List<ImprovementPlanRow>) {
    CardSurface {
        Text("Historial de planes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        if (history.isEmpty()) {
            Text(
                "Aquí van a quedar guardados tus planes anteriores y cómo te fue en cada corte.",
                color = TadeoMuted, fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            history.forEach { plan ->
                Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Text(
                        "${shortDate(plan.accepted_at)} → ${shortDate(plan.closed_at)} · " + if (plan.status == "cerrado") "Corte cerrado" else "Reemplazado por un plan nuevo",
                        fontWeight = FontWeight.Bold, fontSize = 11.5.sp
                    )
                    Text(
                        plan.recommendations.joinToString(" · ") { it.titulo },
                        color = TadeoMuted, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 2.dp)
                    )
                    plan.outcome?.verdict?.let {
                        Text(it, color = TadeoInk, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                    plan.update_reason?.let {
                        Text("Se actualizó porque: “$it”", color = TadeoMuted, fontSize = 10.5.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

// Un ítem de la barra inferior: id para saber cuál está activo, ícono real
// de Material (antes un glifo de texto como "⌂"/"○") y la etiqueta.
private data class NavItem(val id: String, val icon: ImageVector, val label: String)

@Composable private fun StudentBottomNav(active: StudentTab, modifier: Modifier = Modifier, onSelect: (StudentTab) -> Unit) {
    BottomNav(
        listOf(
            NavItem(StudentTab.Home.name, Icons.Filled.Home, "Inicio"),
            NavItem(StudentTab.Search.name, Icons.Filled.Search, "Buscar"),
            NavItem(StudentTab.Favorites.name, Icons.Filled.FavoriteBorder, "Favoritos"),
            NavItem(StudentTab.Profile.name, Icons.Filled.Person, "Perfil")
        ),
        active.name, modifier
    ) { onSelect(StudentTab.valueOf(it)) }
}

@Composable private fun ProfessorBottomNav(active: ProfessorTab, modifier: Modifier = Modifier, onSelect: (ProfessorTab) -> Unit) {
    BottomNav(
        listOf(
            NavItem(ProfessorTab.Home.name, Icons.Filled.Home, "Inicio"),
            NavItem(ProfessorTab.Results.name, Icons.Filled.BarChart, "Resultados"),
            NavItem(ProfessorTab.Improve.name, Icons.Filled.TrendingUp, "Mejorar"),
            NavItem(ProfessorTab.Profile.name, Icons.Filled.Person, "Perfil")
        ),
        active.name, modifier
    ) { onSelect(ProfessorTab.valueOf(it)) }
}

// Barra inferior "flotante": ya no toca los bordes de la pantalla ni usa un
// rectángulo blanco plano — es una pastilla de vidrio con margen alrededor,
// como las barras de pestañas rediseñadas de iOS. Los íconos ahora son
// vectores de Material en vez de glifos de texto, así que quedan del mismo
// tamaño y perfectamente centrados en cualquier teléfono.
@Composable private fun BottomNav(items: List<NavItem>, active: String, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    Box(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp)
            .glassPanel(RoundedCornerShape(26.dp))
    ) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp, vertical = 7.dp)) {
            items.forEach { item ->
                val selected = item.id == active
                val tint = if (selected) TadeoBlue else Color(0xFF637086)
                Column(Modifier.weight(1f).fillMaxHeight().clickable { onSelect(item.id) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(21.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(item.label, color = tint, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
