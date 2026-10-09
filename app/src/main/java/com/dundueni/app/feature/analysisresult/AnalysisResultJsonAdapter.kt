package com.dundueni.app.feature.analysisresult

import org.json.JSONObject

/** FE integration example, not an agreed BE API contract. Invalid verdicts must never become SAFE. */
object AnalysisResultJsonAdapter {
    fun decode(json: String): AnalysisResult {
        require(json.length <= 65_536) { "분석 결과가 너무 큽니다." }
        val value = JSONObject(json)
        val evidence = value.optJSONArray("evidence")
        return AnalysisResult(
            id = value.requiredText("id"),
            riskLevel = RiskLevel.fromWireValue(value.requiredText("riskLevel")),
            summary = value.requiredText("summary"),
            keyFinding = value.optionalText("keyFinding"),
            fraudLevel = value.optionalText("fraudLevel")?.let(RiskLevel::fromWireValue),
            fraudScore = value.optionalScore("fraudScore"),
            aiLikelihood = value.optionalText("aiLikelihood")?.let {
                AiLikelihood.valueOf(it.uppercase(java.util.Locale.ROOT))
            } ?: AiLikelihood.UNKNOWN,
            aiScore = value.optionalScore("aiScore"),
            evidence = List(evidence?.length() ?: 0) { index ->
                val item = evidence!!.getJSONObject(index)
                RiskEvidence(item.requiredText("title"), item.requiredText("description"))
            },
            sourceText = value.optionalText("sourceText"),
            sourceUrl = value.optionalText("sourceUrl"),
            targetLabel = value.optionalText("targetLabel") ?: "수신된 문자 메시지 및 첨부 링크",
        )
    }

    private fun JSONObject.requiredText(key: String): String {
        val value = get(key)
        require(value is String && value.isNotBlank()) { "$key 값이 필요합니다." }
        return value
    }

    private fun JSONObject.optionalText(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = get(key)
        require(value is String) { "$key 값은 문자열이어야 합니다." }
        return value.takeIf { it.isNotBlank() }
    }

    private fun JSONObject.optionalScore(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val value = get(key)
        require(value is Number) { "$key 값은 0~100의 숫자여야 합니다." }
        return value.toDouble().also {
            require(it.isFinite() && it in 0.0..100.0) { "$key 범위를 확인하세요." }
        }
    }
}
