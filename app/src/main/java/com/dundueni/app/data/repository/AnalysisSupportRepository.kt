package com.dundueni.app.data.repository

import com.dundueni.app.data.remote.api.AnalysisSupportApiService
import com.dundueni.app.data.remote.dto.ImageUploadLookupDto
import com.dundueni.app.data.remote.dto.RetryAnalysisDto
import com.dundueni.app.data.remote.dto.RetryAnalysisRequestDto

/** Prepared separately from the working image upload/selection and result polling. */
class AnalysisSupportRepository(private val api: AnalysisSupportApiService) {
    suspend fun retry(
        analysisId: String,
        idempotencyKey: String,
        failedElementIds: List<String>? = null,
    ): RetryAnalysisDto {
        require(analysisId.isNotBlank() && idempotencyKey.isNotBlank())
        require(failedElementIds == null || (failedElementIds.isNotEmpty() && failedElementIds.none(String::isBlank)))
        val result = api.retry(analysisId, idempotencyKey, RetryAnalysisRequestDto(failedElementIds))
            .requireResult(202, "ANALYSIS202")
        checked(result.analysisId == analysisId && !result.status.isNullOrBlank() &&
            !result.expiresAt.isNullOrBlank(), "Incomplete retry response")
        return result
    }

    suspend fun lookupUpload(uploadId: String): ImageUploadLookupDto {
        require(uploadId.isNotBlank())
        val result = api.lookupUpload(uploadId).requireResult(200, "IMAGE_UPLOAD200")
        checked(result.uploadId == uploadId &&
            result.status in setOf("AWAITING_SELECTION", "NO_CANDIDATES") &&
            result.image?.width != null && result.image.width > 0 &&
            result.image.height != null && result.image.height > 0 &&
            result.image.coordinateSpace == "NORMALIZED_IMAGE" &&
            result.original?.type == "ORIGINAL" && !result.original.elementId.isNullOrBlank() &&
            result.elements != null && !result.expiresAt.isNullOrBlank(),
            "Incomplete image upload lookup")
        return result
    }
}
