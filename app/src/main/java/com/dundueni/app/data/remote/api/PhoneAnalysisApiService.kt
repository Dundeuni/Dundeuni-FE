package com.dundueni.app.data.remote.api

import java.util.UUID
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PhoneAnalysisApiService {
    @POST("/api/analyses/phone")
    suspend fun analyzePhone(@Body request: PhoneAnalysisRequestDto): Response<PhoneAnalysisResponseDto>
}

data class PhoneAnalysisRequestDto(val phoneNumber: String)

data class PhoneAnalysisResponseDto(
    val analysisId: String? = null,
    val status: String? = null,
) {
    fun isCompleted(): Boolean {
        val id = analysisId ?: return false
        return status == "COMPLETED" && runCatching {
            UUID.fromString(id).toString().equals(id, ignoreCase = true)
        }.getOrDefault(false)
    }
}
