package org.delcom.repositories

import org.delcom.entities.Farm

interface IFarmRepository {
    suspend fun getAll(
        userId: String,
        search: String,
        filter: String,   // "all" | "done" | "not_done"
        page: Int,
        limit: Int
    ): Pair<List<Farm>, Int> // Pair<data, totalCount>

    suspend fun getById(farmId: String): Farm?
    suspend fun create(farm: Farm): String
    suspend fun update(userId: String, farmId: String, newFarm: Farm): Boolean
    suspend fun delete(userId: String, farmId: String): Boolean
}