package org.delcom.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.delcom.entities.Farm

@Serializable
data class FarmRequest(
    var userId: String = "",
    var title: String = "",
    var description: String = "",
    var cover: String? = null,
    var isDone: Boolean = false,

    // 🔥 tambahan khusus farm
    @Contextual
    var lastVaccinationDate: Instant? = null,
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "title" to title,
            "description" to description,
            "cover" to cover,
            "isDone" to isDone,
            "lastVaccinationDate" to lastVaccinationDate,
        )
    }

    fun toEntity(): Farm {
        return Farm(
            userId = userId,
            title = title,
            description = description,
            cover = cover,
            isDone = isDone,
            lastVaccinationDate = lastVaccinationDate,
            updatedAt = Clock.System.now()
        )
    }
}