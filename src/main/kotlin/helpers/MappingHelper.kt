package org.delcom.helpers

import kotlinx.coroutines.Dispatchers
import org.delcom.dao.FarmDAO
import org.delcom.dao.RefreshTokenDAO
import org.delcom.dao.UserDAO
import org.delcom.entities.Farm
import org.delcom.entities.RefreshToken
import org.delcom.entities.User
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun <T> suspendTransaction(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block)

// ================= USER =================
fun userDAOToModel(dao: UserDAO, baseUrl: String) = User(
    id = dao.id.value.toString(),
    name = dao.name,
    username = dao.username,
    password = dao.password,
    photo = dao.photo,
    urlPhoto = buildImageUrl(baseUrl, dao.photo ?: "/uploads/defaults/user.png"),
    createdAt = dao.createdAt,
    updatedAt = dao.updatedAt
)

// ================= REFRESH TOKEN =================
fun refreshTokenDAOToModel(dao: RefreshTokenDAO) = RefreshToken(
    dao.id.value.toString(),
    dao.userId.toString(),
    dao.refreshToken,
    dao.authToken,
    dao.createdAt,
)

// ================= FARM =================
fun farmDAOToModel(dao: FarmDAO, baseUrl: String) = Farm(
    id = dao.id.value.toString(),
    userId = dao.userId.toString(),
    title = dao.title,
    description = dao.description,
    isDone = dao.isDone,
    cover = dao.cover,
    urlCover = buildImageUrl(baseUrl, dao.cover ?: "/uploads/defaults/cover.png"),
    lastVaccinationDate = dao.lastVaccinationDate,
    createdAt = dao.createdAt,
    updatedAt = dao.updatedAt
)

/**
 * Membangun URL publik gambar dari path relatif.
 */
fun buildImageUrl(baseUrl: String, pathGambar: String): String {
    val relativePath = pathGambar.removePrefix("uploads/")
    return "$baseUrl/static/$relativePath"
}