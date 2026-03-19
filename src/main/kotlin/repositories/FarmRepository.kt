package org.delcom.repositories

import org.delcom.dao.FarmDAO
import org.delcom.entities.Farm
import org.delcom.helpers.farmDAOToModel
import org.delcom.helpers.suspendTransaction
import org.delcom.tables.FarmTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import java.util.UUID

class FarmRepository(private val baseUrl: String) : IFarmRepository {
    override suspend fun getAll(userId: String, search: String): List<Farm> = suspendTransaction {
        if (search.isBlank()) {
            FarmDAO
                .find {
                    (FarmTable.userId eq UUID.fromString(userId))
                }
                .orderBy(FarmTable.createdAt to SortOrder.DESC)
                .map { farmDAOToModel(it, baseUrl) }
        } else {
            val keyword = "%${search.lowercase()}%"

            FarmDAO
                .find {
                    (FarmTable.userId eq UUID.fromString(userId)) and
                            (FarmTable.title.lowerCase() like keyword)
                }
                .orderBy(FarmTable.title to SortOrder.ASC)
                .map { farmDAOToModel(it, baseUrl) }
        }
    }

    override suspend fun getById(farmId: String): Farm? = suspendTransaction {
        FarmDAO
            .find {
                (FarmTable.id eq UUID.fromString(farmId))
            }
            .limit(1)
            .map { farmDAOToModel(it, baseUrl) }
            .firstOrNull()
    }


    override suspend fun create(farm: Farm): String = suspendTransaction {
        val farmDAO = FarmDAO.new {
            userId = UUID.fromString(farm.userId)
            title = farm.title
            description = farm.description
            cover = farm.cover
            isDone = farm.isDone
            lastVaccinationDate = farm.lastVaccinationDate
            createdAt = farm.createdAt
            updatedAt = farm.updatedAt
        }

        farmDAO.id.value.toString()
    }

    override suspend fun update(userId: String, farmId: String, newFarm: Farm): Boolean = suspendTransaction {
        val farmDAO = FarmDAO
            .find {
                (FarmTable.id eq UUID.fromString(farmId)) and
                        (FarmTable.userId eq UUID.fromString(userId))
            }
            .limit(1)
            .firstOrNull()

        if (farmDAO != null) {
            farmDAO.title = newFarm.title
            farmDAO.description = newFarm.description
            farmDAO.cover = newFarm.cover
            farmDAO.isDone = newFarm.isDone
            farmDAO.lastVaccinationDate = newFarm.lastVaccinationDate
            farmDAO.updatedAt = newFarm.updatedAt
            true
        } else {
            false
        }
    }

    override suspend fun delete(userId: String, farmId: String): Boolean = suspendTransaction {
        val rowsDeleted = FarmTable.deleteWhere {
            (FarmTable.id eq UUID.fromString(farmId)) and
                    (FarmTable.userId eq UUID.fromString(userId))
        }
        rowsDeleted >= 1
    }
}