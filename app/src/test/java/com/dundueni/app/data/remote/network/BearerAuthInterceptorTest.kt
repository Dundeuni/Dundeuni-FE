package com.dundueni.app.data.remote.network

import android.util.Base64
import com.dundueni.app.data.auth.AuthSession
import com.dundueni.app.data.auth.AuthSessionSource
import com.dundueni.app.data.auth.AuthSessionStore
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Headers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.test.runTest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class BearerAuthInterceptorTest {
    private val server = MockWebServer().apply { start() }
    private val sessions = MemorySessionStore()
    private var invalidations = 0
    private val api = Retrofit.Builder().baseUrl(server.url("/"))
        .client(OkHttpClient.Builder().addInterceptor(
            BearerAuthInterceptor(sessions, { invalidations++ }),
        ).build())
        .build().create(TestApi::class.java)

    @After fun close() = server.shutdown()

    @Test fun attachesBearerOnlyToMarkedProtectedCallsAndNeverSendsMockToken() = runTest {
        sessions.save(session(AuthSessionSource.BACKEND))
        server.enqueue(MockResponse().setResponseCode(200))
        api.protectedCall().body()?.close()
        assertEquals("Bearer ${sessions.read()!!.accessToken}", server.takeRequest().getHeader("Authorization"))

        sessions.save(session(AuthSessionSource.MOCK))
        server.enqueue(MockResponse().setResponseCode(200))
        api.protectedCall().body()?.close()
        assertNull(server.takeRequest().getHeader("Authorization"))

        sessions.save(session(AuthSessionSource.BACKEND))
        server.enqueue(MockResponse().setResponseCode(200))
        api.publicCall().body()?.close()
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test fun onlyAuthFailuresForProtectedCallsWithBackendTokenInvalidateTheSession() = runTest {
        sessions.save(session(AuthSessionSource.MOCK))
        server.enqueue(MockResponse().setResponseCode(401).setBody("{\"code\":\"AUTH401\"}"))
        api.protectedCall().errorBody()?.close()
        assertEquals(0, invalidations)
        assertEquals(AuthSessionSource.MOCK, sessions.read()?.source)

        sessions.save(session(AuthSessionSource.BACKEND))
        server.enqueue(MockResponse().setResponseCode(401).setBody("{\"code\":\"AUTH401\"}"))
        api.publicCall().errorBody()?.close()
        assertEquals(0, invalidations)
        assertTrue(sessions.read() != null)

        sessions.save(session(AuthSessionSource.BACKEND))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"code\":\"AUTH401\"}"))
        api.protectedCall().body()?.close()
        assertEquals(1, invalidations)
        assertNull(sessions.read())

        sessions.save(session(AuthSessionSource.BACKEND))
        server.enqueue(MockResponse().setResponseCode(401).setBody("unauthorized"))
        api.protectedCall().errorBody()?.close()
        assertEquals(2, invalidations)
        assertNull(sessions.read())
    }

    @Test fun expiredBackendTokenIsNotAttachedAndIsClearedFromTheSession() = runTest {
        val expired = session(AuthSessionSource.BACKEND, System.currentTimeMillis() / 1_000L - 1L)
        sessions.forceSet(expired)
        server.enqueue(MockResponse().setResponseCode(200))
        api.protectedCall().body()?.close()
        assertNull(server.takeRequest().getHeader("Authorization"))
        assertEquals(1, invalidations)
        assertNull(sessions.read())
    }

    private fun session(
        source: AuthSessionSource,
        exp: Long = System.currentTimeMillis() / 1_000L + 600L,
    ): AuthSession {
        return AuthSession("${encode("{}")}.${encode("{\"exp\":$exp}")}.sig", source)
    }
    private fun encode(value: String) = Base64.encodeToString(
        value.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )

    private interface TestApi {
        @GET("/protected")
        @Headers("X-Dundeuni-Auth-Required: true")
        suspend fun protectedCall(): Response<ResponseBody>

        @GET("/public")
        suspend fun publicCall(): Response<ResponseBody>
    }

    private class MemorySessionStore : AuthSessionStore {
        private var session: AuthSession? = null
        override fun read() = session
        override fun save(session: AuthSession) { this.session = session }
        override fun clear() { session = null }
        fun forceSet(session: AuthSession) { this.session = session }
    }
}
