package com.dundueni.app.feature.history

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dundueni.app.data.remote.dto.HistoryDetailDto
import com.dundueni.app.data.remote.dto.HistoryPageItemDto
import com.dundueni.app.data.repository.ImageSummaryResult
import com.google.gson.Gson
import com.dundueni.app.feature.analysisresult.RiskLevel
import java.time.Instant
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HistoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preferences = context.getSharedPreferences("analysis_result_history", Context.MODE_PRIVATE)
    private val store = HistoryStore(context)

    @Before fun reset() { preferences.edit().clear().commit() }

    @Test fun textHistoryUsesStoredPreviewAndClassifierFields() {
        val detail = Gson().fromJson("""{
            "analysisId":"history-id","type":"TEXT","status":"COMPLETED","createdAt":"2026-11-11T00:00:00Z",
            "detail":{"textPreview":"stored preview","decision":"phishing","riskScore":0.87,"needsReview":false}
        }""", HistoryDetailDto::class.java).toRecordDetail()
        val result = requireNotNull(detail.result)

        assertEquals("history-id", detail.record.id)
        assertEquals(HistoryType.TEXT, detail.record.type)
        assertEquals(RiskLevel.CAUTION, result.riskLevel)
        assertEquals("stored preview", result.sourceText)
        assertEquals(0.87, result.textRiskScore!!, 0.0)
        assertEquals(false, result.textNeedsReview)
        assertNull(result.textSourceType)
        assertNull(result.textOcrUsed)
    }

    @Test fun imageHistoryKeepsAiRiskSeparateAndReadsStoredSummary() {
        val payload = """{
            "analysisId":"image-id","type":"IMAGE","status":"COMPLETED","createdAt":"2026-11-11T00:00:00Z",
            "aiRiskLevel":"CAUTION","aiGenerationScore":82,
            "reasons":[{"description":"stored evidence"}],
            "detail":{"summary":"stored summary","summaryStatus":"COMPLETED"}
        }"""
        val detail = Gson().fromJson(payload, HistoryDetailDto::class.java).toRecordDetail()
        val result = requireNotNull(detail.result)
        assertEquals(RiskLevel.UNKNOWN, result.riskLevel)
        assertEquals("CAUTION", result.aiRiskLabel)
        assertEquals(82.0, result.aiScore!!, 0.0)
        assertEquals("stored evidence", result.evidence.single().description)
        assertEquals("stored summary", result.summary)
        assertEquals("판단 불가", result.imageOverallDecisionLabel)
        assertNull(result.imageOverallDecision)
        assertNull(detail.expiresAt)
        assertNull(detail.completedAt)

        val pending = Gson().fromJson(
            payload.replace("\"summaryStatus\":\"COMPLETED\"", "\"summaryStatus\":\"PENDING\""),
            HistoryDetailDto::class.java,
        ).toRecordDetail()
        assertEquals("", pending.result?.summary)
        assertEquals(ImageSummaryResult.Pending, pending.imageSummaryStatus)

        val withoutTypeDetail = Gson().fromJson(payload, HistoryDetailDto::class.java)
            .copy(detail = null).toRecordDetail()
        assertEquals(82.0, requireNotNull(withoutTypeDetail.result?.aiScore), 0.0)

        val screen = Gson().fromJson(payload, HistoryDetailDto::class.java)
            .copy(type = "SCREEN").toRecordDetail()
        assertEquals(HistoryType.SCREEN, screen.result?.historyType)
    }

    @Test fun imageDetailUsesStoredExpiryAndMapsModelsByKindRegardlessOfOrder() {
        val dto = Gson().fromJson(
            """{
                "analysisId":"image-id","type":"IMAGE","status":"COMPLETED",
                "createdAt":"2026-11-25T07:00:00Z","completedAt":"2026-11-25T07:00:30Z",
                "expiresAt":"2026-11-27T16:00:00+09:00","reasons":[{"description":"stored record evidence"}],
                "detail":{"imageResultStatus":"COMPLETED","summaryStatus":"COMPLETED","summary":"stored summary",
                    "summaryModel":"gemini","summarizedAt":"2026-11-25T07:00:35Z","modelResults":[
                        {"kind":"MANIPULATION","status":"SUCCEEDED","displayScore":84,
                            "decision":"MANIPULATION_SUSPECTED","reasons":[{"description":"stored edit evidence"}],
                            "limitations":["stored limit"],"modelName":"TruFor","modelVersion":"v1","policyVersion":"internal"},
                        {"kind":"GENERATION","status":"SUCCEEDED","displayScore":12,"decision":"NO_CLEAR_GENERATION_SIGNAL"}]}}
            """,
            HistoryDetailDto::class.java,
        )
        val stored = requireNotNull(dto.detail)
        val models = requireNotNull(stored.modelResults)
        val detail = dto.toRecordDetail()
        val result = requireNotNull(detail.result)

        assertEquals(detail, dto.copy(detail = stored.copy(modelResults = models.reversed())).toRecordDetail())
        assertEquals(Instant.parse("2026-11-27T07:00:00Z"), detail.expiresAt)
        assertEquals(Instant.parse("2026-11-25T07:00:30Z"), detail.completedAt)
        assertEquals("COMPLETED", detail.imageResultStatus)
        assertEquals("GENERATION_OR_MANIPULATION_SUSPECTED", result.imageOverallDecision)
        assertEquals("NO_CLEAR_GENERATION_SIGNAL", result.imageGenerationDecision)
        assertEquals(12.0, result.imageGenerationDisplayScore!!, 0.0)
        assertEquals("SUCCEEDED", result.imageManipulationDetails?.status)
        assertEquals("MANIPULATION_SUSPECTED", result.imageManipulationDecision)
        assertEquals(84.0, result.imageManipulationDisplayScore!!, 0.0)
        assertEquals(listOf("stored edit evidence"), result.imageManipulationDetails?.reasons)
        assertEquals(listOf("stored limit"), result.imageManipulationDetails?.limitations)
        assertEquals("stored record evidence", result.evidence.single().description)
        assertEquals("TruFor", models.first()?.modelName)
        val summary = (detail.imageSummaryStatus as ImageSummaryResult.Completed).details
        assertEquals("stored summary", summary.summary)
        assertEquals("gemini", summary.summaryModel)
        assertEquals("2026-11-25T07:00:35Z", summary.summarizedAt)
        assertTrue(summary.detailedReasons.isEmpty())
        assertTrue(summary.actionGuides.isEmpty())
        assertNull(summary.reassurance)
    }

    @Test fun imageHistoryHandlesPartialFailureMissingModelsAndSummaryStates() {
        val dto = Gson().fromJson(
            """{
                "analysisId":"image-id","type":"IMAGE","status":"COMPLETED","createdAt":"2026-11-25T07:00:00Z",
                "aiGenerationScore":82,"detail":{"imageResultStatus":"PARTIAL_FAILED","summaryStatus":"FAILED",
                    "modelResults":[
                      {"kind":"MANIPULATION","status":"FAILED","displayScore":null,"decision":null,
                          "errorCode":"MODEL_ERROR","retryable":true},
                      {"kind":"GENERATION","status":"SUCCEEDED","displayScore":null,"decision":"NO_CLEAR_GENERATION_SIGNAL"}]}}
            """,
            HistoryDetailDto::class.java,
        )
        val stored = requireNotNull(dto.detail)
        val models = requireNotNull(stored.modelResults).filterNotNull()
        val history = dto.toRecordDetail()
        val result = requireNotNull(history.result)
        assertEquals("PARTIAL_FAILED", history.imageResultStatus)
        assertEquals("INCONCLUSIVE", result.imageOverallDecision)
        assertNull(result.aiScore)
        assertNull(result.imageGenerationDisplayScore)
        assertNull(result.imageManipulationDisplayScore)
        assertEquals("FAILED", result.imageManipulationDetails?.status)
        assertEquals(ImageSummaryResult.Failed, history.imageSummaryStatus)
        assertTrue(result.summary.isEmpty())

        val missing = dto.copy(detail = stored.copy(summaryStatus = null, modelResults = models.drop(1))).toRecordDetail()
        assertEquals("INCONCLUSIVE", missing.result?.imageOverallDecision)
        assertNull(missing.result?.imageManipulationDetails)
        assertEquals(ImageSummaryResult.Unavailable, missing.imageSummaryStatus)

        val success = models.first().copy(status = "SUCCEEDED", decision = "NO_CLEAR_MANIPULATION_SIGNAL")
        val clear = dto.copy(detail = stored.copy(modelResults = listOf(success, models.last()))).toRecordDetail()
        assertEquals("NO_CLEAR_SIGNAL", clear.result?.imageOverallDecision)
        val suspected = models.last().copy(decision = "AI_GENERATED_SUSPECTED")
        val partial = dto.copy(detail = stored.copy(modelResults = listOf(models.first(), suspected))).toRecordDetail()
        assertEquals("GENERATION_OR_MANIPULATION_SUSPECTED", partial.result?.imageOverallDecision)
        assertNull(dto.copy(status = "FAILED").toRecordDetail().result)
        assertNull(dto.copy(detail = stored.copy(imageResultStatus = "FAILED")).toRecordDetail().result)
    }

    @Test fun phoneHistoryUsesMaskedNumberAndPreservesRangeAndMissingCount() {
        val listItem = Gson().fromJson("""{
            "analysisId":"phone-id","type":"PHONE","status":"COMPLETED","targetSummary":"010********",
            "aiRiskLevel":"DANGER","createdAt":"2026-11-11T00:00:00Z"
        }""", HistoryPageItemDto::class.java).toHistoryRecord()
        assertEquals("010********", listItem.preview)
        assertEquals(RiskLevel.DANGER, listItem.riskLevel)

        val dto = Gson().fromJson("""{
            "analysisId":"phone-id","type":"PHONE","status":"COMPLETED","createdAt":"2026-11-11T00:00:00Z",
            "detail":{"maskedPhoneNumber":"010********","riskLevel":"CAUTION","reportCount":1000,
                "reportCountIsRange":true,"reasons":["stored report"]}
        }""", HistoryDetailDto::class.java)
        val result = requireNotNull(dto.toRecordDetail().result)
        assertEquals("010********", result.targetLabel)
        assertEquals(RiskLevel.CAUTION, result.riskLevel)
        assertEquals("신고 건수: 1000+건", result.keyFinding)
        assertEquals("stored report", result.evidence.single().description)
        assertNull(dto.copy(detail = dto.detail?.copy(reportCount = null)).toRecordDetail().result?.keyFinding)
        assertNull(dto.copy(detail = null).toRecordDetail().result)
    }

    @Test fun readsExistingPb05RecordsWithoutInventingMissingContent() {
        preferences.edit().putString("results", """[{"id":"old","riskLevel":"SAFE","savedAt":100,"fraudScore":12}]""").commit()
        val item = store.read().single()
        assertEquals(HistoryType.UNKNOWN, item.type)
        assertNull(item.preview)
        assertEquals(12.0, item.fraudScore!!, 0.0)
        assertNull(item.aiScore)
    }

    @Test fun deleteOnePreservesOtherRecordsAndClearPersistsEmptyState() {
        preferences.edit().putString("results", """[{"id":"one","riskLevel":"SAFE","savedAt":100},{"id":"two","riskLevel":"DANGER","savedAt":200}]""").commit()
        assertTrue(store.delete("one"))
        assertEquals(listOf("two"), HistoryStore(context).read().map { it.id })
        assertTrue(store.clear())
        assertTrue(HistoryStore(context).read().isEmpty())
    }

    @Test fun malformedEntriesDoNotHideValidRecordsAndBadScoresAreOmitted() {
        preferences.edit().putString("results", """[{"id":"bad","riskLevel":"ALIEN","savedAt":100},{"id":"valid","riskLevel":"CAUTION","savedAt":200,"aiScore":101}]""").commit()
        assertEquals("valid", store.read().single().id)
        assertNull(store.read().single().aiScore)
    }

    @Test fun filtersCombineTypeAndQueryAndKeepNewestFirst() {
        val records = listOf(
            HistoryRecord("old", RiskLevel.SAFE, 100, HistoryType.TEXT),
            HistoryRecord("new", RiskLevel.DANGER, 300, HistoryType.TEXT),
            HistoryRecord("image", RiskLevel.DANGER, 200, HistoryType.IMAGE),
        )
        assertEquals(listOf("new"), filterHistory(records, HistoryType.TEXT, "위험").map { it.id })
        assertEquals(listOf("new", "image", "old"), filterHistory(records, null, "").map { it.id })
        assertTrue(filterHistory(records, null, "없는 검색어").isEmpty())
    }

    @Test fun resultSerializationAndStoragePreserveEveryInputTypeWithoutOriginalContent() {
        HistoryType.entries.filter { it != HistoryType.UNKNOWN }.forEachIndexed { index, type ->
            val result = com.dundueni.app.feature.analysisresult.AnalysisResult(
                id = "typed-$index", riskLevel = RiskLevel.SAFE, summary = "summary",
                historyType = type, sourceText = "private-message", sourceUrl = "https://private.example",
            )
            val decoded = com.dundueni.app.feature.analysisresult.AnalysisResultJsonAdapter.decode(
                com.google.gson.Gson().toJson(result),
            )
            assertTrue(store.save(decoded, 100L + index))
            assertEquals(result.id, filterHistory(HistoryStore(context).read(), type, "").single().id)
        }
        val stored = preferences.getString("results", "")!!
        assertFalse(stored.contains("private-message"))
        assertFalse(stored.contains("private.example"))
        assertEquals(HistoryType.entries.size - 1, store.read().size)
    }

    @Test fun savingTypedRecordPreservesLegacyRecordAndItsDeletion() {
        preferences.edit().putString("results", """[{"id":"old","riskLevel":"SAFE","savedAt":100}]""").commit()
        val result = com.dundueni.app.feature.analysisresult.AnalysisResult(
            "new", RiskLevel.SAFE, "summary", historyType = HistoryType.IMAGE,
        )
        assertTrue(store.save(result, 200))
        assertTrue(store.save(result, 300))
        assertEquals(2, store.read().size)
        assertEquals(HistoryType.UNKNOWN, store.read().last().type)
        assertTrue(store.delete("old"))
        assertEquals("new", store.read().single().id)
    }
    @Test fun aiBadgeRequiresExplicitClassificationRatherThanAScoreThreshold() {
        val unspecified = HistoryRecord("score-only", RiskLevel.CAUTION, 100, HistoryType.IMAGE, aiScore = 99.0)
        assertEquals("주의", unspecified.statusLabel)
        val classified = unspecified.copy(id = "classified", aiLikelihood = com.dundueni.app.feature.analysisresult.AiLikelihood.HIGH)
        assertEquals("AI 생성 의심", classified.statusLabel)
        assertEquals(listOf("classified"), filterHistory(listOf(unspecified, classified), null, "AI 생성 의심").map { it.id })
    }

    @Test fun readsOptionalAiClassificationAndKeepsUnknownClassificationHonest() {
        preferences.edit().putString("results", """[{"id":"explicit","riskLevel":"CAUTION","savedAt":200,"type":"IMAGE","aiLikelihood":"HIGH"},{"id":"unknown","riskLevel":"SAFE","savedAt":100,"aiLikelihood":"INVALID"}]""").commit()
        val records = store.read()
        assertEquals("AI 생성 의심", records[0].statusLabel)
        assertEquals(com.dundueni.app.feature.analysisresult.AiLikelihood.UNKNOWN, records[1].aiLikelihood)
    }
    @Test fun savingUnclassifiedPhoneAndExplicitAiMetadataIsBackwardCompatible() {
        val result = com.dundueni.app.feature.analysisresult.AnalysisResult(
            "phone-unknown", RiskLevel.UNKNOWN, "summary", historyType = HistoryType.PHONE,
        )
        assertTrue(store.save(result, 100))
        assertEquals(HistoryType.PHONE, store.read().single().type)
        assertEquals(RiskLevel.UNKNOWN, store.read().single().riskLevel)
        val image = result.copy(id = "image-high", historyType = HistoryType.IMAGE,
            aiLikelihood = com.dundueni.app.feature.analysisresult.AiLikelihood.HIGH)
        assertTrue(store.save(image, 200))
        assertEquals("AI 생성 의심", store.read().first().statusLabel)
        assertEquals(2, store.read().size)
    }
}
