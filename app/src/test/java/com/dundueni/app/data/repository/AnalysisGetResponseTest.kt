package com.dundueni.app.data.repository

import com.dundueni.app.data.model.ImageActionGuide
import com.dundueni.app.data.model.ImageAnalysisSummary
import com.dundueni.app.data.model.ImageDetailedReason
import com.dundueni.app.data.remote.api.AnalysisHttpException
import com.dundueni.app.data.remote.network.RetrofitClient
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class AnalysisGetResponseTest {
    private val id = "9dca5a1c-9736-43b1-9b7c-fad6b9ce7e72"

    @Test fun completedTextDetailMapsStoredDecisionScoreAndQuality() = runBlocking {
        val server = MockWebServer().apply { start() }
        try {
            server.enqueue(MockResponse().setResponseCode(200).setBody("""{
                "isSuccess":true,"code":"ANALYSIS200","result":{
                    "analysisId":"$id","type":"TEXT","status":"COMPLETED",
                    "textResult":{"decision":"defer","riskScore":0.37,"sourceType":"TEXT",
                        "quality":{"needsReview":true,"reasonCodes":["LOW_CONFIDENCE"],"ocrUsed":false}}
                }
            }"""))
            val result = repository(server).findResult(id)
            assertEquals(id, result.analysisId)
            assertEquals("TEXT", result.type)
            assertEquals("COMPLETED", result.status)
            assertEquals("defer", result.textDecision)
            assertEquals(0.37, result.riskScore!!, 0.0)
            assertEquals("TEXT", result.sourceType)
            assertEquals(true, result.textNeedsReview)
            assertEquals(listOf("LOW_CONFIDENCE"), result.textReasonCodes)
            assertEquals(false, result.textOcrUsed)
            assertNull(result.normalizedRiskLevel)
            assertEquals("/api/analysis/$id", server.takeRequest().path)
            assertEquals(1, server.requestCount)
        } finally { server.shutdown() }
    }

    @Test fun initialCompletedDetailReturnsSummaryWithASingleGet() = runBlocking {
        val server = MockWebServer().apply { start() }
        try {
            server.enqueue(MockResponse().setResponseCode(200).setBody("""{
                "isSuccess":true,"code":"ANALYSIS200","result":{
                    "analysisId":"$id","type":"IMAGE","status":"COMPLETED",
                    "aiGenerationScore":82,"aiRiskLevel":null,
                    "reasons":[{"description":"server reason"}],
                    "models":{"status":"COMPLETED","generation":null,
                        "manipulation":{"status":"SUCCEEDED","displayScore":37.5}},
                    "overallResult":{"decision":"INCONCLUSIVE","decisionLabel":"Pending policy",
                        "policyVersion":"image-overall-v1","suspectedKinds":[],"uncertainKinds":[]},
                    "guides":["server guide"],
                    "summaryStatus":"COMPLETED","summary":"Gemini summary"
                }
            }"""))
            val repository = repository(server)
            val result = repository.findResult(id)
            assertEquals(listOf("server reason"), result.reasons)
            assertEquals(listOf("server guide"), result.recommendedActions)
            assertNull(result.summary)
            assertNull(result.normalizedRiskLevel)
            assertEquals("/api/analysis/$id", server.takeRequest().path)
            assertEquals(1, server.requestCount)
            assertEquals(ImageAnalysisSummary(summaryStatus = "COMPLETED", summary = "Gemini summary"), result.imageSummary)
        } finally { server.shutdown() }
    }

    @Test fun completedUsesNormalizedInnerResultAndExactGetPath() = runBlocking {
        val server = MockWebServer().apply { start() }
        try {
            server.enqueue(MockResponse().setResponseCode(200).setBody(detail("COMPLETED")))
            val result = repository(server).findResult(id)
            assertEquals(id, result.analysisId)
            assertEquals("COMPLETED", result.status)
            assertEquals("UNKNOWN", result.normalizedRiskLevel)
            assertEquals(listOf("저장된 모델 근거"), result.reasons)
            assertEquals("저장된 요약", result.summary)
            assertEquals(emptyList<String>(), result.recommendedActions)
            assertEquals(82.0, result.aiGenerationScore!!, 0.0)
            assertNull(result.riskScore)
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/analysis/$id", request.path)
            assertNull(request.getHeader("X-Dundeuni-Auth-Required"))
        } finally { server.shutdown() }
    }

    @Test fun queuedAndAnalyzingStayPendingAndFailedNeverBecomesResult() = runBlocking {
        val server = MockWebServer().apply { start() }
        try {
            val repository = repository(server)
            for (status in listOf("QUEUED", "ANALYZING")) {
                server.enqueue(MockResponse().setResponseCode(200).setBody(detail(status)))
                val error = assertThrows(AnalysisNotCompletedException::class.java) {
                    runBlocking { repository.findResult(id) }
                }
                assertEquals(status, error.status.wireValue)
                assertEquals(id, error.analysisId)
            }
            server.enqueue(MockResponse().setResponseCode(200).setBody(detail("FAILED")))
            assertThrows(AnalysisFailedException::class.java) {
                runBlocking { repository.findResult(id) }
            }
        } finally { server.shutdown() }
        Unit
    }

    @Test fun completedWithNullableFieldsDoesNotInventFindings() = runBlocking {
        val server = MockWebServer().apply { start() }
        try {
            server.enqueue(MockResponse().setResponseCode(200).setBody("""{
                "isSuccess":true,"code":"ANALYSIS200","result":{
                    "analysisId":"$id","type":"IMAGE","status":"COMPLETED",
                    "result":{"riskLevel":"UNKNOWN","reasons":[],"guidance":[],"summary":null}
                }
            }"""))
            val result = repository(server).findResult(id)
            assertNull(result.summary)
            assertNull(result.aiGenerationScore)
            assertEquals(emptyList<String>(), result.reasons)
            assertEquals(emptyList<String>(), result.recommendedActions)
        } finally { server.shutdown() }
    }

    @Test fun httpErrorsPreserve400401404And500() = runBlocking {
        val server = MockWebServer().apply { start() }
        try {
            val repository = repository(server)
            for (status in listOf(400, 401, 404, 500)) {
                server.enqueue(MockResponse().setResponseCode(status)
                    .setBody("""{"isSuccess":false,"code":"ERROR","message":"private","result":null}"""))
                val error = assertThrows(AnalysisHttpException::class.java) {
                    runBlocking { repository.findResult(id) }
                }
                assertEquals(status, error.httpStatus)
            }
        } finally { server.shutdown() }
    }


    @Test fun completedPostRequiresGetAndNeverFallsBackAfterGetError() = runBlocking {
        val server = MockWebServer().apply { start() }
        try {
            val repository = repository(server)
            server.enqueue(MockResponse().setResponseCode(202).setBody("""{
                "isSuccess":true,"code":"ANALYSIS202","result":{
                "analysisId":"$id","type":"IMAGE","status":"COMPLETED","aiGenerationScore":1}}
            """))
            server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))
            val part = okhttp3.MultipartBody.Part.createFormData("file", "image.png",
                okhttp3.RequestBody.create(null, byteArrayOf(1)))
            val error = assertThrows(AnalysisLookupException::class.java) {
                runBlocking { repository.analyzeResult(part) }
            }
            assertEquals(id, error.analysisId)
            assertEquals(500, (error.cause as AnalysisHttpException).httpStatus)
            server.enqueue(MockResponse().setResponseCode(200).setBody(detail("COMPLETED")))
            val result = repository.findResult(id)
            assertEquals(82.0, result.aiGenerationScore!!, 0.0)
            assertEquals("POST", server.takeRequest().method)
            repeat(2) {
                val request = server.takeRequest()
                assertEquals("GET", request.method)
                assertEquals("/api/analysis/$id", request.path)
            }
            assertEquals(3, server.requestCount)
        } finally { server.shutdown() }
        Unit
    }

    @Test fun pendingSummaryRefreshUsesTheSameDetailGetAndReturnsAllSummaryFields() = runBlocking {
        val server = MockWebServer().apply { start() }
        try {
            val completedFields = """,
                "summary":"Gemini 요약","summaryModel":"gemini","summarizedAt":"2026-11-25T06:30:00.000Z",
                "detailedReasons":[{"type":"GENERATION","title":"생성 근거","description":"근거 설명"}],
                "actionGuides":[{"title":"공식 연락처","description":"공식 홈페이지에서 확인하세요."}],
                "reassurance":"서두르지 않아도 됩니다."
            """
            server.enqueue(MockResponse().setBody(summary("PENDING")))
            server.enqueue(MockResponse().setBody(summary("COMPLETED", completedFields)))
            val repository = repository(server)
            val original = repository.findResult(id)
            assertEquals(ImageAnalysisSummary(summaryStatus = "PENDING"), original.imageSummary)
            assertEquals(listOf("서버 모델 근거"), original.reasons)
            val refreshed = repository.findImageSummary(id)
            assertEquals(
                ImageSummaryResult.Completed(
                    ImageAnalysisSummary(
                        summaryStatus = "COMPLETED",
                        summary = "Gemini 요약",
                        detailedReasons = listOf(ImageDetailedReason("GENERATION", "생성 근거", "근거 설명")),
                        actionGuides = listOf(ImageActionGuide("공식 연락처", "공식 홈페이지에서 확인하세요.")),
                        reassurance = "서두르지 않아도 됩니다.",
                        summaryModel = "gemini",
                        summarizedAt = "2026-11-25T06:30:00.000Z",
                    ),
                ),
                refreshed,
            )
            assertEquals(2, server.requestCount)
            repeat(2) {
                val request = server.takeRequest()
                assertEquals("GET", request.method)
                assertEquals("/api/analysis/$id", request.path)
            }
        } finally { server.shutdown() }
    }

    @Test fun summaryLookupDistinguishesPendingFailedUnavailableAndLookupErrors() = runBlocking {
        val server = MockWebServer().apply { start() }
        try {
            val repository = repository(server)
            val responses = listOf(
                MockResponse().setResponseCode(404).setBody("{}") to ImageSummaryResult.LookupError,
                MockResponse().setResponseCode(500).setBody("{}") to ImageSummaryResult.LookupError,
                MockResponse().setBody(summary("PENDING")) to ImageSummaryResult.Pending,
                MockResponse().setBody(summary("FAILED")) to ImageSummaryResult.Failed,
                MockResponse().setBody(summary(null)) to ImageSummaryResult.Unavailable,
                MockResponse().setBody(summary("DISABLED")) to ImageSummaryResult.Unavailable,
                MockResponse().setBody(summary("COMPLETED")) to
                    ImageSummaryResult.Completed(ImageAnalysisSummary(summaryStatus = "COMPLETED")),
                MockResponse().setBody(summary("COMPLETED").replace("\"IMAGE\"", "\"TEXT\"")) to
                    ImageSummaryResult.Unavailable,
                MockResponse().setBody(detail("FAILED")) to ImageSummaryResult.LookupError,
                MockResponse().setBody(summary("COMPLETED").replace(id, "other-id")) to ImageSummaryResult.LookupError,
                MockResponse().setBody(summary("COMPLETED").replace("ANALYSIS200", "OTHER")) to ImageSummaryResult.LookupError,
                MockResponse().setBody("invalid json") to ImageSummaryResult.LookupError,
            )
            for ((response, expected) in responses) {
                server.enqueue(response)
                assertEquals(expected, repository.findImageSummary(id))
                val request = server.takeRequest()
                assertEquals("GET", request.method)
                assertEquals("/api/analysis/$id", request.path)
            }
            assertEquals(responses.size, server.requestCount)
        } finally { server.shutdown() }
    }

    private fun summary(status: String?, fields: String = "") = """{
        "isSuccess":true,"code":"ANALYSIS200","result":{
            "analysisId":"$id","type":"IMAGE","status":"COMPLETED",
            "reasons":[{"description":"서버 모델 근거"}],
            "summaryStatus":${status?.let { "\"$it\"" } ?: "null"}$fields
        }
    }"""

    private fun repository(server: MockWebServer): AnalysisRepository {
        val client = RetrofitClient(server.url("/v1/").toString())
        return AnalysisRepository(client.analysisApiService, client.analysisQueryApiService)
    }

    private fun detail(status: String) = """{
        "isSuccess":true,"code":"ANALYSIS200","message":"조회 성공",
        "result":{"analysisId":"$id","type":"IMAGE","sourceType":"IMAGE",
          "status":"$status","aiGenerationScore":82,"aiRiskLevel":null,
          "reasons":[{"description":"레거시 근거"}],
          "result":{"riskLevel":"UNKNOWN","textDecision":null,"riskScore":null,
            "scoreScale":null,"scoreMeaning":null,"reasons":["저장된 모델 근거"],
            "guidance":[],"summary":"저장된 요약"}}
    }"""
}
