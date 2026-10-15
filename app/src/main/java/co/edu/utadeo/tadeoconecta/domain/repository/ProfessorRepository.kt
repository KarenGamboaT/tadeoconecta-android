package co.edu.utadeo.tadeoconecta.domain.repository

import co.edu.utadeo.tadeoconecta.domain.model.Subject

interface ProfessorRepository {
    suspend fun redeemRegistrationCode(code: String): Boolean
    suspend fun subjects(): List<Subject>
    suspend fun saveSubjects(subjectIds: Set<String>)
}
