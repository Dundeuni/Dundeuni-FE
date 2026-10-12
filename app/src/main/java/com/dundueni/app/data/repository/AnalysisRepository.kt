package com.dundueni.app.data.repository

import com.dundueni.app.data.mapper.toDomain
import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.data.remote.api.AnalysisApiService
import com.dundueni.app.data.remote.api.AnalysisHttpException
import com.dundueni.app.data.remote.api.AnalysisProtocolException
import com.dundueni.app.data.remote.dto.AnalysisErrorResponseDto
import com.dundueni.app.data.remote.dto.AnalysisResponseDto
import com.google.gson.Gson
import com.google.gson.JsonParseException
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response

/** Executes image analysis and exposes only completed BE results to the presentation flow. */
class AnalysisRepository(private val apiService: AnalysisApiService) : AnalysisResultRepository {
    suspend fun analyzeImage(image: MultipartBody.Part): Response<ResponseBody> =
        apiService.analyzeImage(image)

    /** Transport-level response for callers/tests that need to distinguish completion from pending. */
    suspend fun analyzeResponse(image: MultipartBody.Part): AnalysisRequestOutcome =
        withContext(Dispatchers.IO) {
            val response = analyzeImage(image)
            if (response.code() != HTTP_ACCEPTED) {
                val error = response.errorBody()?.use { parseErrorBody(it.string()) }
                response.body()?.close()
                throw AnalysisHttpException(response.code(), error?.code, error?.message)
            }

            val body = response.body() ?: run {
                response.errorBody()?.close()
                throw AnalysisProtocolException("Analysis response body is missing")
            }
            val envelope = body.use { parseEnvelope(it.string()) }
            if (envelope.isSuccess != true) {
                throw AnalysisProtocolException("HTTP 202 response is not marked successful")
            }
            if (envelope.code != SUCCESS_CODE) {
                throw AnalysisProtocolException("Unexpected analysis success code: ${envelope.code}")
            }
            val result = envelope.result
                ?: throw AnalysisProtocolException("Analysis response result is missing")
            when (result.status) {
                AnalysisExecutionStatus.COMPLETED.wireValue -> AnalysisRequestOutcome.Completed(envelope)
                AnalysisExecutionStatus.FAILED.wireValue -> AnalysisRequestOutcome.Failed(envelope)
                AnalysisExecutionStatus.QUEUED.wireValue -> AnalysisRequestOutcome.InProgress(
                    envelope, AnalysisExecutionStatus.QUEUED
                )
                AnalysisExecutionStatus.ANALYZING.wireValue -> AnalysisRequestOutcome.InProgress(
                    envelope, AnalysisExecutionStatus.ANALYZING
                )
                else -> throw AnalysisProtocolException(
                    "Unknown analysis status: ${result.status}", serverStatus = result.status
                )
            }
        }

    /** A pending or failed analysis is never exposed to the UI as a completed result. */
    override suspend fun analyzeResult(image: MultipartBody.Part): AnalysisResult =
        when (val outcome = analyzeResponse(image)) {
            is AnalysisRequestOutcome.Completed -> outcome.response.toDomain()
            is AnalysisRequestOutcome.Failed -> {
                val result = outcome.response.result
                throw AnalysisFailedException(result?.errorCode, outcome.response.message)
            }
            is AnalysisRequestOutcome.InProgress -> {
                val result = outcome.response.result
                throw AnalysisNotCompletedException(outcome.status, result?.analysisId, result?.errorCode)
            }
        }

    private fun parseEnvelope(json: String): AnalysisResponseDto = try {
        Gson().fromJson(json, AnalysisResponseDto::class.java)
            ?: throw AnalysisProtocolException("Analysis response is empty or null")
    } catch (error: JsonParseException) {
        throw AnalysisProtocolException("Analysis response JSON is invalid", error)
    } catch (error: IllegalStateException) {
        throw AnalysisProtocolException("Analysis response JSON has an invalid shape", error)
    }

    private fun parseErrorBody(json: String): AnalysisErrorResponseDto? = try {
        Gson().fromJson(json, AnalysisErrorResponseDto::class.java)
    } catch (_: JsonParseException) {
        null
    } catch (_: IllegalStateException) {
        null
    }

    private companion object {
        const val HTTP_ACCEPTED = 202
        const val SUCCESS_CODE = "ANALYSIS202"
    }
}

enum class AnalysisExecutionStatus(val wireValue: String) {
    QUEUED("QUEUED"),
    ANALYZING("ANALYZING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED")
}

sealed interface AnalysisRequestOutcome {
    val response: AnalysisResponseDto

    data class Completed(override val response: AnalysisResponseDto) : AnalysisRequestOutcome
    data class Failed(override val response: AnalysisResponseDto) : AnalysisRequestOutcome
    data class InProgress(
        override val response: AnalysisResponseDto,
        val status: AnalysisExecutionStatus
    ) : AnalysisRequestOutcome
}

class AnalysisFailedException(
    val errorCode: String?,
    serverMessage: String?
) : IOException(serverMessage ?: "Analysis failed")

class AnalysisNotCompletedException(
    val status: AnalysisExecutionStatus,
    val analysisId: String?,
    val errorCode: String?
) : IOException("Analysis is not completed: ${status.wireValue}")
