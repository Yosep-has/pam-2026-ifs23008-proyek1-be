package org.delcom.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object FarmTable : UUIDTable("farms") {
    val userId = uuid("user_id")
    val title = varchar("title", 100)
    val description = text("description")
    val cover = text("cover").nullable()
    val isDone = bool("is_done")
    val lastVaccinationDate = timestamp("last_vaccination_date").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}