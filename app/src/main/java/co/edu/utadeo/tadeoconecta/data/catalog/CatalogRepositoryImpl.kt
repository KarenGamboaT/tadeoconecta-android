package co.edu.utadeo.tadeoconecta.data.catalog

import co.edu.utadeo.tadeoconecta.core.network.SupabaseProvider
import co.edu.utadeo.tadeoconecta.domain.model.Career
import co.edu.utadeo.tadeoconecta.domain.model.Faculty
import co.edu.utadeo.tadeoconecta.domain.model.SubjectCareerLink
import co.edu.utadeo.tadeoconecta.domain.repository.CatalogRepository
import io.github.jan.supabase.postgrest.from

// Catálogo chico (unas pocas facultades/carreras) — se trae todo y se agrupa
// en Kotlin, igual que el resto del repo evita los embeds de Postgrest.
class CatalogRepositoryImpl : CatalogRepository {
    private val supabase get() = SupabaseProvider.client

    override suspend fun faculties(): List<Faculty> =
        supabase.from("faculties")
            .select()
            .decodeList<Faculty>()
            .sortedBy { it.name }

    override suspend fun careers(): List<Career> =
        supabase.from("careers")
            .select()
            .decodeList<Career>()
            .sortedBy { it.name }

    override suspend fun subjectCareerLinks(): List<SubjectCareerLink> =
        supabase.from("subject_careers")
            .select()
            .decodeList<SubjectCareerLink>()
}
