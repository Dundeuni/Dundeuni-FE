package com.dundueni.app.data.repository

import com.dundueni.app.data.remote.api.ServerHistoryApiService
import com.dundueni.app.data.remote.api.HistoryEntryDto
import com.dundueni.app.data.remote.api.HistoryEnvelope
import com.dundueni.app.data.remote.api.AnalysisHttpException
import com.dundueni.app.data.remote.api.AnalysisProtocolException
import retrofit2.Response

internal class ServerHistoryRepository(private val api: ServerHistoryApiService) {
    suspend fun detail(analysisId: String): HistoryEntryDto {
        require(analysisId.isNotBlank())
        return api.detail(analysisId).result().also {
            if (it.id != analysisId) throw AnalysisProtocolException("History ID does not match")
        }
    }

    private fun <T> Response<HistoryEnvelope<T>>.result(): T {
        if (code() != 200) {
            val error = errorBody()?.use { body ->
                runCatching { com.google.gson.Gson().fromJson(body.string(), HistoryEnvelope::class.java) }.getOrNull()
            }
            throw AnalysisHttpException(code(), error?.code, error?.message)
        }
        val envelope = body() ?: throw AnalysisProtocolException("History response is missing")
        if (envelope.isSuccess != true || envelope.code != "HISTORY200")
            throw AnalysisProtocolException("History response is not successful")
        return envelope.result ?: throw AnalysisProtocolException("History result is missing")
    }
}
