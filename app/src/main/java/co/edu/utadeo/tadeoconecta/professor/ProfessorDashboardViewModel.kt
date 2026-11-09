package co.edu.utadeo.tadeoconecta.professor

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.utadeo.tadeoconecta.data.auth.AuthRepositoryImpl
import co.edu.utadeo.tadeoconecta.data.professor.ImprovementGoalRepositoryImpl
import co.edu.utadeo.tadeoconecta.data.professor.ImprovementPlanRepositoryImpl
import co.edu.utadeo.tadeoconecta.data.rating.RatingRepositoryImpl
import co.edu.utadeo.tadeoconecta.domain.model.AiRecommendation
import co.edu.utadeo.tadeoconecta.domain.model.AiReportVerdict
import co.edu.utadeo.tadeoconecta.domain.model.ImprovementPlanRow
import co.edu.utadeo.tadeoconecta.domain.model.PlanMath
import co.edu.utadeo.tadeoconecta.domain.model.PlanProgress
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorFeedbackNote
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorGoalHistoryEntry
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorImprovementGoal
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorRatingSummary
import co.edu.utadeo.tadeoconecta.domain.model.RatingCategory
import co.edu.utadeo.tadeoconecta.domain.model.ReportReason
import co.edu.utadeo.tadeoconecta.domain.repository.AuthRepository
import co.edu.utadeo.tadeoconecta.domain.repository.ImprovementGoalRepository
import co.edu.utadeo.tadeoconecta.domain.repository.ImprovementPlanRepository
import co.edu.utadeo.tadeoconecta.domain.repository.RatingRepository
import co.edu.utadeo.tadeoconecta.rating.MIN_REVIEWS_TO_SHOW_COMMENTS
import kotlinx.coroutines.launch

// Módulo 08 — resultados reales del profesor (antes esta pantalla era
// contenido de muestra fijo en MainShell.kt) y Módulo 09 — plan de mejora
// (categoría más débil + acciones sugeridas + objetivo persistente). Un
// solo ViewModel para ambos: comparten exactamente la misma carga de
// datos, separar Home/Resultados/Mejora es solo cuestión de qué pestaña se
// ve, no de qué se consulta — mismo espíritu que RatingViewModel con
// Overview/Form/Reviews.
//
// A diferencia de RatingViewModel (que recibe el id de UN profesor elegido
// por el estudiante), acá nunca hay "profesor elegido": siempre es el
// propio usuario autenticado viendo SUS PROPIOS resultados — por eso no
// hay un load(professorId), solo load().
enum class ProfessorDashboardScreen { Loading, Home, Results, Improve, Error }

enum class ProfessorDashboardError(val title: String, val message: String, val recovery: String) {
    LoadFailed(
        "No pudimos cargar tu panel",
        "Ocurrió un problema al consultar Supabase.",
        "Revisa tu conexión y vuelve a intentarlo."
    )
}

