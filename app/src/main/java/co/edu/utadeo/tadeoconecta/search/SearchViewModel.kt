package co.edu.utadeo.tadeoconecta.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.utadeo.tadeoconecta.data.auth.AuthRepositoryImpl
import co.edu.utadeo.tadeoconecta.data.catalog.CatalogRepositoryImpl
import co.edu.utadeo.tadeoconecta.data.professor.ProfessorRepositoryImpl
import co.edu.utadeo.tadeoconecta.domain.model.Career
import co.edu.utadeo.tadeoconecta.domain.model.Faculty
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorSummary
import co.edu.utadeo.tadeoconecta.domain.model.Subject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Módulo 03 — Búsqueda de profesores. Mismo contrato que los demás módulos:
// entrada → validación → carga → éxito → error → recuperación → reintento → éxito.
// No hay un estado "vacío de verdad" (Idle): búsqueda en blanco = sin filtro
// de nombre/materia = lista a todos los profesores, igual que el mockup
// original siempre mostraba resultados.
enum class SearchScreen { Loading, Results, Empty, Error }

enum class SearchError(val title: String, val message: String, val recovery: String) {
    LoadFailed(
        "No pudimos buscar profesores",
        "Ocurrió un problema al consultar Supabase.",
        "Revisa tu conexión y vuelve a intentarlo."
    )
}

data class SearchUiState(
    val screen: SearchScreen = SearchScreen.Loading,
    val query: String = "",
    val subjectFilter: Subject? = null,
    val subjectOptions: List<Subject> = emptyList(),
    // Carrera (filtro "Carrera", ya posible ahora que el catálogo de
    // facultades/carreras existe en Supabase — ver Módulo 12) y el catálogo
    // completo para el diálogo de ese filtro.
    val careerFilter: Career? = null,
    val faculties: List<Faculty> = emptyList(),
    val careers: List<Career> = emptyList(),
    val results: List<ProfessorSummary> = emptyList(),
    // Favoritos reales del estudiante (Supabase student_favorites), ya no el
    // Set<String> de ejemplo que vivía como estado local en MainShell.kt.
    val favoriteProfessors: List<ProfessorSummary> = emptyList(),
    val error: SearchError? = null,
    // Chips "⭐ 4+" / "50+ opiniones" — a diferencia de subjectFilter/careerFilter
    // (que sí cambian la consulta a Supabase), estos dos filtran en memoria la
    // misma lista que ya se trajo: averageRating/reviewCount ya vienen reales
    // desde professor_ratings (Módulo 05) en cada ProfessorSummary, así que no
    // hace falta volver a pedirle nada a Supabase al prender/apagar un chip.
    val minRatingFilter: Boolean = false,
    val minReviewsFilter: Boolean = false,
    // Revisión general — pull-to-refresh: distinto de screen == Loading a
    // propósito, que reemplaza toda la lista por el estado de carga inicial
    // (con los skeletons). isRefreshing deja los resultados actuales
    // visibles debajo del indicador mientras se vuelve a consultar.
    val isRefreshing: Boolean = false,
    // Revisión general — orden en memoria, igual que minRatingFilter/
    // minReviewsFilter: no dispara runSearch(), solo reordena la misma
    // lista ya traída. false = el orden que ya devuelve Supabase
    // (relevancia/nombre), true = mejor calificado primero.
    val sortByRating: Boolean = false
) {
    val visibleResults: List<ProfessorSummary> get() {
        val filtered = results.filter { summary ->
            (!minRatingFilter || (summary.averageRating ?: 0.0) >= 4.0) &&
                (!minReviewsFilter || summary.reviewCount >= 50)
        }
        return if (sortByRating) filtered.sortedByDescending { it.averageRating ?: -1.0 } else filtered
    }

    // Revisión general — cuenta solo los filtros que de verdad restringen o
    // reordenan la lista (no el texto de búsqueda, que ya tiene su propia
    // forma de borrarse en el campo). Alimenta el botón "Limpiar filtros".
    val activeFilterCount: Int get() = listOf(
        subjectFilter != null, careerFilter != null, minRatingFilter, minReviewsFilter, sortByRating
    ).count { it }
}

class SearchViewModel : ViewModel() {
    private val authRepository = AuthRepositoryImpl()
    private val catalogRepository = CatalogRepositoryImpl()
    private val professorRepository = ProfessorRepositoryImpl(authRepository, catalogRepository)

    var state by mutableStateOf(SearchUiState())
        private set

    private var searchJob: Job? = null

    // Revisión general: profesores favoritos cuyo toggle ya está en vuelo.
    // Sin esto, dos taps rápidos del corazón (antes de que la primera
    // escritura+recarga terminara) podían leer el mismo currentlyFavorite
    // viejo y pisarse entre sí, dejando el corazón en el estado contrario
    // al que de verdad quedó en Supabase hasta la próxima recarga manual.
    private val favoriteTogglesInFlight = mutableSetOf<String>()

    // Primera carga al entrar a la pestaña Buscar: trae el catálogo de
    // materias y de facultades/carreras (para los diálogos de filtro), los
    // favoritos reales del estudiante, y la lista completa de profesores.
    fun start() {
        viewModelScope.launch {
            runCatching { professorRepository.subjects() }
                .onSuccess { state = state.copy(subjectOptions = it) }
            // si el catálogo falla, el buscador sigue funcionando sin filtro de materia
        }
        viewModelScope.launch {
            runCatching { catalogRepository.faculties() to catalogRepository.careers() }
                .onSuccess { (faculties, careers) -> state = state.copy(faculties = faculties, careers = careers) }
            // si falla, el buscador sigue funcionando sin filtro de carrera
        }
        loadFavorites()
        runSearch()
    }

