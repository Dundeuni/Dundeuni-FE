package com.dundueni.app.data.auth

import com.dundueni.app.data.remote.api.AuthApiService
import com.dundueni.app.data.remote.dto.*
import com.dundueni.app.data.remote.network.RetrofitClient
import com.dundueni.app.feature.auth.*
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.util.Base64
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class HttpAuthGatewayTest {
    private val server = MockWebServer().apply { start() }
    private val now = Instant.parse("2030-01-01T00:00:00Z").toEpochMilli()
    private val sessions = object : AuthSessionStore {
        var value: AuthSession? = null
        override fun read() = value
        override fun save(session: AuthSession) { value = session }
        override fun clear() { value = null }
    }
    private var invalidations = 0
    private val api = RetrofitClient(server.url("/").toString(), sessions) { invalidations++ }.authApiService
    private val gateway = HttpAuthGateway(api, { now }, { 1_000L })
    private val email = "user@example.com"
    private val id = "8c1b24c0-3186-4ee5-a679-74acb055b001"
    private val privacyId = "a65d2da5-8ec3-4f89-b066-0f24ce940001"

    @After fun close() = server.shutdown()

    private fun success(result: String, status: Int = 200, code: String = "AUTH200") =
        MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json")
            .setBody("""{"isSuccess":true,"code":"$code","message":"ok","result":$result}""")
    private fun error(status: Int, code: String) = MockResponse().setResponseCode(status)
        .setBody("""{"isSuccess":false,"code":"$code","message":"never reflect password token or code","result":null}""")
    private fun token(expiry: Long = now / 1000 + 3600): String {
        fun encode(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
        return "${encode("""{"alg":"HS256"}""")}.${encode("""{"sub":"$id","exp":$expiry}""")}.signature"
    }
    private fun authResult(onboarding: String = ",\"onboardingCompleted\":true", accessToken: String = token(), expiry: String = "2030-01-01T01:00:00Z") =
        """{"user":{"userId":"$id","email":"$email","nickname":"테스트"},"accessToken":"$accessToken","tokenType":"Bearer","accessTokenExpiresAt":"$expiry"$onboarding}"""
    private fun grant(emailValue: String = email, deadline: Long = 60_000) = SignUpAuthorization(emailValue, "one-use-token", deadline,
        listOf(TermAgreement(id, "1.2", true), TermAgreement(privacyId, "2.0", true)))
    private fun submitter(value: SignUpAuthorization? = grant(), consumed: () -> Unit = {}) =
        HttpSignUpSubmitter(gateway, { value }, consumed, { 1_000L })

    @Test fun loginPostsExactFieldsAndReturnsBackendSessionWithOnboarding() = runBlocking {
        server.enqueue(success(authResult()))
        val result = gateway.submit(email, " password preserved ") as LoginOutcome.Success
        assertEquals(AuthSessionSource.BACKEND, result.session.source)
        assertEquals(true, result.session.onboardingCompleted)
        assertEquals(now / 1000 + 3600, result.session.expiresAtEpochSeconds())
        val request = server.takeRequest()
        assertEquals("/api/auth/login", request.path)
        assertEquals("POST", request.method)
        val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertEquals(setOf("email", "password"), body.keySet())
        assertEquals(" password preserved ", body["password"].asString)
        assertNull(request.getHeader("Authorization"))
        assertNull(request.getHeader("X-Dundeuni-Auth-Required"))
        assertFalse(result.toString().contains(result.session.accessToken))
    }

    @Test fun loginRequiresOnboardingAndRejectsMalformedExpiredOrWrongUserTokens() = runBlocking {
        val invalid = listOf(authResult(onboarding = ""), authResult(accessToken = "not-jwt"),
            authResult(accessToken = token(now / 1000 - 1)), authResult(expiry = "2029-12-31T23:59:59Z"),
            authResult().replace(email, "another@example.com"), authResult().replace("Bearer", "Basic"))
        invalid.forEach {
            server.enqueue(success(it))
            assertEquals(LoginOutcome.Rejected(AuthFailure.CONTRACT), gateway.submit(email, "password"))
        }
    }

    @Test fun loginFalseOnboardingAndEarlierServerExpiryArePreserved() = runBlocking {
        server.enqueue(success(authResult(onboarding = ",\"onboardingCompleted\":false", expiry = "2030-01-01T00:30:00Z")))
        val result = gateway.submit(email, "password") as LoginOutcome.Success
        assertEquals(false, result.session.onboardingCompleted)
        assertEquals(now / 1000 + 1800, result.session.expiresAtEpochSeconds())
    }

    @Test fun login401IsCredentialsFailureAndDoesNotInvalidateExistingSession() = runBlocking {
        sessions.save(AuthSession(token(), AuthSessionSource.BACKEND))
        server.enqueue(error(401, "AUTH401"))
        assertEquals(LoginOutcome.InvalidCredentials, gateway.submit(email, "password"))
        assertNotNull(sessions.read())
        assertEquals(0, invalidations)
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test fun sendUsesServerDatesAndReturnsBothDeadlinesEvenWithDeviceClockSkew() = runBlocking {
        server.enqueue(success("""{"verificationId":"$id","expiresAt":"2030-01-01T00:03:00Z","resendAt":"2030-01-01T00:01:00Z"}""", 202, "AUTH202")
            .setHeader("Date", "Tue, 1 Jan 2030 00:00:10 GMT"))
        val skewed = HttpAuthGateway(api, { now + 86_400_000L }, { 10_000L })
        val result = skewed.sendCode(email) as SendCodeOutcome.Sent
        assertEquals(170, result.validForSeconds)
        assertEquals(50, result.resendAfterSeconds)
        assertEquals(180_000L, result.expiresAtElapsedMillis)
        assertEquals(60_000L, result.resendAtElapsedMillis)
        val request = server.takeRequest()
        assertEquals("/api/auth/email-codes", request.path)
        assertEquals("""{"email":"$email"}""", request.body.readUtf8())
    }

    @Test fun verificationReturnsPrivateGrantAndPostsOnlyIdAndCode() = runBlocking {
        server.enqueue(success("""{"emailVerificationToken":"verified-secret","expiresAt":"2030-01-01T00:10:00Z"}"""))
        val result = gateway.verifyCode(id, "012345") as VerifyCodeOutcome.VerifiedForSignUp
        assertEquals("verified-secret", result.token)
        assertEquals(601_000L, result.expiresAtElapsedMillis)
        assertFalse(result.toString().contains(result.token))
        val request = server.takeRequest()
        assertEquals("/api/auth/email-codes/verify", request.path)
        assertEquals("""{"verificationId":"$id","code":"012345"}""", request.body.readUtf8())
    }

    @Test fun verificationMismatchExpiryAndAttemptLimitAreDistinct() = runBlocking {
        server.enqueue(error(422, "AUTH422"))
        assertEquals(VerifyCodeOutcome.Mismatch, gateway.verifyCode(id, "123456"))
        server.enqueue(error(410, "AUTH410"))
        assertEquals(VerifyCodeOutcome.Expired, gateway.verifyCode(id, "123456"))
        server.enqueue(error(429, "AUTH429").setHeader("Retry-After", "90"))
        assertEquals(VerifyCodeOutcome.Failed(AuthFailure.RATE_LIMITED, 90), gateway.verifyCode(id, "123456"))
    }

    @Test fun sendRateLimitMailOutageAndServerErrorsUseSafeMessages() = runBlocking {
        server.enqueue(error(429, "AUTH429").setHeader("Retry-After", "60"))
        assertEquals(SendCodeOutcome.Failed(AuthFailure.RATE_LIMITED.message, 60), gateway.sendCode(email))
        server.enqueue(error(503, "AUTH503"))
        assertEquals(SendCodeOutcome.Failed(AuthFailure.MAIL_UNAVAILABLE.message), gateway.sendCode(email))
        server.enqueue(error(500, "COMMON500"))
        assertEquals(SendCodeOutcome.Failed(AuthFailure.SERVER.message), gateway.sendCode(email))
        server.enqueue(error(400, "COMMON400"))
        assertEquals(SendCodeOutcome.Failed(AuthFailure.INVALID_REQUEST.message), gateway.sendCode(email))
    }

    @Test fun retryAfterHttpDateIsSupported() = runBlocking {
        server.enqueue(error(429, "AUTH429").setHeader("Date", "Tue, 1 Jan 2030 00:00:00 GMT")
            .setHeader("Retry-After", "Tue, 1 Jan 2030 00:01:30 GMT"))
        assertEquals(90, (gateway.sendCode(email) as SendCodeOutcome.Failed).retryAfterSeconds)
    }

    @Test fun signupForwardsExactTokenAndVersionedAgreementsAndConsumesOnce() = runBlocking {
        var current: SignUpAuthorization? = grant()
        var consumed = 0
        val submit = HttpSignUpSubmitter(gateway, { current }, { consumed++; current = null }, { 1000 })
        server.enqueue(success(authResult(onboarding = ""), 201, "AUTH201"))
        val result = submit.submit(email, "abcdefgh", "가".repeat(50), false) as SignUpOutcome.Authenticated
        assertEquals(false, result.session.onboardingCompleted)
        assertEquals(1, consumed)
        val request = server.takeRequest()
        assertEquals("/api/auth/signup", request.path)
        val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertEquals(setOf("email", "password", "nickname", "emailVerificationToken", "agreements"), body.keySet())
        assertEquals("one-use-token", body["emailVerificationToken"].asString)
        val agreements = body["agreements"].asJsonArray
        assertEquals(2, agreements.size())
        assertEquals(id, agreements[0].asJsonObject["termsId"].asString)
        assertEquals("1.2", agreements[0].asJsonObject["version"].asString)
        assertTrue(agreements[0].asJsonObject["agreed"].asBoolean)
        assertEquals(SignUpOutcome.Rejected(AuthFailure.VERIFICATION_EXPIRED), submit.submit(email, "abcdefgh", "tester", false))
        assertEquals(1, server.requestCount)
    }

    @Test fun missingExpiredOrDifferentEmailGrantBlocksHttp() = runBlocking {
        listOf(null, grant(deadline = 1000), grant(emailValue = "other@example.com")).forEach {
            assertEquals(SignUpOutcome.Rejected(AuthFailure.VERIFICATION_EXPIRED), submitter(it).submit(email, "password", "tester", false))
        }
        assertEquals(0, server.requestCount)
    }

    @Test fun signupDuplicateEmailTermsAndExpiredGrantErrorsRemainDistinct() = runBlocking {
        val cases = listOf(Triple(409, "AUTH409", AuthFailure.EMAIL_TAKEN), Triple(403, "AUTH403", AuthFailure.TERMS_REQUIRED),
            Triple(410, "AUTH410", AuthFailure.VERIFICATION_EXPIRED), Triple(400, "COMMON400", AuthFailure.INVALID_REQUEST),
            Triple(500, "COMMON500", AuthFailure.SERVER))
        cases.forEach { (status, code, expected) ->
            var consumed = false
            server.enqueue(error(status, code))
            assertEquals(SignUpOutcome.Rejected(expected), submitter(consumed = { consumed = true }).submit(email, "password", "tester", false))
            assertEquals(status == 410, consumed)
        }
    }

    @Test fun termsCarryServerIdsVersionsTitlesAndContentAndEmptyListIsValidTransport() = runBlocking {
        server.enqueue(success("""{"terms":[{"termsId":"$id","type":"SERVICE","version":"3.2","title":"약관","required":true,"content":"서버 약관 본문"}]}"""))
        val terms = (gateway.loadTerms() as TermsOutcome.Loaded).terms
        assertEquals("3.2", terms.single().version)
        assertEquals("서버 약관 본문", terms.single().content)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/terms", request.path)
        assertEquals(0, request.bodySize)
        server.enqueue(success("""{"terms":[]}"""))
        assertTrue((gateway.loadTerms() as TermsOutcome.Loaded).terms.isEmpty())
    }

    @Test fun nullEmptyMalformedOrContradictorySuccessEnvelopeNeverAuthenticates() = runBlocking {
        listOf("{}", "null", "not-json", """{"isSuccess":false,"code":"AUTH200","result":${authResult()}}""",
            """{"isSuccess":true,"code":"AUTH200","result":null}""",
            """{"isSuccess":true,"code":"WRONG","result":${authResult()}}""").forEach {
            server.enqueue(MockResponse().setResponseCode(200).setBody(it))
            assertFalse(gateway.submit(email, "password") is LoginOutcome.Success)
        }
        server.enqueue(success(authResult(), 201))
        assertFalse(gateway.submit(email, "password") is LoginOutcome.Success)
    }

    @Test fun malformedDatesAndEmptyGrantDoNotAdvanceVerification() = runBlocking {
        server.enqueue(success("""{"verificationId":"$id","expiresAt":"bad","resendAt":"2030-01-01T00:00:00Z"}""", 202, "AUTH202"))
        assertTrue(gateway.sendCode(email) is SendCodeOutcome.Failed)
        server.enqueue(success("""{"emailVerificationToken":"","expiresAt":"2030-01-01T00:10:00Z"}"""))
        assertEquals(VerifyCodeOutcome.Failed(AuthFailure.CONTRACT), gateway.verifyCode(id, "123456"))
        server.enqueue(success("""{"emailVerificationToken":"token","expiresAt":"2029-12-31T23:59:59Z"}"""))
        assertEquals(VerifyCodeOutcome.Failed(AuthFailure.CONTRACT), gateway.verifyCode(id, "123456"))
    }

    @Test fun disconnectedTransportMapsNetworkFailureWithoutAutomaticRetry() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertEquals(LoginOutcome.ConnectionError, gateway.submit(email, "password"))
        assertEquals(1, server.requestCount)
    }

    @Test fun timeoutMapsNetworkFailure() = runBlocking {
        val slowApi = Retrofit.Builder().baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().readTimeout(100, TimeUnit.MILLISECONDS).retryOnConnectionFailure(false).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(AuthApiService::class.java)
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        assertEquals(LoginOutcome.ConnectionError, HttpAuthGateway(slowApi).submit(email, "password"))
    }

    @Test fun redirectIsNotFollowedWithCredentials() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(307).setHeader("Location", server.url("/unexpected")))
        assertEquals(LoginOutcome.Rejected(AuthFailure.CONTRACT), gateway.submit(email, "password"))
        assertEquals(1, server.requestCount)
    }

    @Test fun cancellationPropagatesInsteadOfBecomingFailure() = runBlocking {
        val cancelledApi = object : AuthApiService by api {
            override suspend fun login(request: LoginRequest): retrofit2.Response<AuthEnvelope<AuthTokenResult>> = throw CancellationException()
        }
        try { HttpAuthGateway(cancelledApi).submit(email, "password"); fail("Cancellation swallowed") }
        catch (_: CancellationException) { }
    }

    @Test fun configurationRequiresExplicitHttpModeAndHttpsOrigin() {
        assertTrue(AuthDependencies.isHttpConfiguration("HTTP", "https://example.com/"))
        assertFalse(AuthDependencies.isHttpConfiguration("UNCONNECTED", "https://example.com/"))
        assertFalse(AuthDependencies.isHttpConfiguration("MOCK", "https://example.com/"))
        assertFalse(AuthDependencies.isHttpConfiguration("HTTP", "http://example.com/"))
        assertFalse(AuthDependencies.isHttpConfiguration("HTTP", "https://example.com/api/"))
        assertFalse(AuthDependencies.isHttpConfiguration("HTTP", "https://user:pass@example.com/"))
    }
}
