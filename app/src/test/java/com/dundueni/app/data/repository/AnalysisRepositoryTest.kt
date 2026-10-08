package com.dundueni.app.data.repository

import com.dundueni.app.data.remote.api.AnalysisApiService
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import okio.buffer
import okio.source

class AnalysisRepositoryTest {
    private val part = MultipartBody.Part.createFormData("image", "test.png", byteArrayOf(1).toRequestBody())

    @Test
    fun resultRequestParsesMapsAndClosesBody() = runBlocking {
        var closed = false
        val stream = object : java.io.ByteArrayInputStream(
            """{"risk_level":"HIGH","reasons":["reason"],"recommended_actions":["action"]}""".toByteArray()
        ) {
            override fun close() { closed = true; super.close() }
        }
        val source = stream.source().buffer()
        val body = object : ResponseBody() {
            override fun contentType() = null
            override fun contentLength() = -1L
            override fun source() = source
        }
        val fake = object : AnalysisApiService {
            override suspend fun analyzeImage(image: MultipartBody.Part): Response<ResponseBody> {
                assertSame(part, image)
                return Response.success(body)
            }
        }
        val result = AnalysisRepository(fake).analyzeResult(part)
        assertEquals(com.dundueni.app.data.model.RiskLevel.HIGH, result.riskLevel)
        assertEquals(listOf("reason"), result.reasons)
        assertEquals(listOf("action"), result.recommendedActions)
        assertTrue(closed)
    }

    @Test
    fun resultRequestRejectsHttpErrorMissingEmptyNullAndInvalidBody() {
        val responses = listOf(
            Response.error<ResponseBody>(500, "error".toResponseBody()),
            Response.success<ResponseBody>(null),
            Response.success("".toResponseBody()),
            Response.success("null".toResponseBody()),
            Response.success("""{"reasons":{}}""".toResponseBody())
        )
        for (response in responses) {
            val fake = object : AnalysisApiService {
                override suspend fun analyzeImage(image: MultipartBody.Part) = response
            }
            assertThrows(Exception::class.java) {
                runBlocking { AnalysisRepository(fake).analyzeResult(part) }
            }
        }
    }

    @Test
    fun forwardsSamePartAndReturnsSameResponse() = runBlocking {
        val response = Response.success("raw response".toResponseBody())
        var calls = 0
        val fake = object : AnalysisApiService {
            override suspend fun analyzeImage(image: MultipartBody.Part): Response<ResponseBody> {
                calls++
                assertSame(part, image)
                return response
            }
        }
        val result = AnalysisRepository(fake).analyzeImage(part)
        assertSame(response, result)
        assertEquals(1, calls)
        result.body()!!.use { assertEquals("raw response", it.string()) }
    }

    @Test
    fun preservesHttpErrorResponse() = runBlocking {
        val response = Response.error<ResponseBody>(422, "unconfirmed error format".toResponseBody())
        val fake = object : AnalysisApiService {
            override suspend fun analyzeImage(image: MultipartBody.Part) = response
        }
        val result = AnalysisRepository(fake).analyzeImage(part)
        assertSame(response, result)
        assertFalse(result.isSuccessful)
        result.errorBody()!!.use { assertEquals("unconfirmed error format", it.string()) }
    }

    @Test
    fun propagatesIoFailureAndCancellation() {
        for (failure in listOf(IOException("offline"), CancellationException("cancelled"))) {
            val fake = object : AnalysisApiService {
                override suspend fun analyzeImage(image: MultipartBody.Part): Response<ResponseBody> = throw failure
            }
            val caught = assertThrows(failure.javaClass) {
                runBlocking { AnalysisRepository(fake).analyzeImage(part) }
            }
            assertSame(failure, caught)
        }
    }
}
