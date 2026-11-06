package com.dundueni.app.feature.analysis

import com.dundueni.app.data.remote.api.AnalysisHttpException
import com.dundueni.app.data.remote.api.AnalysisProtocolException
import com.dundueni.app.data.remote.api.TextAnalysisResponse
import retrofit2.Response

/** Only a completed BE OCR decision is eligible for the screen result. */
data class ScreenCropAnalysis(val analysisId: String, val decision: String) {
    val label: String
        get() = when (decision) {
            "normal" -> "정상 신호"
            "phishing" -> "피싱 의심"
            "defer" -> "판정 보류"
            else -> error("Unsupported screen crop decision")
        }
}

internal fun Response<TextAnalysisResponse>.toCompletedScreenCrop(): ScreenCropAnalysis {
    if (code() != 202) throw AnalysisHttpException(code(), null, null)
    val envelope = body() ?: throw AnalysisProtocolException("Screen crop response is missing")
    if (envelope.isSuccess != true || envelope.code != "ANALYSIS202") {
        throw AnalysisProtocolException("Screen crop response is not successful")
    }
    val result = envelope.result ?: throw AnalysisProtocolException("Screen crop result is missing")
    if (result.type != "TEXT" || result.status != "COMPLETED") {
        throw AnalysisProtocolException("Screen crop analysis is not completed")
    }
    val id = result.analysisId?.takeIf { it.isNotBlank() }
        ?: throw AnalysisProtocolException("Screen crop analysis ID is missing")
    val textResult = result.textResult
        ?: throw AnalysisProtocolException("Screen crop text result is missing")
    if (textResult.sourceType != null && textResult.sourceType != "SCREEN_CROP") {
        throw AnalysisProtocolException("Unexpected text source type")
    }
    val decision = textResult.decision
    if (decision !in setOf("normal", "phishing", "defer")) {
        throw AnalysisProtocolException("Unknown screen crop decision")
    }
    return ScreenCropAnalysis(id, decision!!)
}
