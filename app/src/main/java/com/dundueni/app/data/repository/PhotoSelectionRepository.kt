package com.dundueni.app.data.repository

import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.data.remote.api.AnalysisHttpException
import com.dundueni.app.data.remote.api.AnalysisProtocolException
import com.dundueni.app.data.remote.api.PhotoSelectionApiService
import com.dundueni.app.data.remote.api.PhotoSelectionRequest
import com.dundueni.app.data.remote.api.PhotoUpload
import com.dundueni.app.data.remote.dto.AnalysisErrorResponseDto
import com.dundueni.app.data.remote.dto.AnalysisResponseDto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import retrofit2.Response

/** The selected upload uses the existing detail and summary lookup after POST returns an ID. */
class PhotoSelectionRepository(
    private val api: PhotoSelectionApiService,
    private val results: AnalysisResultRepository,
) {
    private val gson = Gson()

    suspend fun upload(file: MultipartBody.Part): PhotoUpload = withContext(Dispatchers.IO) {
        val response = api.upload(file)
        if (response.code() != 201) throw response.failure()
        val envelope = response.body()
        if (envelope?.isSuccess != true || envelope.code != "IMAGE_UPLOAD201")
            throw AnalysisProtocolException("Unexpected image upload response")
        val result = envelope.result ?: throw AnalysisProtocolException("Upload result is missing")
        val image = result.image ?: throw AnalysisProtocolException("Image dimensions are missing")
        val original = result.original ?: throw AnalysisProtocolException("Original choice is missing")
        if (result.uploadId.isNullOrBlank() || image.width <= 0 || image.height <= 0 ||
            image.coordinateSpace != "NORMALIZED_IMAGE" || original.type != "ORIGINAL" ||
            original.elementId.isNullOrBlank() || result.elements == null ||
            result.status !in setOf("AWAITING_SELECTION", "NO_CANDIDATES") ||
            result.elements.any { it.type != "PHOTO_REGION" || it.elementId.isNullOrBlank() ||
                it.bbox == null || it.bbox.width <= 0 || it.bbox.height <= 0 ||
                it.bbox.left < 0 || it.bbox.top < 0 ||
                it.bbox.left.toLong() + it.bbox.width > image.width ||
                it.bbox.top.toLong() + it.bbox.height > image.height }
        ) throw AnalysisProtocolException("Invalid image upload choices")
        result
    }

    suspend fun select(uploadId: String, elementId: String): AnalysisResult = withContext(Dispatchers.IO) {
        val response = api.select(uploadId, PhotoSelectionRequest(elementId))
        if (response.code() != 202) throw response.failure()
        val raw = response.body()?.use { it.string() }
            ?: throw AnalysisProtocolException("Selected analysis response is missing")
        val envelope = try { gson.fromJson(raw, AnalysisResponseDto::class.java) }
            catch (error: RuntimeException) { throw AnalysisProtocolException("Invalid selected analysis response", error) }
        if (envelope?.isSuccess != true || envelope.code != "ANALYSIS202")
            throw AnalysisProtocolException("Unexpected selected analysis response")
        val result = envelope.result ?: throw AnalysisProtocolException("Selected analysis result is missing")
        val id = result.analysisId?.takeIf { it.isNotBlank() }
            ?: throw AnalysisProtocolException("Selected analysis ID is missing")
        if (result.status == "FAILED") throw AnalysisFailedException(result.errorCode, envelope.message)
        if (result.status !in setOf("QUEUED", "ANALYZING", "COMPLETED"))
            throw AnalysisProtocolException("Unknown selected analysis status")
        results.findResultForDisplay(id)
    }

    suspend fun cancel(uploadId: String) = withContext(Dispatchers.IO) {
        val response = api.cancel(uploadId)
        if (response.code() != 200) {
            response.body()?.close()
            throw response.failure()
        }
        response.errorBody()?.close()
        val raw = response.body()?.use { it.string() }
            ?: throw AnalysisProtocolException("Image upload cancellation response is missing")
        val envelope = try { gson.fromJson(raw, AnalysisErrorResponseDto::class.java) }
            catch (error: RuntimeException) { throw AnalysisProtocolException("Invalid image upload cancellation response", error) }
        if (envelope?.isSuccess != true || envelope.code != "IMAGE_UPLOAD200" || envelope.result != null)
            throw AnalysisProtocolException("Unexpected image upload cancellation response")
    }

    private fun <T> Response<T>.failure(): AnalysisHttpException {
        val error = try {
            errorBody()?.use { gson.fromJson(it.string(), AnalysisErrorResponseDto::class.java) }
        } catch (_: RuntimeException) { null }
        return AnalysisHttpException(code(), error?.code, error?.message)
    }
}
