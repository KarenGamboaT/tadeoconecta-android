package co.edu.utadeo.tadeoconecta.data.professor

import co.edu.utadeo.tadeoconecta.core.network.SupabaseProvider
import co.edu.utadeo.tadeoconecta.domain.model.ProfessorSubjectInsert
import co.edu.utadeo.tadeoconecta.domain.model.Subject
import co.edu.utadeo.tadeoconecta.domain.repository.AuthRepository
import co.edu.utadeo.tadeoconecta.domain.repository.ProfessorRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ProfessorRepositoryImpl(
    private val authRepository: AuthRepository
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
}
