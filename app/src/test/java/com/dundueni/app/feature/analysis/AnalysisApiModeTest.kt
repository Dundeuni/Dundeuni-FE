package com.dundueni.app.feature.analysis

import com.dundueni.app.BuildConfig
import com.dundueni.app.data.remote.api.AnalysisHttpException
import com.dundueni.app.data.repository.AnalysisRepository
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class AnalysisApiModeTest {
    private val imageBytes = "image-content".toByteArray()
    private val image = MultipartBody.Part.createFormData(
        "file", "photo.jpg", imageBytes.toRequestBody("image/jpeg".toMediaType())
    )

    @Test
    fun defaultBuildConfigurationSelectsFake() {
        assertEquals("FAKE", BuildConfig.ANALYSIS_API_MODE)
        assertTrue(AnalysisFlowDependencies.createRepository() is AnalysisRepository)
    }

    @Test
    fun realModeUsesRetrofitAndPreservesMultipartAndResponseFlow() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(202).setBody(completedResponse()))
            val repository = AnalysisFlowDependencies.createRepository("REAL", server.url("/").toString())
                as AnalysisRepository
            val result = repository.analyzeResult(image)

            assertEquals("real-id", result.analysisId)
            assertEquals("SERVER_RISK", result.aiRiskLevel)
            assertEquals(76.3, result.aiGenerationScore!!, 0.0)
            assertEquals(listOf("server reason"), result.reasons)

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/analysis", request.path)
            assertTrue(request.getHeader("Content-Type")!!.startsWith("multipart/form-data; boundary="))
            val multipart = request.body.readUtf8()
            assertEquals(2, Regex("Content-Disposition:").findAll(multipart).count())
            assertTrue(multipart.contains("name=\"type\""))
            assertTrue(multipart.contains("IMAGE"))
            assertTrue(multipart.contains("name=\"file\"; filename=\"photo.jpg\""))
            assertTrue(multipart.contains("Content-Type: image/jpeg"))
            assertTrue(multipart.contains("image-content"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun realHttpErrorAndConnectionFailureNeverFallBackToFake(): Unit = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse().setResponseCode(503).setBody(
                    """{"isSuccess":false,"code":"ANALYSIS503","message":"unavailable","result":null}"""
                )
            )
            val repository = AnalysisFlowDependencies.createRepository("REAL", server.url("/").toString())
            val httpError = try {
                repository.analyzeResult(image)
                fail("Expected the real HTTP error to reach the repository caller")
                null
            } catch (error: AnalysisHttpException) {
                error
            }
            assertEquals(503, httpError!!.httpStatus)
            assertEquals("ANALYSIS503", httpError.code)

            val unreachableBaseUrl = server.url("/").toString()
            server.shutdown()
            assertThrows(IOException::class.java) {
                runBlocking {
                    AnalysisFlowDependencies.createRepository("REAL", unreachableBaseUrl).let {
                        it.analyzeResult(image)
                    }
                }
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun missingOrInvalidRealBaseUrlReturnsAnExplicitConfigurationError() {
        for (baseUrl in listOf(null, "", "ftp://example.com/", "https://example.com", "not-a-url")) {
            val error = assertThrows(AnalysisApiConfigurationException::class.java) {
                runBlocking {
                    AnalysisFlowDependencies.createRepository("REAL", baseUrl).analyzeResult(image)
                }
            }
            assertTrue(error.message!!.contains("analysis.api.baseUrl"))
        }
    }

    @Test
    fun invalidModeDoesNotSilentlySelectFake() {
        val error = assertThrows(AnalysisApiConfigurationException::class.java) {
            runBlocking {
                AnalysisFlowDependencies.createRepository("UNKNOWN", "https://example.com/").analyzeResult(image)
            }
        }
        assertTrue(error.message!!.contains("FAKE or REAL"))
    }

    private fun completedResponse() = """{
        "isSuccess":true,
        "code":"ANALYSIS202",
        "message":"accepted",
        "result":{
          "analysisId":"real-id","type":"IMAGE","status":"COMPLETED",
          "aiGenerationScore":76.3,"aiRiskLevel":"SERVER_RISK",
          "reasons":[{"description":"server reason"}],"modelVersion":"test-model",
          "errorCode":null,"createdAt":"2026-10-08T00:00:00.000Z",
          "completedAt":"2026-10-08T00:00:01.000Z","expiresAt":"2026-10-15T00:00:01.000Z"
        }
    }"""
}
