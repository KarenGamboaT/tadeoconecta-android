package co.edu.utadeo.tadeoconecta.rating

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.utadeo.tadeoconecta.data.auth.AuthRepositoryImpl
import co.edu.utadeo.tadeoconecta.data.rating.RatingRepositoryImpl
import co.edu.utadeo.tadeoconecta.domain.model.CategoryAnswers
import co.edu.utadeo.tadeoconecta.domain.model.CategoryNotes
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorRatingRow
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorRatingSummary
import co.edu.utadeo.tadeoconecta.domain.model.RatingCards
import co.edu.utadeo.tadeoconecta.domain.model.RatingCategory
import kotlinx.coroutines.launch

// Módulo 05 — Calificaciones y reseñas. Mismo contrato que los demás módulos:
// entrada → validación → carga → éxito → error → recuperación → reintento → éxito.
// Overview = el resumen (promedio + categorías) que ya vive dentro de
// ProfessorDetailScreen (Módulo 04); Form = calificar/editar; Reviews = ver
// las reseñas con comentario.
enum class RatingScreen { Loading, Overview, Form, Reviews, Error }

// Módulo 11 — resultado de revisar UN paso del wizard (ver moderateStepText
// más abajo). Failed existe aparte de Blocked a propósito: un error técnico
// (sin red, Gemini caído) no es lo mismo que "escribiste algo ofensivo", y
// antes ambos casos quedaban indistinguibles (silenciosamente dejados
// pasar) — eso hacía imposible saber, con solo mirar la pantalla, si el
// chequeo de verdad se ejecutó.
sealed class StepModerationResult {
    object Clean : StepModerationResult()
    data class Blocked(val reason: String) : StepModerationResult()
    data class Failed(val detail: String) : StepModerationResult()
}

enum class RatingError(val title: String, val message: String, val recovery: String) {
    LoadFailed(
        "No pudimos cargar las evaluaciones",
        "Ocurrió un problema al consultar Supabase.",
        "Revisa tu conexión y vuelve a intentarlo."
    ),
    SaveFailed(
        "No pudimos guardar tu evaluación",
        "Tu calificación no se actualizó en Supabase.",
        "Tu selección no se perdió; intenta guardar de nuevo."
    )
}

// Cuántas PALABRAS mínimo exige cada campo de texto obligatorio — así
// nadie lo pasa con una sola letra ("respondiendo por responder"). Revisión
// general, a pedido de Juan: esto era un mínimo de CARACTERES; ahora es un
// mínimo de PALABRAS, porque una sola palabra larga no debería contar más
// que dos cortas — contar palabras mide mejor "cuánto dijo" que contar
// caracteres.
//
// También se subió el mínimo real, pero no a un mismo número fijo (Juan
// sugería 100-250 palabras) para los 5 campos de este formulario: buscando
// cómo se mide un "comentario de calidad" en retroalimentación abierta
// (evaluaciones de curso, encuestas de feedback — ver p.ej.
// kirkpatrickpartners.com/blog/the-what-where-how-why-and-when-of-gathering-actionable-feedback),
// no existe un número mágico universal, pero sí es consistente que la
// ESPECIFICIDAD importa más que la longitud (un ejemplo concreto, no solo
// "más texto"). Pedir 100-250 palabras EN CADA UNO de los campos (hasta 2
// notas de categoría + justificación + tip + lo más difícil) significaría
// exigir entre 500 y más de 1250 palabras en una sola sesión de un
// formulario de celular — eso baja la tasa de gente que lo termina y, peor,
// empuja otra vez hacia el relleno mecánico que TextQuality trata de
// evitar. Por eso cada mínimo sube bastante frente al anterior (traducido
// de caracteres a palabras: ~60/80/120 caracteres eran ~10/14/21 palabras)
// pero se queda en el tamaño de "unas pocas frases con un ejemplo
// concreto". El que más se acerca al rango que pedías es el tip (el
// comentario más leído de la reseña, siempre tuvo el mínimo más alto de
// los tres) — si lo quieres más cerca de 100 solo ahí, dímelo y lo subimos.
// General: notas de justificación de una tarjeta mal calificada y "qué fue
// lo más difícil del curso".
const val MIN_COMMENT_WORDS = 25
// El motivo de "¿volverías a tomar clase?" pide más detalle porque ahora es
// obligatorio sea cual sea la respuesta (sí o no) — decisión de Juan.
const val MIN_RECOMMEND_REASON_WORDS = 35
// El tip para futuros estudiantes es el comentario más leído de la reseña,
// así que pide el mínimo más alto de los tres.
const val MIN_TIP_WORDS = 45

