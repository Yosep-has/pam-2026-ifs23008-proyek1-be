package org.delcom.dao

import org.delcom.tables.FarmTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import java.util.UUID

class FarmDAO(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, FarmDAO>(FarmTable)

    var userId by FarmTable.userId
    var title by FarmTable.title
    var description by FarmTable.description
    var cover by FarmTable.cover
    var isDone by FarmTable.isDone
    var lastVaccinationDate by FarmTable.lastVaccinationDate
    var createdAt by FarmTable.createdAt
    var updatedAt by FarmTable.updatedAt
}