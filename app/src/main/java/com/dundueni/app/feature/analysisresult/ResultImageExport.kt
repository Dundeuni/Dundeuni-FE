package com.dundueni.app.feature.analysisresult

import com.dundueni.app.data.model.ImageAnalysisSummary
import com.dundueni.app.data.model.ImageModelDetails
import com.dundueni.app.data.repository.ImageSummaryResult
import com.dundueni.app.feature.analysis.ScreenResultType
import com.dundueni.app.feature.history.HistoryType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun AnalysisResult.waitsForImageSummary(
    state: ImageSummaryUiState?,
    screenResultType: ScreenResultType?,
): Boolean {
    val showsImage = historyType == HistoryType.IMAGE ||
        historyType == HistoryType.SCREEN && screenResultType != ScreenResultType.TEXT
    return showsImage && if (state == null) {
        initialImageSummary() == ImageSummaryResult.Pending
    } else {
        state.checking && (state.result == ImageSummaryResult.Pending || state.result == ImageSummaryResult.LookupError)
    }
}

internal fun AnalysisResult.toImageExport(state: ImageSummaryUiState?): AnalysisResult {
    val summaryResult = if (state == null) initialImageSummary() else state.result
    val completed = summaryResult as? ImageSummaryResult.Completed
    val originalValues = buildList {
        addAll(listOfNotNull(sourceText, sourceUrl))
        if (historyType == HistoryType.PHONE) add(targetLabel)
        extractedPhoneNumbers.forEach { phone ->
            add(phone.phoneNumber)
            add(phone.displayNumber)
        }
    }.filter(String::isNotBlank).distinct().sortedByDescending(String::length)

    fun redact(value: String): String {
        val withoutOriginals = originalValues.fold(value) { text, original -> text.replace(original, REDACTED) }
        return phoneNumber.replace(webAddressOrEmail.replace(withoutOriginals, REDACTED), REDACTED)
    }

    fun RiskEvidence.redacted() = copy(title = redact(title), description = redact(description))
    fun ImageModelDetails.redacted() = copy(reasons = reasons.map(::redact), limitations = emptyList())
    val summaryAtClick = completed?.details?.let { stored ->
        stored.copy(
            summaryStatus = "COMPLETED",
            summary = stored.summary?.let(::redact),
            detailedReasons = stored.detailedReasons.map { it.copy(title = redact(it.title), description = redact(it.description)) },
            actionGuides = stored.actionGuides.map { it.copy(title = redact(it.title), description = redact(it.description)) },
            reassurance = stored.reassurance?.let(::redact),
            summaryModel = null,
        )
    }

    return copy(
        summary = redact(summary),
        textSummary = textSummary?.let(::redact),
        keyFinding = keyFinding?.let(::redact),
        guidance = guidance.map(::redact),
        evidence = evidence.map { it.redacted() },
        screenTextEvidence = screenTextEvidence.map { it.redacted() },
        textLimitations = textLimitations.map(::redact),
        imageGenerationDetails = imageGenerationDetails?.redacted(),
        imageManipulationDetails = imageManipulationDetails?.redacted(),
        imageOverallDecisionLabel = imageOverallDecisionLabel?.let(::redact),
        imageSummary = summaryAtClick ?: imageSummary?.let { ImageAnalysisSummary() },
        sourceText = null,
        sourceUrl = null,
        targetLabel = "",
        extractedPhoneNumbers = emptyList(),
        aiRiskLabel = null,
        textReasonCodes = null,
    )
}

internal fun formatImageInspectionTime(value: String?, zone: ZoneId = ZoneId.systemDefault()): String? =
    value?.let { runCatching { dateFormat.format(Instant.parse(it).atZone(zone)) }.getOrNull() }

private const val REDACTED = "[가림]"
private val dateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
private val phoneNumber = Regex(
    """(?<!\d)(?:(?:\+82[ .-]?(?:0[ .-]?)?|\(?0)(?:1[016-9]|2|[3-6][1-5]|50\d?|60|70|80)\)?[ .-]?\d{3,4}[ .-]?\d{4}|1[568]\d{2}[ .-]?\d{4})(?!\d)""",
)
private val webAddressOrEmail = Regex(
    """(?i)(?:https?://|www\.)[^\s<>"]+|[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}""",
)
