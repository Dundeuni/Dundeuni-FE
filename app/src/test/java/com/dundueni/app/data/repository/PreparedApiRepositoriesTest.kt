package com.dundueni.app.data.repository

import com.dundueni.app.data.remote.api.AnalysisSupportApiService
import com.dundueni.app.data.remote.api.HistoryApiService
import com.dundueni.app.data.remote.api.HomeDraftApiService
import com.dundueni.app.data.remote.api.UserSettingsApiService
import com.dundueni.app.data.remote.dto.SettingsPatchRequestDto
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Interceptor responses exercise Retrofit/DTO/repository contracts without opening a socket. */
class PreparedApiRepositoriesTest {
    private class StubTransport {
        private val replies = ArrayDeque<Pair<Int, String>>()
        val requests = mutableListOf<Request>()

        fun reply(status: Int, body: String) { replies.addLast(status to body) }

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://example.invalid/")
            .client(OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
                val request = chain.request()
                synchronized(this) {
                    requests.add(request)
                    val (status, body) = replies.removeFirst()
                    okhttp3.Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(status)
                        .message("stub")
                        .body(body.toResponseBody("application/json".toMediaType()))
                        .build()
                }
            }).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        inline fun <reified T> service(): T = retrofit.create(T::class.java)
    }

    private fun envelope(code: String, result: String) =
        """{"isSuccess":true,"code":"$code","message":"ok","result":$result}"""

    private fun Request.json() = JsonParser.parseString(body!!.let { body ->
        Buffer().also(body::writeTo).readUtf8()
    }).asJsonObject

    @Test fun historyApisStaySeparateFromLocalStoreAndRejectFailures() = runBlocking {
        val stub = StubTransport()
        val repository = HistoryApiRepository(stub.service<HistoryApiService>())
        stub.reply(200, envelope("HISTORY200", """{"items":[],"page":0,"size":10,"totalElements":0,"totalPages":0}"""))
        stub.reply(200, envelope("HISTORY200", """{"items":[{"analysisId":"a","type":"TEXT","title":"x","maskedSummary":"y","createdAt":"2026-01-01T00:00:00Z"}]}"""))
        stub.reply(200, envelope("HISTORY200", """{"deleted":true}"""))
        stub.reply(200, envelope("HISTORY200", """{"deletedCount":0}"""))
        assertEquals(0, repository.list(0, 10).items!!.size)
        assertEquals("y", repository.recent().items!!.single().maskedSummary)
        repository.deleteOne("analysis-id")
        assertEquals(0, repository.deleteAll())
        assertEquals(listOf("GET", "GET", "DELETE", "DELETE"), stub.requests.map { it.method })
        assertEquals(listOf(
            "/api/analysis-history?page=0&size=10", "/api/analysis-history/recent",
            "/api/analysis-history/analysis-id", "/api/analysis-history",
        ), stub.requests.map { it.url.encodedPath + (it.url.encodedQuery?.let { q -> "?$q" } ?: "") })
        assertTrue(stub.requests.all { it.header("X-Dundeuni-Auth-Required") == "true" })

        stub.reply(404, """{"isSuccess":false,"code":"HISTORY404","message":"missing","result":null}""")
        try {
            repository.deleteOne("missing")
            fail("404 was accepted")
        } catch (error: PreparedApiHttpException) {
            assertEquals(404, error.httpStatus)
            assertEquals("HISTORY404", error.apiCode)
        }
    }

    @Test fun settingsApisValidatePatchAndDoNotTouchLocalPreferences() = runBlocking {
        val stub = StubTransport()
        val repository = UserSettingsApiRepository(stub.service<UserSettingsApiService>())
        val settings = """{"easyMode":true,"saveHistory":true,"retentionDays":30,"onboardingCompleted":true,"ageRange":null}"""
        stub.reply(200, envelope("SETTINGS200", settings))
        stub.reply(200, envelope("SETTINGS200", settings))
        stub.reply(200, envelope("SETTINGS200", settings.replace("\"saveHistory\":true", "\"saveHistory\":false")))
        stub.reply(200, envelope("USAGE200", """{"plan":"BASIC","limit":30,"used":4,"reserved":1,"remaining":25,"resetsAt":"2026-12-01T00:00:00Z"}"""))
        assertEquals(true, repository.completeOnboarding(true).onboardingCompleted)
        assertEquals(30, repository.getSettings().retentionDays)
        assertEquals(false, repository.updateSettings(SettingsPatchRequestDto(saveHistory = false)).saveHistory)
        assertEquals(25, repository.getUsage().remaining)
        assertEquals(listOf("PUT", "GET", "PATCH", "GET"), stub.requests.map { it.method })
        assertEquals(listOf("/api/me/onboarding", "/api/me/settings", "/api/me/settings", "/api/me/usage"),
            stub.requests.map { it.url.encodedPath })
        assertEquals(setOf("easyMode", "completed"), stub.requests[0].json().keySet())
        assertEquals(setOf("saveHistory"), stub.requests[2].json().keySet())
        assertFalse(stub.requests[2].json()["saveHistory"].asBoolean)
        try {
            repository.updateSettings(SettingsPatchRequestDto())
            fail("Empty PATCH was accepted")
        } catch (_: IllegalArgumentException) { }
        assertEquals(4, stub.requests.size)
    }

    @Test fun homeApisUseTheirDistinctSuccessDataEnvelope() = runBlocking {
        val stub = StubTransport()
        val repository = HomeDraftRepository(stub.service<HomeDraftApiService>())
        stub.reply(200, """{"success":true,"data":{"floating":{"enabled":true},"recentAnalyses":[]}}""")
        stub.reply(200, """{"success":true,"data":{"tips":[{"tipId":1,"category":"PHISHING","content":"Tip"}]}}""")
        assertEquals(true, repository.dashboard().floating!!.enabled)
        assertEquals("Tip", repository.safetyTips(2).tips!!.single().content)
        assertEquals("/api/home", stub.requests[0].url.encodedPath)
        assertEquals("/api/safety-tips?limit=2", stub.requests[1].url.encodedPath + "?" + stub.requests[1].url.encodedQuery)
        assertEquals("true", stub.requests[0].header("X-Dundeuni-Auth-Required"))
        assertNull(stub.requests[1].header("X-Dundeuni-Auth-Required"))
        try {
            repository.safetyTips(11)
            fail("Out-of-range limit was accepted")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun retryAndUploadLookupCarryRequiredHeadersAndResponseFields() = runBlocking {
        val stub = StubTransport()
        val repository = AnalysisSupportRepository(stub.service<AnalysisSupportApiService>())
        stub.reply(202, envelope("ANALYSIS202", """{"analysisId":"analysis-id","status":"ANALYZING","expiresAt":"2026-12-01T00:00:00Z"}"""))
        stub.reply(200, envelope("IMAGE_UPLOAD200", """{"uploadId":"upload-id","status":"NO_CANDIDATES","image":{"width":400,"height":800,"coordinateSpace":"NORMALIZED_IMAGE"},"detectorVersion":"v1","expiresAt":"2026-12-01T00:00:00Z","original":{"elementId":"original-id","type":"ORIGINAL","bbox":{"left":0,"top":0,"width":400,"height":800}},"elements":[]}"""))
        assertEquals("ANALYZING", repository.retry("analysis-id", "retry-key").status)
        assertEquals("original-id", repository.lookupUpload("upload-id").original!!.elementId)
        assertEquals("POST", stub.requests[0].method)
        assertEquals("/api/analysis/analysis-id/retry", stub.requests[0].url.encodedPath)
        assertEquals("retry-key", stub.requests[0].header("Idempotency-Key"))
        assertEquals(emptySet<String>(), stub.requests[0].json().keySet())
        assertEquals("GET", stub.requests[1].method)
        assertEquals("/api/analysis/image-uploads/upload-id", stub.requests[1].url.encodedPath)
        assertTrue(stub.requests.all { it.header("X-Dundeuni-Auth-Required") == "true" })
    }

    @Test fun successfulHttpStatusDoesNotHideProtocolFailures() = runBlocking {
        val stub = StubTransport()
        val history = HistoryApiRepository(stub.service<HistoryApiService>())
        val home = HomeDraftRepository(stub.service<HomeDraftApiService>())
        val analysis = AnalysisSupportRepository(stub.service<AnalysisSupportApiService>())
        stub.reply(200, envelope("WRONG_CODE", """{"items":[]}"""))
        try {
            history.recent()
            fail("Wrong envelope code was accepted")
        } catch (_: PreparedApiProtocolException) { }

        stub.reply(200, """{"success":false,"data":{"floating":{"enabled":true},"recentAnalyses":[]}}""")
        try {
            home.dashboard()
            fail("False success was accepted")
        } catch (_: PreparedApiProtocolException) { }

        stub.reply(409, """{"isSuccess":false,"code":"ANALYSIS409","message":"Not retryable","result":null}""")
        try {
            analysis.retry("analysis-id", "retry-key")
            fail("409 was accepted")
        } catch (error: PreparedApiHttpException) {
            assertEquals(409, error.httpStatus)
            assertEquals("ANALYSIS409", error.apiCode)
        }
    }
}
