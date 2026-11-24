package com.dundueni.app.data.repository

import com.dundueni.app.data.remote.api.HistoryApiService
import com.dundueni.app.data.remote.dto.HistoryDetailDto
import com.dundueni.app.data.remote.dto.HistoryPageItemDto

data class HistoryPage(
    val items: List<HistoryPageItemDto>,
    val hasNext: Boolean,
)

class HistoryApiRepository(private val api: HistoryApiService) {
    suspend fun list(page: Int = 1, limit: Int = 20): HistoryPage {
        require(page >= 1 && limit in 1..100)
        val offset = (page - 1L) * limit
        require(offset <= Int.MAX_VALUE)
        val result = api.list(page, limit).requireResult(200, "ANALYSIS200")
        val items = result.items ?: throw PreparedApiProtocolException("Missing history items")
        val hasNext = result.hasNext ?: throw PreparedApiProtocolException("Missing history pagination")
        val total = result.total ?: throw PreparedApiProtocolException("Missing history total")
        checked(
            result.page == page && result.limit == limit && total >= 0 &&
                items.size <= limit && hasNext == (offset + items.size < total) &&
                (!hasNext || items.isNotEmpty()),
            "Invalid history pagination",
        )
        return HistoryPage(items, hasNext)
    }

    suspend fun detail(analysisId: String): HistoryDetailDto {
        require(analysisId.isNotBlank())
        val result = api.detail(analysisId).requireResult(200, "ANALYSIS200")
        checked(result.analysisId == analysisId, "History ID does not match")
        return result
    }

    suspend fun deleteOne(analysisId: String) {
        require(analysisId.isNotBlank())
        val result = api.deleteOne(analysisId).requireResult(200, "ANALYSIS200")
        checked(result.deletedCount == 1, "History was not deleted")
    }

    suspend fun deleteAll(): Int {
        val result = api.deleteAll().requireResult(200, "ANALYSIS200")
        val count = result.deletedCount ?: throw PreparedApiProtocolException("Missing deleted count")
        checked(count >= 0, "Invalid deleted count")
        return count
    }
}