data class ProfessorDashboardUiState(
    val screen: ProfessorDashboardScreen = ProfessorDashboardScreen.Loading,
    val summary: ProfessorRatingSummary = ProfessorRatingSummary.EMPTY,
    // Lo que escribieron los estudiantes, completo y en privado — ver
    // ProfessorFeedbackNote. SOLO esta pantalla (la del propio profesor)
    // puede llegar a mostrar esto.
    val feedback: List<ProfessorFeedbackNote> = emptyList(),
    val goal: ProfessorImprovementGoal? = null,
    // Módulo 09 — metas anteriores (reemplazadas o quitadas), más reciente
    // primero. Se carga junto con el resto en load() y se refresca después
    // de clearGoal()/setGoal() exitosos, que son los dos únicos momentos en
    // que se archiva una meta nueva — ver ImprovementGoalRepositoryImpl.
    val goalHistory: List<ProfessorGoalHistoryEntry> = emptyList(),
    // Módulo 11 — reemplaza el reporte de un solo tap: ahora el profesor
    // elige un motivo (como reportar una cuenta en Meta) y una IA decide
    // de inmediato. reportDialogFor es el ratingId para el que el diálogo
    // de motivo está abierto (null = cerrado); reportSubmitting indica que
    // ya se mandó un motivo y se está esperando el veredicto de la IA;
    // reportVerdicts guarda, por ratingId, el veredicto ya recibido — para
    // poder mostrarlo (bajó o no, y por qué) sin tener que esperar a
    // recargar todo el panel. Un ratingId con bajado=true en este mapa ya
    // no debe seguir mostrándose en la lista (ver feedback visible en
    // MainShell.kt).
    val reportDialogFor: String? = null,
    val reportSubmitting: Boolean = false,
    val reportVerdicts: Map<String, AiReportVerdict> = emptyMap(),
    // No nulo cuando el último intento de reportar falló por una razón
    // técnica (red, Gemini sin cuota) — nunca por un veredicto de la IA,
    // eso va a reportVerdicts. Se limpia al abrir el diálogo o reintentar.
    val reportError: String? = null,
    // Puntos 1+2 de la lista de Juan — recomendaciones de IA (ver
    // generateAiRecommendations() más abajo). A PROPÓSITO no se cargan
    // junto con el resto en load(): cada una es una llamada real a la Edge
    // Function (que a su vez llama a Gemini), así que piden un tap
    // explícito del profesor en vez de gastar cuota de la API cada vez que
    // alguien solo abre su panel. aiError guarda el mensaje si la última
    // llamada falló (cuota agotada, sin red, etc.), para poder mostrarlo
    // sin perder las recomendaciones anteriores si ya había alguna.
    val aiRecommendations: List<AiRecommendation> = emptyList(),
    val aiLoading: Boolean = false,
    val aiError: String? = null,
    // Módulo 12 — false cuando lo generado es solo "todavía pocas
    // evaluaciones" (no se puede aceptar como plan).
    val aiSufficient: Boolean = true,
    // Razón que dio el profesor para el borrador actual (si es una
    // actualización); se guarda junto con el plan al aceptarlo.
    val aiUpdateReason: String? = null,
    val updateDialogOpen: Boolean = false,
    // Módulo 12 — plan ACEPTADO (vigente) y su historial permanente.
    val activePlan: ImprovementPlanRow? = null,
    val planHistory: List<ImprovementPlanRow> = emptyList(),
    val planSaving: Boolean = false,
    val planError: String? = null,
    val error: ProfessorDashboardError? = null
) {
    // Avance del plan vigente contra las evaluaciones nuevas (las que
    // llegaron después de aceptarlo) — null si no hay plan vigente.
    val planProgress: PlanProgress? get() = activePlan?.let { PlanMath.progress(it, feedback) }

    // Mismo umbral que ReviewsScreen usa para los comentarios públicos: con
    // pocas evaluaciones, un promedio por categoría todavía no significa
    // nada estable como para señalarlo como "punto débil" o "punto fuerte".
    val enoughForInsights: Boolean get() = summary.reviewCount >= MIN_REVIEWS_TO_SHOW_COMMENTS

    val weakestCategory: RatingCategory? get() =
        if (!enoughForInsights) null else summary.categoryAverages.minByOrNull { it.value.average }?.key

    val strongestCategory: RatingCategory? get() =
        if (!enoughForInsights) null else summary.categoryAverages.maxByOrNull { it.value.average }?.key
}

// Pequeño holder solo para el resultado de load() — Kotlin no trae una
// Quadruple, y crear una data class acá es más claro que anidar Pair/Triple.
private data class DashboardLoad(
    val summary: ProfessorRatingSummary,
    val feedback: List<ProfessorFeedbackNote>,
    val goal: ProfessorImprovementGoal?,
    val history: List<ProfessorGoalHistoryEntry>,
    val activePlan: ImprovementPlanRow?,
    val planHistory: List<ImprovementPlanRow>
)

class ProfessorDashboardViewModel : ViewModel() {
    private val authRepository: AuthRepository = AuthRepositoryImpl()
    private val ratingRepository: RatingRepository = RatingRepositoryImpl(authRepository)
    private val goalRepository: ImprovementGoalRepository = ImprovementGoalRepositoryImpl(authRepository)
    private val planRepository: ImprovementPlanRepository = ImprovementPlanRepositoryImpl()

    var state by mutableStateOf(ProfessorDashboardUiState())
        private set

    // Revisión general — guardado por USUARIO, no por un boolean a secas:
    // este ViewModel vive mientras vive la Activity (viewModel() no lo
    // recrea al cerrar sesión y entrar con otra cuenta), así que un
    // boolean fijo dejaba el panel pegado con los datos del profesor
    // ANTERIOR para quien entrara después, sin volver a consultar
    // Supabase. Guardarlo por id de usuario deja intacto el motivo
    // original del guard (no recargar en cada cambio de pestaña), pero sí
    // recarga cuando cambia quién está autenticado.
    private var loadedForUser: String? = null

