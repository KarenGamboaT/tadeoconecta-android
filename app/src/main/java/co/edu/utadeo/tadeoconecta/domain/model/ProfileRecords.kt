package co.edu.utadeo.tadeoconecta.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class StudentProfileInsert(
    val user_id: String
)