// Revisión general — un solo lugar para "cantidad mínima de palabras Y no
// es relleno sin sentido" (ver TextQuality), así RatingFormState.isValid()
// (lo que de verdad bloquea el envío) e isRatingStepComplete() en
// MainShell.kt (lo que prende/apaga "Continuar" en cada paso del wizard)
// nunca queden desincronizados sobre qué cuenta como "completo".
fun isValidComment(text: String, minWords: Int): Boolean {
    val trimmed = text.trim()
    return TextQuality.wordCount(trimmed) >= minWords && TextQuality.isMeaningful(trimmed)
}

// Umbral mínimo de reseñas antes de mostrar CUALQUIER comentario abierto o
// señalar una categoría como "punto débil" — así un solo estudiante
// molesto nunca es la única voz sobre un profesor. Lo usa ReviewsScreen (lo
// público, en MainShell.kt) y ProfessorDashboardViewModel (lo privado, el
// panel del propio profesor en Módulo 08/09) — mismo número, un solo lugar.
const val MIN_REVIEWS_TO_SHOW_COMMENTS = 5

data class RatingFormState(
    val categoryAnswers: CategoryAnswers = CategoryAnswers.UNANSWERED,
    val categoryNotes: CategoryNotes = CategoryNotes.EMPTY,
    val wouldRecommend: Boolean? = null,
    val recommendReason: String = "",
    val recommendFor: Set<String> = emptySet(),
    val workload: String? = null,
    val attendanceMandatory: Boolean? = null,
    // Módulo 12 — asistencia declarada (ver AttendanceBands). Se pregunta al
    // principio de la evaluación y decide cuánto pesa la reseña.
    val attendanceBand: String? = null,
    val gradingBasis: String? = null,
    val allowsReviewOfExams: Boolean? = null,
    val classStyle: String? = null,
    val tipComment: String = "",
    val hardestPartComment: String = "",
    // Los mensajes de "esto es obligatorio" solo se muestran después del
    // primer intento de enviar — no desde que se abre el formulario en blanco.
    val showValidation: Boolean = false
) {
    // Categorías donde se eligió una de las dos tarjetas más críticas y
    // todavía falta contar qué pasó — el formulario no deja enviar mientras
    // esta lista no esté vacía.
    fun categoriesMissingNote(): List<RatingCategory> = RatingCategory.entries.filter { category ->
        val index = categoryAnswers.get(category)
        index >= 0 &&
            RatingCards.byCategory.getValue(category)[index].requiresNote &&
            !isValidComment(categoryNotes.get(category).orEmpty(), MIN_COMMENT_WORDS)
    }

    fun isValid(): Boolean =
        RatingCategory.entries.all { categoryAnswers.get(it) >= 0 } &&
            categoriesMissingNote().isEmpty() &&
            wouldRecommend != null &&
            isValidComment(recommendReason, MIN_RECOMMEND_REASON_WORDS) &&
            workload != null &&
            attendanceMandatory != null &&
            attendanceBand != null &&
            gradingBasis != null &&
            allowsReviewOfExams != null &&
            classStyle != null &&
            isValidComment(tipComment, MIN_TIP_WORDS) &&
            isValidComment(hardestPartComment, MIN_COMMENT_WORDS)
}