    // Se llama desde un LaunchedEffect(Unit) en ProfessorShell: solo carga
    // la primera vez que ESTE usuario entra al panel, no cada vez que se
    // cambia de pestaña (eso lo maneja goToResults/goToImprove sin volver a
    // pedir nada a Supabase).
    fun loadIfNeeded() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId()
            if (userId != null && userId == loadedForUser) return@launch
            loadedForUser = userId
            load()
        }
    }

    fun load() {
        state = state.copy(screen = ProfessorDashboardScreen.Loading, error = null)
        viewModelScope.launch {
            runCatching {
                val professorId = authRepository.currentUserId() ?: error("No hay una sesión autenticada.")
                val summary = ratingRepository.summaryFor(professorId)
                val feedback = ratingRepository.professorFeedback()
                val goal = goalRepository.myGoal()
                val history = goalRepository.history()
                // Módulo 12 — si por algo la tabla de planes todavía no
                // existe, el panel igual carga (solo sin plan guardado).
                val activePlan = runCatching { planRepository.activePlan() }.getOrNull()
                val planHistory = runCatching { planRepository.history() }.getOrDefault(emptyList())
                DashboardLoad(summary, feedback, goal, history, activePlan, planHistory)
            }.onSuccess { result ->
                state = state.copy(
                    screen = ProfessorDashboardScreen.Home,
                    summary = result.summary,
                    feedback = result.feedback,
                    goal = result.goal,
                    goalHistory = result.history,
                    activePlan = result.activePlan,
                    planHistory = result.planHistory
                )
            }.onFailure {
                state = state.copy(screen = ProfessorDashboardScreen.Error, error = ProfessorDashboardError.LoadFailed)
            }
        }
    }

    fun goToResults() { state = state.copy(screen = ProfessorDashboardScreen.Results, error = null) }
    fun goToImprove() { state = state.copy(screen = ProfessorDashboardScreen.Improve, error = null) }
    fun backToHome() { state = state.copy(screen = ProfessorDashboardScreen.Home, error = null) }

    // Revisión general: sin esto, dos taps rápidos de "Marcar como
    // objetivo" o "Quitar objetivo" (antes de que el primero terminara)
    // disparaban dos llamadas reales a Supabase — el Mutex del repositorio
    // (ver ImprovementGoalRepositoryImpl) evita que se pisen los datos,
    // pero igual archivaría dos veces la misma meta en el historial. Este
    // guard evita directamente el segundo tap mientras el primero está en
    // vuelo.
    private var goalActionInFlight = false

    // Único punto de entrada para activar un objetivo: siempre la
    // categoría más débil VIGENTE según el panel — no se elige cualquier
    // categoría a mano, para que el objetivo de verdad corresponda a lo
    // que las evaluaciones están señalando.
    fun startGoalForWeakestCategory() {
        if (goalActionInFlight) return
        val category = state.weakestCategory ?: return
        goalActionInFlight = true
        val optimistic = ProfessorImprovementGoal.blank(category)
        state = state.copy(goal = optimistic)
        viewModelScope.launch {
            runCatching { goalRepository.setGoal(category) }
                .onSuccess { refreshHistory() }
                .onFailure { state = state.copy(goal = null) }
            goalActionInFlight = false
        }
    }

    // Se llama tras un setGoal()/clearGoal() exitoso — son los dos únicos
    // momentos en que ImprovementGoalRepositoryImpl archiva una meta nueva.
    // Un pedido aparte en vez de devolver el historial desde esas mismas
    // funciones: así el repositorio no necesita saber nada de cómo se
    // presenta el historial en pantalla.
    private fun refreshHistory() {
        viewModelScope.launch {
            runCatching { goalRepository.history() }
                .onSuccess { state = state.copy(goalHistory = it) }
        }
    }

    fun toggleAction(index: Int) {
        val goal = state.goal ?: return
        if (index !in goal.actionsDone.indices) return
        val done = !goal.actionsDone[index]
        val previous = goal.actionsDone
        // Optimista: se refleja de inmediato en pantalla y se confirma
        // contra Supabase en segundo plano; si falla, se revierte.
        state = state.copy(goal = goal.copy(actionsDone = previous.toMutableList().also { it[index] = done }))
        viewModelScope.launch {
            runCatching { goalRepository.setActionDone(index, done) }
                .onFailure { state = state.copy(goal = goal.copy(actionsDone = previous)) }
        }
    }

    fun clearGoal() {
        if (goalActionInFlight) return
        val previous = state.goal ?: return
        goalActionInFlight = true
        state = state.copy(goal = null)
        viewModelScope.launch {
            runCatching { goalRepository.clearGoal() }
                .onSuccess { refreshHistory() }
                .onFailure { state = state.copy(goal = previous) }
            goalActionInFlight = false
        }
    }

    fun retryFromError() = load()

    // Puntos 1+2 de la lista de Juan — lo dispara ensureAiRecommendations()
    // al entrar a "Mejorar", y también "Generar de nuevo"/"Reintentar". Mismo guard
    // contra doble-tap que startGoalForWeakestCategory()/clearGoal(); no
    // exige enoughForInsights porque la Edge Function ya decide con cuántas
    // evaluaciones tiene sentido generar algo (y qué decir si son muy
    // pocas) — acá solo se muestra lo que responda.
    private var aiRequestInFlight = false
    private var aiAutoRequested = false

    // A pedido de Juan: el plan con IA se genera solo cada vez que el
    // profesor entra a "Mejorar" (se llama desde un LaunchedEffect de
    // ProfessorImprove). Solo una vez por sesión de este ViewModel: si ya
    // hay recomendaciones no se vuelve a gastar una llamada a Gemini cada
    // vez que cambia de pestaña, y si el intento automático falló tampoco
    // se reintenta solo en bucle — el profesor usa "Reintentar".
    fun ensureAiRecommendations() {
        if (aiAutoRequested || state.activePlan != null || state.aiRecommendations.isNotEmpty() || aiRequestInFlight) return
        aiAutoRequested = true
        generateAiRecommendations()
    }

    // reason != null cuando el profesor pidió ACTUALIZAR el plan explicando
    // qué le faltó (Módulo 12): se manda junto con el plan anterior para que
    // el nuevo lo atienda. "Reintentar" (sin reason) reusa la razón del
    // borrador actual si la había.
    fun generateAiRecommendations(reason: String? = null) {
        if (aiRequestInFlight) return
        aiRequestInFlight = true
        val effectiveReason = reason?.trim()?.takeIf { it.isNotEmpty() } ?: state.aiUpdateReason
        val previous = state.aiRecommendations.ifEmpty { state.activePlan?.recommendations.orEmpty() }
        state = state.copy(aiLoading = true, aiError = null, updateDialogOpen = false, planError = null)
        viewModelScope.launch {
            runCatching { ratingRepository.generateImprovementRecommendations(effectiveReason, previous) }
                .onSuccess { response ->
                    state = state.copy(
                        aiLoading = false,
                        aiRecommendations = response.recommendations,
                        aiSufficient = response.suficiente,
                        aiUpdateReason = effectiveReason
                    )
                }
                .onFailure {
                    Log.e("ProfessorDashboardViewModel", "generateImprovementRecommendations() falló", it)
                    state = state.copy(
                        aiLoading = false,
                        aiError = "No pudimos preparar tu plan de mejora en este momento. Intenta de nuevo."
                    )
                }
            aiRequestInFlight = false
        }
    }

    // ---- Módulo 12 — aceptar el plan, avance, cierre de corte e historial ----

    fun openUpdateDialog() { state = state.copy(updateDialogOpen = true) }
    fun closeUpdateDialog() { state = state.copy(updateDialogOpen = false) }

    // Descarta el borrador propuesto y vuelve a mostrar el plan vigente.
    fun discardDraft() {
        state = state.copy(aiRecommendations = emptyList(), aiUpdateReason = null, aiError = null, planError = null)
    }

    private var planActionInFlight = false

    // "Aceptar plan": guarda las recomendaciones + una foto de los promedios
    // de hoy (baseline) como el objetivo del próximo corte. Si ya había un
    // plan vigente queda como "reemplazado" en el historial, nunca se borra.
    fun acceptPlan() {
        if (planActionInFlight || !state.aiSufficient || state.aiRecommendations.isEmpty()) return
        planActionInFlight = true
        val recommendations = state.aiRecommendations
        val reason = state.aiUpdateReason
        val baseline = PlanMath.baseline(state.feedback)
        state = state.copy(planSaving = true, planError = null)
        viewModelScope.launch {
            runCatching {
                planRepository.accept(recommendations, baseline, reason)
                planRepository.activePlan() to planRepository.history()
            }.onSuccess { (active, history) ->
                state = state.copy(
                    planSaving = false,
                    activePlan = active,
                    planHistory = history,
                    aiRecommendations = emptyList(),
                    aiUpdateReason = null
                )
            }.onFailure {
                Log.e("ProfessorDashboardViewModel", "acceptPlan() falló", it)
                state = state.copy(planSaving = false, planError = "No pudimos guardar tu plan. Intenta de nuevo.")
            }
            planActionInFlight = false
        }
    }

    // Marca/desmarca "ya lo apliqué" en una acción del plan vigente.
    fun toggleApplied(index: Int) {
        val plan = state.activePlan ?: return
        if (index !in plan.recommendations.indices) return
        val previous = plan.applied.let { current ->
            if (current.size == plan.recommendations.size) current else List(plan.recommendations.size) { current.getOrElse(it) { false } }
        }
        val updated = previous.toMutableList().also { it[index] = !it[index] }
        state = state.copy(activePlan = plan.copy(applied = updated))
        viewModelScope.launch {
            runCatching { planRepository.setApplied(plan.id, updated) }
                .onFailure {
                    Log.e("ProfessorDashboardViewModel", "setApplied() falló", it)
                    state = state.copy(activePlan = state.activePlan?.copy(applied = previous))
                }
        }
    }

    // Cierra el corte del plan vigente: compara contra las evaluaciones
    // nuevas, guarda el resultado en el historial y propone un plan nuevo.
    fun closeCut() {
        if (planActionInFlight) return
        val plan = state.activePlan ?: return
        val progress = state.planProgress ?: return
        planActionInFlight = true
        val outcome = PlanMath.outcome(plan, progress)
        state = state.copy(planSaving = true, planError = null)
        viewModelScope.launch {
            runCatching {
                planRepository.close(plan.id, outcome)
                planRepository.history()
            }.onSuccess { history ->
                state = state.copy(
                    planSaving = false,
                    activePlan = null,
                    planHistory = history,
                    aiRecommendations = emptyList(),
                    aiUpdateReason = null
                )
                // Ya sin plan vigente: se propone uno nuevo para el siguiente corte.
                aiAutoRequested = false
                ensureAiRecommendations()
            }.onFailure {
                Log.e("ProfessorDashboardViewModel", "closeCut() falló", it)
                state = state.copy(planSaving = false, planError = "No pudimos cerrar el corte. Intenta de nuevo.")
            }
            planActionInFlight = false
        }
    }

    // Módulo 11 — un profesor no tenía forma de reportar feedback privado
    // abusivo (insultos, acoso, nada que ver con una evaluación real) sin
    // depender de que 3 estudiantes distintos reportaran lo mismo — algo
    // que nunca pasa en un reporte privado, porque solo el profesor lo ve.
    // Ahora el profesor elige un motivo (como reportar una cuenta en Meta)
    // y una IA decide de inmediato — ver reportFeedbackWithReason() en
    // RatingRepository y la Edge Function moderate-report.
    fun openReportDialog(ratingId: String) {
        if (ratingId.isBlank() || ratingId in state.reportVerdicts) return
        state = state.copy(reportDialogFor = ratingId, reportError = null)
    }

    fun closeReportDialog() {
        if (state.reportSubmitting) return
        state = state.copy(reportDialogFor = null, reportError = null)
    }

    fun submitReport(ratingId: String, reason: ReportReason) {
        if (state.reportSubmitting) return
        state = state.copy(reportSubmitting = true, reportError = null)
        viewModelScope.launch {
            runCatching { ratingRepository.reportFeedbackWithReason(ratingId, reason) }
                .onSuccess { verdict ->
                    state = state.copy(
                        reportSubmitting = false,
                        reportDialogFor = null,
                        reportVerdicts = state.reportVerdicts + (ratingId to verdict)
                    )
                }
                .onFailure {
                    // Se deja el diálogo abierto con el mismo motivo elegido
                    // para que el profesor pueda reintentar sin tener que
                    // elegir todo de nuevo — un problema de red o de cuota
                    // de Gemini no debe sentirse como "ya reporté y no pasó
                    // nada".
                    Log.e("ProfessorDashboardViewModel", "reportFeedbackWithReason() falló para ratingId=$ratingId", it)
                    state = state.copy(
                        reportSubmitting = false,
                        reportError = "No pudimos procesar el reporte en este momento. Intenta de nuevo."
                    )
                }
        }
    }
}
