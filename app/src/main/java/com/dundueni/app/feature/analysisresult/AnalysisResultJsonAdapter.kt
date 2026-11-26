package com.dundueni.app.feature.analysisresult

import com.dundueni.app.data.model.ExtractedPhoneNumber
import com.dundueni.app.data.model.ImageActionGuide
import com.dundueni.app.data.model.ImageAnalysisSummary
import com.dundueni.app.data.model.ImageDetailedReason
import com.dundueni.app.data.model.ImageModelDetails
import org.json.JSONObject
import com.dundueni.app.feature.history.HistoryType

/** FE integration example, not an agreed BE API contract. Invalid verdicts must never become SAFE. */
object AnalysisResultJsonAdapter {
    fun decode(json: String): AnalysisResult {
        val value = JSONObject(json)
        val historyType = HistoryType.entries.firstOrNull { it.name == value.optionalText("historyType") }
            ?: HistoryType.UNKNOWN
        val riskLevel = value.requiredText("riskLevel")
        return AnalysisResult(
            id = value.requiredText("id"),
            historyType = historyType,
            riskLevel = if (riskLevel == "UNKNOWN") RiskLevel.UNKNOWN else RiskLevel.fromWireValue(riskLevel),
            summary = value.optionalText("summary").orEmpty(),
            keyFinding = value.optionalText("keyFinding"),
            guidance = value.optJSONArray("guidance")?.let { items ->
                List(items.length()) { items.getString(it) }.filter { it.isNotBlank() }
            }.orEmpty(),
            aiRiskLabel = value.optionalText("aiRiskLabel"),
            fraudLevel = value.optionalText("fraudLevel")?.let(RiskLevel::fromWireValue),
            fraudScore = value.optionalScore("fraudScore"),
            aiLikelihood = value.optionalText("aiLikelihood")?.let {
                AiLikelihood.valueOf(it.uppercase(java.util.Locale.ROOT))
            } ?: AiLikelihood.UNKNOWN,
            aiScore = value.optionalScore("aiScore"),
            evidence = value.evidence("evidence"),
            sourceText = value.optionalText("sourceText"),
            sourceUrl = value.optionalText("sourceUrl"),
            targetLabel = value.optionalText("targetLabel")
                ?: if (historyType == HistoryType.PHONE) "" else "수신된 문자 메시지 및 첨부 링크",
            imageModelResult = value.optBoolean("imageModelResult", false),
            imageOverallDecisionLabel = value.optionalText("imageOverallDecisionLabel"),
            imageGenerationDecision = value.optionalText("imageGenerationDecision"),
            imageGenerationDisplayScore = value.optionalScore("imageGenerationDisplayScore"),
            imageManipulationDecision = value.optionalText("imageManipulationDecision"),
            imageManipulationDisplayScore = value.optionalScore("imageManipulationDisplayScore"),
            textDecision = value.optionalText("textDecision"),
            textRiskScore = value.optionalUnitScore("textRiskScore"),
            textSourceType = value.optionalText("textSourceType"),
            textNeedsReview = value.optionalBoolean("textNeedsReview"),
            textReasonCodes = value.optJSONArray("textReasonCodes")?.let { items ->
                List(items.length()) { items.getString(it) }
            },
            textOcrUsed = value.optionalBoolean("textOcrUsed"),
            textLimitations = value.optJSONArray("textLimitations")?.let { items ->
                List(items.length()) { items.getString(it) }.filter { it.isNotBlank() }
            }.orEmpty(),
            imageGenerationDetails = value.modelDetails("imageGenerationDetails"),
            imageManipulationDetails = value.modelDetails("imageManipulationDetails"),
            screenTextEvidence = value.evidence("screenTextEvidence"),
            imageOverallDecision = value.optionalText("imageOverallDecision"),
            imageSummary = value.optJSONObject("imageSummary")?.toImageSummary(),
            analyzedAt = value.optionalText("analyzedAt"),
            textSummary = value.optionalText("textSummary"),
            extractedPhoneNumbers = value.optJSONArray("extractedPhoneNumbers")?.let { phones ->
                List(phones.length()) { index ->
                    val phone = phones.getJSONObject(index)
                    ExtractedPhoneNumber(
                        phone.getString("phoneNumber"),
                        phone.getString("displayNumber"),
                        phone.getString("source"),
                    )
                }
            }.orEmpty(),
        )
    }

    private fun JSONObject.toImageSummary() = ImageAnalysisSummary(
        summaryStatus = optionalString("summaryStatus"),
        summary = optionalString("summary"),
        detailedReasons = optJSONArray("detailedReasons")?.let { items ->
            List(items.length()) { index ->
                val item = items.getJSONObject(index)
                ImageDetailedReason(
                    item.getString("type"),
                    item.getString("title"),
                    item.getString("description"),
                )
            }
        }.orEmpty(),
        actionGuides = optJSONArray("actionGuides")?.let { items ->
            List(items.length()) { index ->
                val item = items.getJSONObject(index)
                ImageActionGuide(item.getString("title"), item.getString("description"))
            }
        }.orEmpty(),
        reassurance = optionalString("reassurance"),
        summaryModel = optionalString("summaryModel"),
        summarizedAt = optionalString("summarizedAt"),
    )

    private fun JSONObject.evidence(key: String): List<RiskEvidence> =
        optJSONArray(key)?.let { items ->
            List(items.length()) { index ->
                val item = items.getJSONObject(index)
                RiskEvidence(item.requiredText("title"), item.requiredText("description"))
            }
        }.orEmpty()

    private fun JSONObject.modelDetails(key: String): ImageModelDetails? =
        optJSONObject(key)?.let { model ->
            ImageModelDetails(
                status = model.optionalText("status"),
                reasons = model.strings("reasons"),
                limitations = model.strings("limitations"),
            )
        }

    private fun JSONObject.strings(key: String): List<String> =
        optJSONArray(key)?.let { items ->
            List(items.length()) { items.getString(it) }.filter(String::isNotBlank)
        }.orEmpty()

    private fun JSONObject.requiredText(key: String): String {
        val value = get(key)
        require(value is String && value.isNotBlank()) { "$key 값이 필요합니다." }
        return value
    }

    private fun JSONObject.optionalText(key: String): String? =
        optionalString(key)?.takeIf { it.isNotBlank() }

    private fun JSONObject.optionalString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = get(key)
        require(value is String) { "$key 값은 문자열이어야 합니다." }
        return value
    }

    private fun JSONObject.optionalScore(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val value = get(key)
        require(value is Number) { "$key 값은 0~100의 숫자여야 합니다." }
        return value.toDouble().also {
            require(it.isFinite() && it in 0.0..100.0) { "$key 범위를 확인하세요." }
        }
    }

    private fun JSONObject.optionalUnitScore(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val value = get(key)
        require(value is Number) { "$key must be a number" }
        return value.toDouble().also { require(it.isFinite() && it in 0.0..1.0) { "$key must be between 0 and 1" } }
    }

    private fun JSONObject.optionalBoolean(key: String): Boolean? {
        if (!has(key) || isNull(key)) return null
        return (get(key) as? Boolean) ?: throw IllegalArgumentException("$key must be a boolean")
    }
}