data class RatingUiState(
    val screen: RatingScreen = RatingScreen.Loading,
    val professorId: String = "",
    val summary: ProfessorRatingSummary = ProfessorRatingSummary.EMPTY,
    val myRating: ProfessorRatingRow? = null,
    val form: RatingFormState = RatingFormState(),
    val error: RatingError? = null,
    // Módulo 11 — no nulo cuando la IA determinó que el texto que se acaba
    // de intentar enviar contiene lenguaje ofensivo: RatingFormScreen lo
    // muestra como una tarjeta de aviso y submitRating() NUNCA se llegó a
    // llamar, así que la fila jamás se guardó. Se limpia al volver a
    // intentar enviar (ver submit()).
    val moderationBlocked: String? = null
)

class RatingViewModel : ViewModel() {
    private val authRepository = AuthRepositoryImpl()
    private val ratingRepository = RatingRepositoryImpl(authRepository)

    var state by mutableStateOf(RatingUiState())
        private set

    private var recoveryScreen = RatingScreen.Overview

    // Se llama desde un LaunchedEffect(professor.id) en MainShell.kt cada vez
    // que se abre el perfil de un profesor — así una sola instancia de este
    // ViewModel (viva mientras dure StudentShell) no arrastra el resumen del
    // profesor anterior al abrir uno nuevo.
    fun load(professorId: String) {
        recoveryScreen = RatingScreen.Overview
        state = RatingUiState(screen = RatingScreen.Loading, professorId = professorId)
        viewModelScope.launch {
            runCatching {
                val summary = ratingRepository.summaryFor(professorId)
                val mine = ratingRepository.myRating(professorId)
                summary to mine
            }.onSuccess { (summary, mine) ->
                state = state.copy(screen = RatingScreen.Overview, summary = summary, myRating = mine, form = formFrom(mine))
            }.onFailure {
                state = state.copy(screen = RatingScreen.Error, error = RatingError.LoadFailed)
            }
        }
    }

    // Si ya había evaluado antes, el formulario arranca con sus respuestas
    // (modo edición); si no, en blanco.
    private fun formFrom(mine: ProfessorRatingRow?): RatingFormState {
        if (mine == null) return RatingFormState()
        return RatingFormState(
            categoryAnswers = mine.category_answers,
            categoryNotes = mine.category_notes,
            wouldRecommend = mine.would_recommend,
            recommendReason = mine.recommend_reason,
            recommendFor = mine.recommend_for.toSet(),
            workload = mine.workload,
            attendanceMandatory = mine.attendance_mandatory,
            attendanceBand = mine.attendance_band,
            gradingBasis = mine.grading_basis,
            allowsReviewOfExams = mine.allows_review_of_exams,
            classStyle = mine.class_style,
            tipComment = mine.tip_comment,
            hardestPartComment = mine.hardest_part_comment
        )
    }

    fun goToForm() {
        recoveryScreen = RatingScreen.Form
        state = state.copy(screen = RatingScreen.Form, error = null)
    }

    fun goToReviews() {
        recoveryScreen = RatingScreen.Reviews
        state = state.copy(screen = RatingScreen.Reviews, error = null)
    }

    fun backToOverview() {
        state = state.copy(screen = RatingScreen.Overview, error = null)
    }

    private fun updateForm(update: (RatingFormState) -> RatingFormState) {
        state = state.copy(form = update(state.form))
    }

    // Revisión general: antes esto borraba la nota al cambiar a una tarjeta
    // que no la exigía, porque las tarjetas buenas no tenían caja de texto.
    // Ahora SIEMPRE hay una caja (obligatoria en las dos críticas, opcional
    // en las dos buenas — ver CardQuestion en MainShell.kt), así que ya no
    // tiene sentido borrar nada solo por cambiar de tarjeta: lo que la
    // persona haya escrito se conserva sin importar cuál quede seleccionada.
    fun setCategoryAnswer(category: RatingCategory, index: Int) = updateForm { form ->
        form.copy(categoryAnswers = form.categoryAnswers.with(category, index))
    }

    fun setCategoryNote(category: RatingCategory, note: String) = updateForm {
        it.copy(categoryNotes = it.categoryNotes.with(category, note))
    }

