package com.dundueni.app.data.repository

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