    // Debounce a propósito (ver runSearch): sin esto, cada letra escrita
    // lanzaba de inmediato hasta ~4 consultas a Supabase y hacía parpadear
    // la pantalla de carga en cada tecla.
    fun setQuery(query: String) {
        state = state.copy(query = query)
        runSearch(debounce = true)
    }

    fun setSubjectFilter(subject: Subject?) {
        state = state.copy(subjectFilter = subject)
        runSearch()
    }

    fun setCareerFilter(career: Career?) {
        state = state.copy(careerFilter = career)
        runSearch()
    }

    // Filtro en memoria (ver SearchUiState.visibleResults) — no dispara
    // runSearch(), solo cambia qué parte de los resultados ya traídos se
    // muestra, así que prender/apagar el chip es instantáneo.
    fun toggleMinRatingFilter() {
        state = state.copy(minRatingFilter = !state.minRatingFilter)
    }

    fun toggleMinReviewsFilter() {
        state = state.copy(minReviewsFilter = !state.minReviewsFilter)
    }

    // Filtro/orden en memoria, mismo criterio que los de arriba.
    fun toggleSortByRating() {
        state = state.copy(sortByRating = !state.sortByRating)
    }

    fun retry() = runSearch()

    // Revisión general — pull-to-refresh en la lista de resultados. A
    // diferencia de runSearch(), nunca pasa por screen = Loading: los
    // resultados actuales se quedan en pantalla debajo del indicador
    // mientras se vuelve a consultar Supabase con los mismos filtros.
    fun refresh() {
        if (state.isRefreshing) return
        state = state.copy(isRefreshing = true)
        viewModelScope.launch {
            val query = state.query.trim()
            val subjectId = state.subjectFilter?.id
            val careerId = state.careerFilter?.id
            runCatching { professorRepository.searchProfessors(query, subjectId, careerId) }
                .onSuccess { results ->
                    state = state.copy(
                        screen = if (results.isEmpty()) SearchScreen.Empty else SearchScreen.Results,
                        results = results
                    )
                }
            state = state.copy(isRefreshing = false)
        }
    }

    // Se llama al volver a Inicio desde Buscar (por el botón de volver o por
    // la barra inferior) — así la próxima vez que el estudiante abra el
    // buscador empieza en blanco, en vez de arrastrar el filtro de materia o
    // carrera de la búsqueda anterior. No hace nada (ni dispara una consulta
    // de más) si ya estaba en blanco.
    fun resetFilters() {
        if (state.query.isEmpty() && state.subjectFilter == null && state.careerFilter == null &&
            !state.minRatingFilter && !state.minReviewsFilter && !state.sortByRating
        ) return
        state = state.copy(query = "", subjectFilter = null, careerFilter = null, minRatingFilter = false, minReviewsFilter = false, sortByRating = false)
        runSearch()
    }

    fun isFavorite(professorId: String): Boolean =
        state.favoriteProfessors.any { it.id == professorId }

    fun loadFavorites() {
        viewModelScope.launch {
            runCatching { professorRepository.favoriteProfessors() }
                .onSuccess { state = state.copy(favoriteProfessors = it) }
            // si falla, los corazones simplemente se ven vacíos hasta el próximo intento
        }
    }

    // Antes solo tocaba un Set<String> en memoria (MainShell.kt); ahora
    // persiste en Supabase y recarga la lista real para que la pestaña
    // Favoritos siempre refleje lo que de verdad quedó guardado.
    fun toggleFavorite(professorId: String) {
        if (professorId in favoriteTogglesInFlight) return
        val currentlyFavorite = isFavorite(professorId)
        favoriteTogglesInFlight += professorId
        viewModelScope.launch {
            runCatching { professorRepository.setFavorite(professorId, !currentlyFavorite) }
                .onSuccess { loadFavorites() }
            favoriteTogglesInFlight -= professorId
        }
    }

    // debounce=true (solo desde setQuery, ver arriba) espera un poco antes
    // de consultar Supabase y recién ahí muestra "cargando" — así escribir
    // rápido no dispara una consulta por letra ni hace parpadear la
    // pantalla. Los demás llamadores (filtros de materia/carrera, reintentar,
    // limpiar filtros) siguen disparando de inmediato, sin esperar nada.
    private fun runSearch(debounce: Boolean = false) {
        searchJob?.cancel()
        val query = state.query.trim()
        val subjectId = state.subjectFilter?.id
        val careerId = state.careerFilter?.id
        if (!debounce) state = state.copy(screen = SearchScreen.Loading, error = null)
        searchJob = viewModelScope.launch {
            if (debounce) {
                delay(350)
                state = state.copy(screen = SearchScreen.Loading, error = null)
            }
            runCatching { professorRepository.searchProfessors(query, subjectId, careerId) }
                .onSuccess { results ->
                    state = state.copy(
                        screen = if (results.isEmpty()) SearchScreen.Empty else SearchScreen.Results,
                        results = results
                    )
                }
                .onFailure {
                    state = state.copy(screen = SearchScreen.Error, error = SearchError.LoadFailed)
                }
        }
    }
}
