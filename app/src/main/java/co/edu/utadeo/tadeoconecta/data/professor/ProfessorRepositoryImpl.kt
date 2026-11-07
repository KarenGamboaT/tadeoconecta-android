package co.edu.utadeo.tadeoconecta.data.professor

import co.edu.utadeo.tadeoconecta.core.network.SupabaseProvider
import co.edu.utadeo.tadeoconecta.data.catalog.CatalogRepositoryImpl
import co.edu.utadeo.tadeoconecta.data.rating.RatingRepositoryImpl
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorProfileRow
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorSubjectInsert
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorSubjectLink
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorSubjectRow
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorSummary
import co.edu.utadeo.tadeoconecta.domain.model.StudentFavoriteInsert
import co.edu.utadeo.tadeoconecta.domain.model.StudentFavoriteRow
import co.edu.utadeo.tadeoconecta.domain.model.Subject
import co.edu.utadeo.tadeoconecta.domain.model.UserProfile
import co.edu.utadeo.tadeoconecta.domain.repository.AuthRepository
import co.edu.utadeo.tadeoconecta.domain.repository.CatalogRepository
import co.edu.utadeo.tadeoconecta.domain.repository.ProfessorRepository
import co.edu.utadeo.tadeoconecta.domain.repository.RatingRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ProfessorRepositoryImpl(
    private val authRepository: AuthRepository,
    private val catalogRepository: CatalogRepository = CatalogRepositoryImpl(),
    private val ratingRepository: RatingRepository = RatingRepositoryImpl(authRepository)
) : ProfessorRepository {
    private val supabase get() = SupabaseProvider.client

    override suspend fun redeemRegistrationCode(code: String): Boolean {
        return supabase.postgrest.rpc(
            function = "redeem_professor_code",
            parameters = buildJsonObject {
                put("p_code", code.trim())
            }
        ).decodeAs<Boolean>()
    }

    override suspend fun subjects(): List<Subject> =
        supabase.from("subjects")
            .select {
                filter { eq("active", true) }
            }
            .decodeList<Subject>()
            .sortedBy { it.name }

    override suspend fun saveSubjects(subjectIds: Set<String>) {
        val userId = authRepository.currentUserId()
            ?: error("No hay una sesión autenticada.")

        supabase.from("professor_subjects").delete {
            filter { eq("professor_id", userId) }
        }

        if (subjectIds.isNotEmpty()) {
            supabase.from("professor_subjects").insert(
                subjectIds.map { subjectId ->
                    ProfessorSubjectInsert(
                        professor_id = userId,
                        subject_id = subjectId
                    )
                }
            )
        }
    }

    override suspend fun professorSubjectIds(professorId: String): Set<String> =
        supabase.from("professor_subjects")
            .select {
                filter { eq("professor_id", professorId) }
            }
            .decodeList<ProfessorSubjectRow>()
            .map { it.subject_id }
            .toSet()

    // Profesores que dictan alguna materia de la carrera dada (subject_careers
    // → professor_subjects), para el filtro de chip "Carrera". El caller
    // (searchProfessors) solo la invoca cuando careerId no es null; "sin
    // filtro" se representa ahí como careerFilterIds == null, no llamando a esto.
    private suspend fun professorIdsForCareer(careerId: String): Set<String> {
        val subjectIdsInCareer = catalogRepository.subjectCareerLinks()
            .filter { it.career_id == careerId }
            .map { it.subject_id }
        if (subjectIdsInCareer.isEmpty()) return emptySet()
        return supabase.from("professor_subjects")
            .select { filter { isIn("subject_id", subjectIdsInCareer) } }
            .decodeList<ProfessorSubjectLink>()
            .map { it.professor_id }
            .toSet()
    }

    // Arma ProfessorSummary para un conjunto ya resuelto de ids de profesor:
    // profiles + professor_profiles + materias (professor_subjects/subjects)
    // + carreras reales de esas materias (subject_careers/careers). Compartido
    // entre searchProfessors y favoriteProfessors para no duplicar las
    // consultas. Sin embeds de Postgrest (no hay forma de probarlos sin
    // compilar en este entorno): varias consultas simples, armadas en Kotlin.
    private suspend fun summariesFor(ids: List<String>): List<ProfessorSummary> {
        if (ids.isEmpty()) return emptyList()

        val profiles = supabase.from("profiles")
            .select { filter { isIn("id", ids) } }
            .decodeList<UserProfile>()

        val detailsByUserId = supabase.from("professor_profiles")
            .select { filter { isIn("user_id", ids) } }
            .decodeList<ProfessorProfileRow>()
            .associateBy { it.user_id }

        val subjectNamesById = subjects().associate { it.id to it.name }
        val subjectIdsByProfessorId = supabase.from("professor_subjects")
            .select { filter { isIn("professor_id", ids) } }
            .decodeList<ProfessorSubjectLink>()
            .groupBy({ it.professor_id }, { it.subject_id })

        val careersById = catalogRepository.careers().associateBy { it.id }
        val careerIdsBySubjectId = catalogRepository.subjectCareerLinks()
            .groupBy({ it.subject_id }, { it.career_id })

        // Módulo 05 — promedio/reseñas reales, en una sola consulta para
        // todos los ids en vez de una por profesor.
        val ratingSummaries = ratingRepository.summariesFor(ids)

        return profiles.map { p ->
            val details = detailsByUserId[p.id]
            val subjectIds = subjectIdsByProfessorId[p.id].orEmpty()
            val careerNames = subjectIds
                .flatMap { careerIdsBySubjectId[it].orEmpty() }
                .distinct()
                .mapNotNull { careersById[it]?.name }
                .sorted()
            val ratingSummary = ratingSummaries[p.id]
            ProfessorSummary(
                id = p.id,
                fullName = p.fullName,
                email = p.email,
                avatarUrl = p.avatarUrl,
                professionalTitle = details?.professional_title,
                bio = details?.bio,
                subjectNames = subjectIds.mapNotNull { subjectNamesById[it] }.sorted(),
                careerNames = careerNames,
                averageRating = ratingSummary?.averageOverall,
                reviewCount = ratingSummary?.reviewCount ?: 0
            )
        }.sortedBy { it.fullName }
    }

    override suspend fun searchProfessors(query: String, subjectId: String?, careerId: String?): List<ProfessorSummary> {
        val trimmed = query.trim()

        // Profesores que dictan la materia del filtro de chip (si hay uno activo).
        val subjectFilterIds: Set<String>? = subjectId?.let { sid ->
            supabase.from("professor_subjects")
                .select { filter { eq("subject_id", sid) } }
                .decodeList<ProfessorSubjectLink>()
                .map { it.professor_id }
                .toSet()
        }
        // Profesores que dictan alguna materia de la carrera del filtro (si hay una activa).
        val careerFilterIds: Set<String>? = careerId?.let { professorIdsForCareer(it) }

        // Si algún filtro activo no trae a nadie, no hay nada más que consultar.
        if (subjectFilterIds?.isEmpty() == true || careerFilterIds?.isEmpty() == true) return emptyList()

        val candidateIds: Set<String> = if (trimmed.isEmpty()) {
            // Sin texto de búsqueda: todos los profesores (los filtros de
            // materia/carrera, si hay, se aplican después).
            supabase.from("profiles")
                .select { filter { eq("role", "professor") } }
                .decodeList<UserProfile>()
                .map { it.id }
                .toSet()
        } else {
            // Coincide por nombre del profesor...
            val nameMatchIds = supabase.from("profiles")
                .select {
                    filter {
                        eq("role", "professor")
                        ilike("full_name", "%$trimmed%")
                    }
                }
                .decodeList<UserProfile>()
                .map { it.id }
                .toSet()

            // ...o por el nombre de una materia que dicten.
            val matchingSubjectIds = supabase.from("subjects")
                .select { filter { ilike("name", "%$trimmed%") } }
                .decodeList<Subject>()
                .map { it.id }
            val subjectTextMatchIds = if (matchingSubjectIds.isEmpty()) emptySet() else
                supabase.from("professor_subjects")
                    .select { filter { isIn("subject_id", matchingSubjectIds) } }
                    .decodeList<ProfessorSubjectLink>()
                    .map { it.professor_id }
                    .toSet()

            nameMatchIds + subjectTextMatchIds
        }

        var finalIds = candidateIds
        subjectFilterIds?.let { finalIds = finalIds intersect it }
        careerFilterIds?.let { finalIds = finalIds intersect it }
        if (finalIds.isEmpty()) return emptyList()

        return summariesFor(finalIds.toList())
    }

    override suspend fun favoriteProfessors(): List<ProfessorSummary> {
        val userId = authRepository.currentUserId() ?: return emptyList()
        val ids = supabase.from("student_favorites")
            .select { filter { eq("student_id", userId) } }
            .decodeList<StudentFavoriteRow>()
            .map { it.professor_id }
        return summariesFor(ids)
    }

    override suspend fun setFavorite(professorId: String, favorite: Boolean) {
        val userId = authRepository.currentUserId()
            ?: error("No hay una sesión autenticada.")

        if (favorite) {
            supabase.from("student_favorites").upsert(
                StudentFavoriteInsert(student_id = userId, professor_id = professorId)
            ) {
                onConflict = "student_id,professor_id"
            }
        } else {
            supabase.from("student_favorites").delete {
                filter {
                    eq("student_id", userId)
                    eq("professor_id", professorId)
                }
            }
        }
    }
}
