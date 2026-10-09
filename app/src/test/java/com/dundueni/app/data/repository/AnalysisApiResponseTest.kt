package com.dundueni.app.data.repository

import com.dundueni.app.data.remote.api.AnalysisApiService
import com.dundueni.app.data.remote.api.AnalysisHttpException
import com.dundueni.app.data.remote.api.AnalysisProtocolException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class AnalysisApiResponseTest {
    private val part = MultipartBody.Part.createFormData("file", "image.png", byteArrayOf(1).toRequestBody())

    @Test
    fun parsesActualHttp202EnvelopeAndNestedNullableResult() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(202).setBody(completedJson()))
            val repository = AnalysisRepository(
                com.dundueni.app.data.remote.network.RetrofitClient(server.url("/").toString()).analysisApiService
            )
            val outcome = repository.analyzeResponse(part) as AnalysisRequestOutcome.Completed
            val envelope = outcome.response
            assertTrue(envelope.isSuccess!!)
            assertEquals("ANALYSIS202", envelope.code)
            assertEquals("analysis-uuid", envelope.result!!.analysisId)
            assertEquals("IMAGE", envelope.result.type)
            assertEquals(82.4, envelope.result.aiGenerationScore!!, 0.0)
            assertEquals("CAUTION", envelope.result.aiRiskLevel)
            assertEquals("signal", envelope.result.reasons!!.single()!!.description)
            assertNull(envelope.result.errorCode)
            assertNull(envelope.result.completedAt)
            assertEquals("2026-10-04T00:00:00.000Z", envelope.result.createdAt)
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/analysis", request.path)
        } finally { server.shutdown() }
    }

    @Test
    fun distinguishesEveryDocumentedAnalysisStatus() {
        val cases = listOf(
            "COMPLETED" to AnalysisRequestOutcome.Completed::class.java,
            "FAILED" to AnalysisRequestOutcome.Failed::class.java,
            "QUEUED" to AnalysisRequestOutcome.InProgress::class.java,
            "ANALYZING" to AnalysisRequestOutcome.InProgress::class.java
        )
        for ((status, expected) in cases) {
            val outcome = runBlocking { repository(Response.success(202, body(successJson(status)))).analyzeResponse(part) }
            assertTrue("$status outcome", expected.isInstance(outcome))
            assertEquals(status, outcome.response.result!!.status)
            if (outcome is AnalysisRequestOutcome.InProgress) {
                assertEquals(AnalysisExecutionStatus.valueOf(status), outcome.status)
            }
        }
    }

    @Test
    fun domainResultPathReturnsOnlyCompletedAndRejectsFailedOrPendingStatuses() {
        val completed = runBlocking {
            repository(Response.success(202, body(successJson("COMPLETED")))).analyzeResult(part)
        }
        assertEquals("COMPLETED", completed.status)
        for (status in listOf("FAILED", "QUEUED", "ANALYZING")) {
            assertThrows(IOException::class.java) {
                runBlocking {
                    repository(Response.success(202, body(successJson(status)))).analyzeResult(part)
                }
            }
        }
    }

    @Test
    fun preservesFailedAnalysisErrorCodeAndNullableFields() {
        val outcome = runBlocking {
            repository(Response.success(202, body(successJson("FAILED", "ANALYSIS503"))))
                .analyzeResponse(part)
        } as AnalysisRequestOutcome.Failed
        assertEquals("ANALYSIS503", outcome.response.result!!.errorCode)
        assertNull(outcome.response.result.aiGenerationScore)
        assertNull(outcome.response.result.aiRiskLevel)
        assertNull(outcome.response.result.reasons)
        assertNull(outcome.response.result.completedAt)
    }

    @Test
    fun parsesStructuredHttpErrorsAndPreservesHttpStatusAndBeFields() {
        val cases = listOf(
            400 to "COMMON400", 413 to "COMMON413", 415 to "COMMON415",
            503 to "ANALYSIS503", 500 to "COMMON500", 401 to "AUTH401",
            409 to "COMMON409", 429 to "USAGE429", 418 to "UNEXPECTED418"
        )
        for ((http, code) in cases) {
            val response = Response.error<ResponseBody>(
                http, errorJson(code).toResponseBody("application/json".toMediaType())
            )
            val error = assertThrows(AnalysisHttpException::class.java) {
                runBlocking { repository(response).analyzeResponse(part) }
            }
            assertEquals(http, error.httpStatus)
            assertEquals(code, error.code)
            assertEquals("BE message $code", error.serverMessage)
        }
    }

    @Test
    fun malformedOrMissingErrorBodyStillPreservesHttpStatus() {
        for (bodyText in listOf("", "not-json", "{}")) {
            val response = Response.error<ResponseBody>(
                415, bodyText.toResponseBody()
            )
            val error = assertThrows(AnalysisHttpException::class.java) {
                runBlocking { repository(response).analyzeResponse(part) }
            }
            assertEquals(415, error.httpStatus)
            assertNull(error.code)
        }
    }

    @Test
    fun rejectsEmptyInvalidUnexpectedAndContradictorySuccessBodies() {
        val cases = listOf(
            "",
            "not-json",
            "null",
            "{}",
            """{"isSuccess":false,"code":"COMMON500","message":"bad","result":null}""",
            successJson("UNKNOWN"),
            """{"isSuccess":true,"code":"WRONG","result":{"status":"COMPLETED"}}""",
            """{"isSuccess":true,"code":"ANALYSIS202","result":null}"""
        )
        for (json in cases) {
            val error = assertThrows(AnalysisProtocolException::class.java) {
                runBlocking { repository(Response.success(202, body(json))).analyzeResponse(part) }
            }
            assertNotNull(error.message)
            if (json == successJson("UNKNOWN")) assertEquals("UNKNOWN", error.serverStatus)
        }
        val wrongHttp = assertThrows(AnalysisHttpException::class.java) {
            runBlocking { repository(Response.success(200, body(completedJson()))).analyzeResponse(part) }
        }
        assertEquals(200, wrongHttp.httpStatus)
    }

    @Test
    fun closesSuccessAndErrorResponseBodies() {
        var successClosed = false
        val successBody = trackedBody(completedJson()) { successClosed = true }
        runBlocking { repository(Response.success(202, successBody)).analyzeResponse(part) }
        assertTrue(successClosed)

        var errorClosed = false
        val errorBody = trackedBody(errorJson("COMMON415")) { errorClosed = true }
        val response = Response.error<ResponseBody>(415, errorBody)
        assertThrows(AnalysisHttpException::class.java) {
            runBlocking { repository(response).analyzeResponse(part) }
        }
        assertTrue(errorClosed)
    }

    @Test
    fun propagatesNetworkIoAndCancellationWithoutRelabeling() {
        for (failure in listOf(IOException("timeout"), CancellationException("cancelled"))) {
            val caught = assertThrows(failure.javaClass) {
                runBlocking { repository { throw failure }.analyzeResponse(part) }
            }
            assertEquals(failure.message, caught.message)
            if (failure is CancellationException) assertTrue(caught is CancellationException)
        }
    }

    private fun repository(response: Response<ResponseBody>) = repository { response }
    private fun repository(call: suspend (MultipartBody.Part) -> Response<ResponseBody>) =
        AnalysisRepository(object : AnalysisApiService {
            override suspend fun analyzeImage(image: MultipartBody.Part, type: RequestBody) = call(image)
        })

    private fun body(json: String) = json.toResponseBody("application/json".toMediaType())
    private fun trackedBody(json: String, onClose: () -> Unit) = object : ResponseBody() {
        private val data = Buffer().writeUtf8(json)
        override fun contentType() = "application/json".toMediaType()
        override fun contentLength() = data.size
        override fun source() = data
        override fun close() { onClose(); super.close() }
    }

    private fun completedJson() = successJson("COMPLETED")
    private fun successJson(status: String, errorCode: String? = null) = """{
        "isSuccess":true,"code":"ANALYSIS202","message":"accepted","result":{
          "analysisId":"analysis-uuid","type":"IMAGE","status":"$status",
          "aiGenerationScore":${if (status == "FAILED") "null" else "82.4"},
          "aiRiskLevel":${if (status == "FAILED") "null" else "\"CAUTION\""},
          "reasons":${if (status == "FAILED") "null" else "[{\"description\":\"signal\"}]"},
          "modelVersion":"model-1","errorCode":${errorCode?.let { "\"$it\"" } ?: "null"},
          "createdAt":"2026-10-04T00:00:00.000Z","completedAt":null,"expiresAt":"2026-10-05T00:00:00.000Z"
        }
    }"""
    private fun errorJson(code: String) =
        """{"isSuccess":false,"code":"$code","message":"BE message $code","result":null}"""
}