    // La justificación ahora es obligatoria sea cual sea la respuesta, pero
    // el motivo de "sí" y el de "no" son preguntas distintas — si cambia de
    // opinión entre una y otra, se limpia el texto para que no quede un
    // "sí lo recomiendo" pegado a una justificación que en realidad
    // explicaba un "no" (o viceversa). Si solo vuelve a tocar la misma
    // opción que ya tenía, lo que haya escrito se conserva.
    fun setWouldRecommend(value: Boolean) = updateForm {
        if (it.wouldRecommend == value) it else it.copy(wouldRecommend = value, recommendReason = "")
    }

    fun setRecommendReason(value: String) = updateForm { it.copy(recommendReason = value) }

    fun toggleRecommendFor(tag: String) = updateForm {
        it.copy(recommendFor = if (tag in it.recommendFor) it.recommendFor - tag else it.recommendFor + tag)
    }

    fun setWorkload(value: String) = updateForm { it.copy(workload = value) }
    fun setAttendanceMandatory(value: Boolean) = updateForm { it.copy(attendanceMandatory = value) }
    fun setAttendanceBand(value: String) = updateForm { it.copy(attendanceBand = value) }
    fun setGradingBasis(value: String) = updateForm { it.copy(gradingBasis = value) }
    fun setAllowsReviewOfExams(value: Boolean) = updateForm { it.copy(allowsReviewOfExams = value) }
    fun setClassStyle(value: String) = updateForm { it.copy(classStyle = value) }
    fun setTipComment(value: String) = updateForm { it.copy(tipComment = value) }
    fun setHardestPartComment(value: String) = updateForm { it.copy(hardestPartComment = value) }

    fun submit() {
        val form = state.form
        if (!form.isValid()) {
            // No dispara la petición: solo prende los mensajes de "falta
            // esto" para que la persona vea exactamente qué le falta.
            updateForm { it.copy(showValidation = true) }
            return
        }

        val professorId = state.professorId
        recoveryScreen = RatingScreen.Form
        state = state.copy(screen = RatingScreen.Loading, moderationBlocked = null)
        viewModelScope.launch {
            // Módulo 11 — se revisa ANTES de guardar nada. Si la llamada de
            // moderación en sí falla (sin red, Gemini sin cuota, etc.) se
            // deja pasar el envío: una reseña válida no debe quedar
            // atascada por un problema de infraestructura ajeno a lo que
            // el estudiante escribió — solo un ofensivo=true explícito
            // bloquea.
            val blockedReason = runCatching {
                ratingRepository.moderateReviewText(
                    categoryNotes = form.categoryNotes,
                    recommendReason = form.recommendReason,
                    tipComment = form.tipComment,
                    hardestPartComment = form.hardestPartComment
                )
            }.onFailure {
                Log.e("RatingViewModel", "moderateReviewText() falló; se deja pasar el envío", it)
            }.getOrNull()?.takeIf { it.ofensivo }?.motivo

            if (blockedReason != null) {
                state = state.copy(screen = RatingScreen.Form, moderationBlocked = blockedReason)
                return@launch
            }

            runCatching {
                ratingRepository.submitRating(
                    professorId = professorId,
                    categoryAnswers = form.categoryAnswers,
                    categoryNotes = form.categoryNotes,
                    wouldRecommend = form.wouldRecommend!!,
                    recommendReason = form.recommendReason,
                    recommendFor = form.recommendFor.toList(),
                    workload = form.workload!!,
                    attendanceMandatory = form.attendanceMandatory!!,
                    attendanceBand = form.attendanceBand!!,
                    gradingBasis = form.gradingBasis!!,
                    allowsReviewOfExams = form.allowsReviewOfExams!!,
                    classStyle = form.classStyle!!,
                    tipComment = form.tipComment,
                    hardestPartComment = form.hardestPartComment
                )
                ratingRepository.summaryFor(professorId) to ratingRepository.myRating(professorId)
            }.onSuccess { (summary, mine) ->
                state = state.copy(screen = RatingScreen.Overview, summary = summary, myRating = mine, form = formFrom(mine))
            }.onFailure {
                // Diagnóstico — antes esto quedaba en silencio y el Logcat
                // nunca mostraba nada, aunque el guardado SÍ fallara: el
                // "No pudimos guardar tu evaluación" nunca decía por qué.
                // Ahora el error real (RLS, columna, red, etc.) queda en
                // Logcat con el tag "RatingViewModel" para poder ver la
                // causa exacta la próxima vez que esto pase.
                Log.e("RatingViewModel", "submitRating() falló para professorId=$professorId", it)
                state = state.copy(screen = RatingScreen.Error, error = RatingError.SaveFailed)
            }
        }
    }

