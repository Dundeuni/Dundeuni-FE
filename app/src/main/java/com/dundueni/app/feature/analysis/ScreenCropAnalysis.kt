package com.dundueni.app.feature.analysis

import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.data.remote.api.AnalysisHttpException
import com.dundueni.app.data.remote.api.AnalysisProtocolException
import com.dundueni.app.data.remote.api.TextAnalysisResponse
import com.dundueni.app.data.repository.AnalysisExecutionStatus
import com.dundueni.app.data.repository.AnalysisFailedException
import com.dundueni.app.data.repository.AnalysisNotCompletedException
import com.dundueni.app.data.repository.AnalysisResultRepository
import com.dundueni.app.data.repository.findResultForDisplay
import retrofit2.Response

/** A screen OCR result always comes from the detail GET, never from POST metadata. */
data class ScreenCropAnalysis(val analysisId: String, val decision: String?, val detail: AnalysisResult) {
    val label: String
        get() = when (decision) {
            "normal" -> "정상 신호"
            "phishing" -> "피싱 의심"
            "defer" -> "판정 보류"
            else -> "판정 정보 없음"
        }
}

internal suspend fun Response<TextAnalysisResponse>.toCompletedScreenCrop(
    repository: AnalysisResultRepository,
): ScreenCropAnalysis {
    val id = completedTextAnalysisId()
    return repository.findResultForDisplay(id).toScreenCropAnalysis()
}

internal fun AnalysisResult.toScreenCropAnalysis(): ScreenCropAnalysis {
    if (status != "COMPLETED" || type != "TEXT" || sourceType !in setOf(null, "SCREEN_CROP")) {
        throw AnalysisProtocolException("Unexpected screen crop detail")
    }
    return ScreenCropAnalysis(requireNotNull(analysisId), textDecision, this)
}

/** POST completion authorizes one GET, but does not supply final findings. */
internal fun Response<TextAnalysisResponse>.completedTextAnalysisId(): String {
    if (code() != 202) {
        errorBody()?.close()
        throw AnalysisHttpException(code(), null, null)
    }
    val envelope = body() ?: throw AnalysisProtocolException("Text response is missing")
    if (envelope.isSuccess != true || envelope.code != "ANALYSIS202") {
        throw AnalysisProtocolException("Text response is not successful")
    }
    val result = envelope.result ?: throw AnalysisProtocolException("Text result is missing")
    val id = result.analysisId?.takeIf { it.isNotBlank() }
        ?: throw AnalysisProtocolException("Text analysis ID is missing")
    if (result.type != "TEXT") throw AnalysisProtocolException("Unexpected text type")
    when (result.status) {
        "COMPLETED" -> return id
        "QUEUED", "ANALYZING" -> throw AnalysisNotCompletedException(
            AnalysisExecutionStatus.valueOf(result.status), id, null,
        )
        "FAILED" -> throw AnalysisFailedException(null, null)
        else -> throw AnalysisProtocolException("Unknown text status")
    }
}
