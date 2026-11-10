package com.dundueni.app.data.remote.dto

/** Separate Gemini summary contract; a missing or unfinished summary is not an analysis failure. */
data class AnalysisSummaryResponseDto(
    val isSuccess: Boolean? = null,
    val code: String? = null,
    val message: String? = null,
    val result: AnalysisSummaryDto? = null,
)

data class AnalysisSummaryDto(
    val analysisId: String? = null,
    val summaryStatus: String? = null,
    val summary: String? = null,
    val summaryModel: String? = null,
    val summarizedAt: String? = null,
)