    // Módulo 11 (ajuste a pedido de Juan): revisa UN SOLO texto apenas el
    // estudiante intenta pasar al siguiente paso del wizard, en vez de
    // esperar a que llene las 10+ preguntas para recién ahí enterarse de
    // que algo quedó bloqueado — eso lo obligaría a rehacer todo el
    // formulario por una sola frase. Se llama desde el botón "Continuar" de
    // RatingFormScreen en MainShell.kt, solo en los pasos que de verdad
    // tienen una caja de texto (nota de categoría, motivo, tip, lo más
    // difícil); el texto se manda en el mismo "hueco" (tipComment) que usa
    // moderate-review-text, da igual cuál — la Edge Function solo evalúa el
    // contenido, no le importa bajo qué nombre de campo llegó.
    //
    // Devuelve Clean (nada que decir), Blocked (la IA marcó el texto como
    // ofensivo — no avanza) o Failed (la llamada en sí reventó: sin red,
    // Gemini sin cuota, etc.). Mismo criterio "fail open" que submit(): un
    // Failed NO bloquea el avance (un problema de infraestructura ajeno al
    // estudiante no debería trabarlo) — pero a diferencia de antes, ya no
    // se traga el error en silencio: RatingFormScreen lo muestra en
    // pantalla para poder diagnosticar sin tener que ir a Logcat.
    suspend fun moderateStepText(
        text: String,
        question: String? = null,
        attendanceLabel: String? = null
    ): StepModerationResult {
        if (text.isBlank()) return StepModerationResult.Clean
        return runCatching {
            ratingRepository.moderateReviewText(
                categoryNotes = CategoryNotes.EMPTY,
                recommendReason = "",
                tipComment = text,
                hardestPartComment = "",
                question = question,
                attendanceLabel = attendanceLabel
            )
        }.fold(
            onSuccess = { verdict ->
                when {
                    verdict.ofensivo -> StepModerationResult.Blocked(verdict.motivo)
                    // Módulo 12 — la respuesta no contesta la pregunta que se
                    // hizo (habla de otra cosa, es relleno o contradice lo que
                    // dijo de su asistencia). Se le pide reformularla.
                    !verdict.relevante -> StepModerationResult.Blocked(
                        verdict.sugerencia.ifBlank {
                            "Tu respuesta no parece contestar esta pregunta. Cuéntanos con tus palabras lo que viviste en esta clase."
                        }
                    )
                    else -> StepModerationResult.Clean
                }
            },
            onFailure = { e ->
                Log.e("RatingViewModel", "moderateStepText() falló; se deja pasar el paso", e)
                StepModerationResult.Failed(e::class.simpleName + ": " + (e.message ?: "sin mensaje"))
            }
        )
    }

    // Moderación: cualquier estudiante puede reportar una reseña ajena por
    // su id (ya no por quién la escribió — ver ProfessorReview.ratingId).
    // No hay pantalla de administración, así que varios reportes distintos
    // ocultan la reseña directamente (ver report_professor_rating).
    fun report(ratingId: String) {
        val professorId = state.professorId
        viewModelScope.launch {
            runCatching { ratingRepository.reportReview(ratingId) }
                .onSuccess { state = state.copy(summary = ratingRepository.summaryFor(professorId)) }
        }
    }

    fun retryFromError() {
        state = state.copy(screen = recoveryScreen, error = null)
    }
}
