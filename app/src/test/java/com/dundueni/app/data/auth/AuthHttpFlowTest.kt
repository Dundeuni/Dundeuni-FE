package com.dundueni.app.data.auth

import android.os.SystemClock
import androidx.lifecycle.ViewModelStore
import com.dundueni.app.data.remote.network.BearerAuthInterceptor
import com.dundueni.app.data.remote.network.RetrofitClient
import com.dundueni.app.feature.auth.*
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AuthHttpFlowTest {
    @Test fun termsVerificationSignupPersistenceAndProtected401WorkTogether() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val models = ViewModelStore()
        val server = MockWebServer().apply { start() }
        try {
            val serviceId = "8c1b24c0-3186-4ee5-a679-74acb055b001"
            val privacyId = "a65d2da5-8ec3-4f89-b066-0f24ce940001"
            val epoch = Instant.parse("2030-01-01T00:00:00Z").toEpochMilli()
            fun enqueue(result: String, status: Int = 200) {
                server.enqueue(MockResponse().setResponseCode(status).setBody(
                    """{"isSuccess":true,"code":"AUTH$status","message":"ok","result":$result}"""))
            }
            val gateway = HttpAuthGateway(RetrofitClient(server.url("/").toString()).authApiService, { epoch }, SystemClock::elapsedRealtime)
            enqueue("""{"terms":[
                {"termsId":"$serviceId","type":"SERVICE","version":"1.4","title":"서비스","required":true,"content":"서비스 본문"},
                {"termsId":"$privacyId","type":"PRIVACY","version":"2.1","title":"개인정보","required":true,"content":"개인정보 본문"}]}""")
            val terms = SignUpTermsViewModel(gateway).also { models.put("terms", it) }
            withTimeout(5_000) { terms.uiState.first { it.ready } }
            terms.select(SignUpTerm.SERVICE, true)
            assertNull(terms.beginFlow())
            terms.select(SignUpTerm.PRIVACY, true)
            val flow = requireNotNull(terms.beginFlow())
            assertNull(SignUpFlowMemory.authorization(flow))

            val verification = EmailVerificationViewModel(gateway).also { models.put("email", it) }
            enqueue("""{"verificationId":"$serviceId","expiresAt":"2030-01-01T00:03:00Z","resendAt":"2030-01-01T00:01:00Z"}""", 202)
            verification.changeEmail("user@example.com"); verification.sendCode()
            withTimeout(5_000) { verification.uiState.first { it.hasSentCode } }
            enqueue("""{"emailVerificationToken":"private-verification-token","expiresAt":"2030-01-01T00:10:00Z"}""")
            verification.changeCode("123456"); verification.checkCode {}
            withTimeout(5_000) { verification.uiState.first { it.verified } }
            assertTrue(SignUpFlowMemory.verify(flow, "user@example.com", requireNotNull(verification.signUpGrant)))
            verification.consumeNavigation()

            fun encode(text: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray())
            val token = "${encode("{}")}.${encode("""{"exp":1893459600,"sub":"$serviceId"}""")}.signature"
            enqueue("""{"user":{"userId":"$serviceId","email":"user@example.com","nickname":"테스트"},"accessToken":"$token","tokenType":"Bearer","accessTokenExpiresAt":"2030-01-01T01:00:00Z"}""", 201)
            val store = object : AuthSessionStore {
                var session: AuthSession? = null
                override fun read() = session
                override fun save(session: AuthSession) { this.session = session }
                override fun clear() { session = null }
            }
            val submitter = HttpSignUpSubmitter(gateway, { SignUpFlowMemory.authorization(flow) }, { SignUpFlowMemory.clearVerification(flow) })
            val signup = SignUpCredentialsViewModel("user@example.com", false, submitter, AuthInputPolicy.DRAFT, store::save)
                .also { models.put("signup", it) }
            signup.changePassword("abcdefgh"); signup.changeConfirmation("abcdefgh"); signup.changeNickname("테스트")
            signup.submit()
            withTimeout(5_000) { signup.uiState.first { it.completed } }
            assertEquals(token, store.read()?.accessToken)
            assertEquals(false, store.read()?.onboardingCompleted)
            assertNull(SignUpFlowMemory.authorization(flow))
            assertEquals("", signup.uiState.value.password)
            val requests = List(4) { server.takeRequest() }
            assertEquals(listOf("/api/terms", "/api/auth/email-codes", "/api/auth/email-codes/verify", "/api/auth/signup"), requests.map { it.path })
            requests.forEach { assertNull(it.getHeader("Authorization")) }
            val body = JsonParser.parseString(requests.last().body.readUtf8()).asJsonObject
            assertEquals("private-verification-token", body["emailVerificationToken"].asString)
            assertEquals("1.4", body["agreements"].asJsonArray[0].asJsonObject["version"].asString)
            assertEquals("2.1", body["agreements"].asJsonArray[1].asJsonObject["version"].asString)
            var invalidations = 0
            val protectedClient = OkHttpClient.Builder().addInterceptor(BearerAuthInterceptor(store, { invalidations++ }, { epoch / 1000 })).build()
            server.enqueue(MockResponse().setResponseCode(401).setBody("""{"isSuccess":false,"code":"AUTH401","result":null}"""))
            protectedClient.newCall(Request.Builder().url(server.url("/protected"))
                .header(BearerAuthInterceptor.REQUIRES_AUTH_HEADER, "true").build()).execute().use { assertEquals(401, it.code) }
            assertEquals("Bearer $token", server.takeRequest().getHeader("Authorization"))
            assertNull(store.read())
            assertEquals(1, invalidations)
            models.clear()
            assertFalse(SignUpFlowMemory.contains(flow))
        } finally {
            models.clear()
            server.shutdown()
            Dispatchers.resetMain()
        }
    }
}
