package com.dundueni.app.data.auth

import android.os.SystemClock
import com.dundueni.app.data.remote.api.AuthApiService
import com.dundueni.app.data.remote.dto.*
import com.dundueni.app.feature.auth.*
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.ceil

/** No logging, automatic POST retries, sample fallback, or password reset endpoints. */
class HttpAuthGateway(
    private val api: AuthApiService,
    private val epochMillis: () -> Long = System::currentTimeMillis,
    private val elapsedMillis: () -> Long = SystemClock::elapsedRealtime,
) : LoginSubmitter, EmailVerificationGateway, TermsGateway {
    override suspend fun submit(email: String, password: String): LoginOutcome = try {
        val response = api.login(LoginRequest(email, password))
        val result = response.result(200, "AUTH200")
        requireNotNull(result.onboardingCompleted)
        LoginOutcome.Success(result.session(email, result.onboardingCompleted))
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (failure: AuthHttpException) {
        if (failure.status == 401 && failure.apiCode == "AUTH401") LoginOutcome.InvalidCredentials
        else LoginOutcome.Rejected(failure.reason)
    } catch (_: IOException) { LoginOutcome.ConnectionError }
    catch (_: Exception) { LoginOutcome.Rejected(AuthFailure.CONTRACT) }

    override suspend fun sendCode(email: String): SendCodeOutcome = try {
        val response = api.sendEmailCode(EmailCodeRequest(email))
        val result = response.result(202, "AUTH202")
        val id = requireNotNull(result.verificationId).also(::requireUuid)
        val reference = response.serverTimeMillis()
        val remaining = Instant.parse(result.expiresAt).toEpochMilli() - reference
        val resend = (Instant.parse(result.resendAt).toEpochMilli() - reference).coerceAtLeast(0)
        require(remaining > 0 && remaining <= Int.MAX_VALUE.toLong() * 1000 && resend <= Int.MAX_VALUE.toLong() * 1000)
        SendCodeOutcome.Sent(id, ceil(remaining / 1000.0).toInt(),
            resendAfterSeconds = ceil(resend / 1000.0).toInt(),
            expiresAtElapsedMillis = elapsedMillis() + remaining,
            resendAtElapsedMillis = elapsedMillis() + resend)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (failure: AuthHttpException) { SendCodeOutcome.Failed(failure.reason.message, failure.retryAfterSeconds) }
    catch (_: IOException) { SendCodeOutcome.Failed(AuthFailure.NETWORK.message) }
    catch (_: Exception) { SendCodeOutcome.Failed(AuthFailure.CONTRACT.message) }

    override suspend fun verifyCode(challengeId: String, code: String): VerifyCodeOutcome = try {
        val response = api.verifyEmailCode(VerifyEmailCodeRequest(challengeId, code))
        val result = response.result(200, "AUTH200")
        val token = requireNotNull(result.emailVerificationToken).also { require(it.isNotBlank()) }
        val remaining = Instant.parse(result.expiresAt).toEpochMilli() - response.serverTimeMillis()
        require(remaining > 0)
        VerifyCodeOutcome.VerifiedForSignUp(token, elapsedMillis() + remaining)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (failure: AuthHttpException) {
        when {
            failure.status == 422 && failure.apiCode == "AUTH422" -> VerifyCodeOutcome.Mismatch
            failure.status == 410 && failure.apiCode == "AUTH410" -> VerifyCodeOutcome.Expired
            else -> VerifyCodeOutcome.Failed(failure.reason, failure.retryAfterSeconds)
        }
    } catch (_: IOException) { VerifyCodeOutcome.Failed(AuthFailure.NETWORK) }
    catch (_: Exception) { VerifyCodeOutcome.Failed(AuthFailure.CONTRACT) }

    override suspend fun loadTerms(): TermsOutcome = try {
        val terms = requireNotNull(api.terms().result(200, "AUTH200").terms).map {
            SignUpTermsDocument(
                id = requireNotNull(it.termsId).also(::requireUuid),
                type = SignUpTerm.valueOf(requireNotNull(it.type)).also { type ->
                    require(type == SignUpTerm.SERVICE || type == SignUpTerm.PRIVACY)
                },
                version = requireNotNull(it.version).also { value -> require(value.isNotBlank()) },
                title = requireNotNull(it.title).also { value -> require(value.isNotBlank()) },
                content = requireNotNull(it.content).also { value -> require(value.isNotBlank()) },
                required = requireNotNull(it.required),
            )
        }
        require(terms.distinctBy { it.id }.size == terms.size && terms.distinctBy { it.type }.size == terms.size)
        TermsOutcome.Loaded(terms)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (failure: AuthHttpException) { TermsOutcome.Failed(failure.reason) }
    catch (_: IOException) { TermsOutcome.Failed(AuthFailure.NETWORK) }
    catch (_: Exception) { TermsOutcome.Failed(AuthFailure.CONTRACT) }

    suspend fun signUp(request: SignUpRequest): SignUpOutcome = try {
        val result = api.signUp(request).result(201, "AUTH201")
        // TODO(BE_CONTRACT): signup omits onboardingCompleted; new accounts start onboarding.
        SignUpOutcome.Authenticated(result.session(request.email, false))
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (failure: AuthHttpException) {
        val reason = when {
            failure.status == 409 && failure.apiCode == "AUTH409" -> AuthFailure.EMAIL_TAKEN
            failure.status == 403 && failure.apiCode == "AUTH403" -> AuthFailure.TERMS_REQUIRED
            failure.status == 410 && failure.apiCode == "AUTH410" -> AuthFailure.VERIFICATION_EXPIRED
            else -> failure.reason
        }
        SignUpOutcome.Rejected(reason)
    } catch (_: IOException) { SignUpOutcome.Rejected(AuthFailure.NETWORK) }
    catch (_: Exception) { SignUpOutcome.Rejected(AuthFailure.CONTRACT) }

    private fun AuthTokenResult.session(expectedEmail: String, onboarding: Boolean): AuthSession {
        require(tokenType == "Bearer")
        val user = requireNotNull(user)
        requireUuid(requireNotNull(user.userId))
        require(user.email == expectedEmail && !user.nickname.isNullOrBlank())
        val token = requireNotNull(accessToken)
        require(Regex("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+").matches(token))
        val expiry = Instant.parse(accessTokenExpiresAt).epochSecond
        return AuthSession(token, AuthSessionSource.BACKEND, expiry, onboarding).also {
            require(it.isValid(epochMillis() / 1000))
        }
    }

    private fun <T> Response<AuthEnvelope<T>>.result(status: Int, code: String): T {
        val envelope = if (isSuccessful) body() else errorBody()?.use {
            runCatching { Gson().fromJson(it.charStream(), AuthEnvelope::class.java) }.getOrNull()
        }
        if (code() != status || envelope?.isSuccess != true || envelope.code != code || envelope.result == null) {
            throw AuthHttpException(code(), envelope?.code, retryAfterSeconds())
        }
        return requireNotNull(body()?.result)
    }

    // HTTP Date avoids device clock skew. If absent, compare the ISO deadline to local UTC.
    private fun Response<*>.serverTimeMillis(): Long = headers()["Date"]?.let {
        runCatching { ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }.getOrNull()
    } ?: epochMillis()

    private fun Response<*>.retryAfterSeconds(): Int? = headers()["Retry-After"]?.let { value ->
        value.toLongOrNull()?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt()
            ?: runCatching {
                ceil((ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - serverTimeMillis()).coerceAtLeast(0) / 1000.0)
                    .coerceAtMost(Int.MAX_VALUE.toDouble()).toInt()
            }.getOrNull()
    }
}

private fun requireUuid(value: String) { require(UUID.fromString(value).toString().equals(value, ignoreCase = true)) }

private class AuthHttpException(val status: Int, val apiCode: String?, val retryAfterSeconds: Int?) : Exception() {
    val reason: AuthFailure get() = when (status) {
        400 -> AuthFailure.INVALID_REQUEST
        429 -> AuthFailure.RATE_LIMITED
        503 -> AuthFailure.MAIL_UNAVAILABLE
        in 500..599 -> AuthFailure.SERVER
        else -> AuthFailure.CONTRACT
    }
}

class HttpSignUpSubmitter(
    private val gateway: HttpAuthGateway,
    private val authorization: () -> SignUpAuthorization?,
    private val consumeAuthorization: () -> Unit,
    private val elapsedMillis: () -> Long = SystemClock::elapsedRealtime,
) : SignUpSubmitter {
    override suspend fun submit(email: String, password: String, nickname: String, marketingAgreed: Boolean): SignUpOutcome {
        val grant = authorization() ?: return SignUpOutcome.Rejected(AuthFailure.VERIFICATION_EXPIRED)
        if (grant.email != email || grant.token.isBlank() || grant.expiresAtElapsedMillis <= elapsedMillis()) {
            consumeAuthorization()
            return SignUpOutcome.Rejected(AuthFailure.VERIFICATION_EXPIRED)
        }
        if (grant.agreements.isEmpty() || grant.agreements.any { !it.agreed }) return SignUpOutcome.Rejected(AuthFailure.TERMS_REQUIRED)
        // TODO(BE_CONTRACT): marketing/age have no fields in the draft; never invent term IDs.
        val outcome = gateway.signUp(SignUpRequest(email, password, nickname, grant.token, grant.agreements))
        if (outcome is SignUpOutcome.Authenticated || outcome == SignUpOutcome.Rejected(AuthFailure.VERIFICATION_EXPIRED)) consumeAuthorization()
        return outcome
    }
}
