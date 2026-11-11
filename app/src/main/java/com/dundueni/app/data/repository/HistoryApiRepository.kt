package com.dundueni.app.data.repository

import com.dundueni.app.data.remote.api.HistoryApiService
import com.dundueni.app.data.remote.dto.HistoryPageDto
import com.dundueni.app.data.remote.dto.RecentHistoryDto

/** Not used by HistoryStore until the BE endpoints and recent-item contract are confirmed. */
class HistoryApiRepository(private val api: HistoryApiService) {
    suspend fun list(page: Int? = null, size: Int? = null): HistoryPageDto {
        require(page == null || page >= 0)
        require(size == null || size > 0)
        val result = api.list(page, size).requireResult(200, "HISTORY200")
        checked(result.items != null && result.page != null && result.size != null &&
            result.totalElements != null && result.totalPages != null, "Incomplete history page")
        return result
    }

    suspend fun recent(): RecentHistoryDto {
        val result = api.recent().requireResult(200, "HISTORY200")
        checked(result.items != null, "Missing recent history items")
        // Two ZIP pages disagree on item fields. Do not map them to the UI yet.
        return result
    }

    suspend fun deleteOne(analysisId: String) {
        require(analysisId.isNotBlank())
        val result = api.deleteOne(analysisId).requireResult(200, "HISTORY200")
        checked(result.deleted == true, "History was not deleted")
    }

    suspend fun deleteAll(): Int {
        val result = api.deleteAll().requireResult(200, "HISTORY200")
        checked(result.deletedCount != null && result.deletedCount >= 0, "Invalid deleted count")
        return result.deletedCount!!
    }
}
