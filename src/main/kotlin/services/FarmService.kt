package org.delcom.services

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.cio.*
import io.ktor.utils.io.copyAndClose
import org.delcom.data.AppException
import org.delcom.data.DataResponse
import org.delcom.data.FarmRequest
import org.delcom.helpers.ServiceHelper
import org.delcom.helpers.ValidatorHelper
import org.delcom.repositories.IFarmRepository
import org.delcom.repositories.IUserRepository
import java.io.File
import java.util.UUID

class FarmService(
    private val userRepo: IUserRepository,
    private val farmRepo: IFarmRepository
) {
    // Mengambil semua daftar farm saya (dengan pagination & filter)
    suspend fun getAll(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepo)

        val search = call.request.queryParameters["search"] ?: ""
        val filter = call.request.queryParameters["filter"] ?: "all"   // all | done | not_done
        val page   = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val limit  = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 10

        val (farms, totalCount) = farmRepo.getAll(user.id, search, filter, page, limit)

        val totalPages = if (totalCount == 0) 1 else Math.ceil(totalCount.toDouble() / limit).toInt()
        val hasNextPage = page < totalPages

        val response = DataResponse(
            "success",
            "Berhasil mengambil daftar farm saya",
            mapOf(
                "farms"      to farms,
                "meta"       to mapOf(
                    "page"        to page,
                    "limit"       to limit,
                    "totalCount"  to totalCount,
                    "totalPages"  to totalPages,
                    "hasNextPage" to hasNextPage
                )
            )
        )
        call.respond(response)
    }

    // Mengambil data farm saya berdasarkan id
    suspend fun getById(call: ApplicationCall) {
        val farmId = call.parameters["id"]
            ?: throw AppException(400, "Data farm tidak valid!")

        val user = ServiceHelper.getAuthUser(call, userRepo)

        val farm = farmRepo.getById(farmId)
        if (farm == null || farm.userId != user.id) {
            throw AppException(404, "Data farm tidak tersedia!")
        }

        val response = DataResponse(
            "success",
            "Berhasil mengambil data farm",
            mapOf(Pair("farm", farm))
        )
        call.respond(response)
    }

    // Ubah cover farm
    suspend fun putCover(call: ApplicationCall) {
        val farmId = call.parameters["id"]
            ?: throw AppException(400, "Data farm tidak valid!")

        val user = ServiceHelper.getAuthUser(call, userRepo)

        val request = FarmRequest()
        request.userId = user.id

        val multipartData = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 5)
        multipartData.forEachPart { part ->
            when (part) {
                is PartData.FileItem -> {
                    val ext = part.originalFileName
                        ?.substringAfterLast('.', "")
                        ?.let { if (it.isNotEmpty()) ".$it" else "" }
                        ?: ""

                    val fileName = UUID.randomUUID().toString() + ext
                    val filePath = "uploads/farms/$fileName"

                    val file = File(filePath)
                    file.parentFile.mkdirs()

                    part.provider().copyAndClose(file.writeChannel())
                    request.cover = filePath
                }

                else -> {}
            }

            part.dispose()
        }

        if (request.cover == null) {
            throw AppException(404, "Cover farm tidak tersedia!")
        }

        val newFile = File(request.cover!!)
        if (!newFile.exists()) {
            throw AppException(404, "Cover farm gagal diunggah!")
        }

        val oldFarm = farmRepo.getById(farmId)
        if (oldFarm == null || oldFarm.userId != user.id) {
            throw AppException(404, "Data farm tidak tersedia!")
        }

        request.title = oldFarm.title
        request.description = oldFarm.description
        request.isDone = oldFarm.isDone
        request.lastVaccinationDate = oldFarm.lastVaccinationDate

        val isUpdated = farmRepo.update(user.id, farmId, request.toEntity())
        if (!isUpdated) {
            throw AppException(400, "Gagal memperbarui cover farm!")
        }

        if (oldFarm.cover != null) {
            val oldFile = File(oldFarm.cover!!)
            if (oldFile.exists()) oldFile.delete()
        }

        val response = DataResponse("success", "Berhasil mengubah cover farm", null)
        call.respond(response)
    }

    // Menambahkan data farm
    suspend fun post(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepo)

        val request = call.receive<FarmRequest>()
        request.userId = user.id

        val validator = ValidatorHelper(request.toMap())
        validator.required("title", "Judul farm tidak boleh kosong")
        validator.required("description", "Deskripsi farm tidak boleh kosong")
        validator.validate()

        val farmId = farmRepo.create(request.toEntity())

        val response = DataResponse(
            "success",
            "Berhasil menambahkan data farm",
            mapOf(Pair("farmId", farmId))
        )
        call.respond(response)
    }

    // Mengubah data farm
    suspend fun put(call: ApplicationCall) {
        val farmId = call.parameters["id"]
            ?: throw AppException(400, "Data farm tidak valid!")

        val user = ServiceHelper.getAuthUser(call, userRepo)

        val request = call.receive<FarmRequest>()
        request.userId = user.id

        val validator = ValidatorHelper(request.toMap())
        validator.required("title", "Judul farm tidak boleh kosong")
        validator.required("description", "Deskripsi farm tidak boleh kosong")
        validator.required("isDone", "Status selesai tidak boleh kosong")
        validator.validate()

        val oldFarm = farmRepo.getById(farmId)
        if (oldFarm == null || oldFarm.userId != user.id) {
            throw AppException(404, "Data farm tidak tersedia!")
        }
        request.cover = oldFarm.cover

        val isUpdated = farmRepo.update(user.id, farmId, request.toEntity())
        if (!isUpdated) {
            throw AppException(400, "Gagal memperbarui data farm!")
        }

        val response = DataResponse("success", "Berhasil mengubah data farm", null)
        call.respond(response)
    }

    // Menghapus data farm
    suspend fun delete(call: ApplicationCall) {
        val farmId = call.parameters["id"]
            ?: throw AppException(400, "Data farm tidak valid!")

        val user = ServiceHelper.getAuthUser(call, userRepo)

        val oldFarm = farmRepo.getById(farmId)
        if (oldFarm == null || oldFarm.userId != user.id) {
            throw AppException(404, "Data farm tidak tersedia!")
        }

        val isDeleted = farmRepo.delete(user.id, farmId)
        if (!isDeleted) {
            throw AppException(400, "Gagal menghapus data farm!")
        }

        if (oldFarm.cover != null) {
            val oldFile = File(oldFarm.cover!!)
            if (oldFile.exists()) oldFile.delete()
        }

        val response = DataResponse("success", "Berhasil menghapus data farm", null)
        call.respond(response)
    }

    // Mengambil gambar cover farm
    suspend fun getCover(call: ApplicationCall) {
        val farmId = call.parameters["id"]
            ?: throw AppException(400, "Data farm tidak valid!")

        val farm = farmRepo.getById(farmId)
            ?: return call.respond(HttpStatusCode.NotFound)

        if (farm.cover == null) {
            throw AppException(404, "Farm belum memiliki cover")
        }

        val file = File(farm.cover!!)
        if (!file.exists()) {
            throw AppException(404, "Cover farm tidak tersedia")
        }

        call.respondFile(file)
    }
}
