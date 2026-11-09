package com.dundueni.app.data.remote.api

import com.dundueni.app.data.remote.network.RetrofitClient
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneAnalysisApiServiceTest {
    @Test
    fun sendsPhoneNumberAsJsonAndAcceptsOnlyCompleted201Payload() = runBlocking {
        val server = MockWebServer().apply { start() }
        try {
            server.enqueue(MockResponse().setResponseCode(201).setBody(
                """{"analysisId":"d734f69e-ce2e-4914-8a2e-230713d37c96","status":"COMPLETED"}"""
            ))
            val response = RetrofitClient(server.url("/v1/").toString()).phoneAnalysisApiService
                .analyzePhone(PhoneAnalysisRequestDto("010-1234-5678"))
            assertEquals(201, response.code())
            assertTrue(response.body()!!.isCompleted())
            assertEquals("d734f69e-ce2e-4914-8a2e-230713d37c96", response.body()!!.analysisId)
            assertEquals("COMPLETED", response.body()!!.status)

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/analyses/phone", request.path)
            assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
            assertEquals("""{"phoneNumber":"010-1234-5678"}""", request.body.readUtf8())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun rejectsMissingOrInvalidCompletionFields() {
        assertFalse(PhoneAnalysisResponseDto().isCompleted())
        assertFalse(PhoneAnalysisResponseDto("not-a-uuid", "COMPLETED").isCompleted())
        assertFalse(PhoneAnalysisResponseDto("d734f69e-ce2e-4914-8a2e-230713d37c96", "QUEUED").isCompleted())
    }

    @Test
    fun preservesBackendErrorStatuses() = runBlocking {
        val server = MockWebServer().apply { start() }
        try {
            val api = RetrofitClient(server.url("/").toString()).phoneAnalysisApiService
            for (status in listOf(400, 401, 500, 503)) {
                server.enqueue(MockResponse().setResponseCode(status).setBody("""{"code":"ERROR"}"""))
                val response = api.analyzePhone(PhoneAnalysisRequestDto("010-1234-5678"))
                assertEquals(status, response.code())
                response.errorBody()?.close()
            }
        } finally {
            server.shutdown()
        }
    }
}
