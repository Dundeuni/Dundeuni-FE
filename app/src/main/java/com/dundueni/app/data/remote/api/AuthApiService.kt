package com.dundueni.app.data.remote.api

import com.dundueni.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Base URL is the server origin; all draft PB-09 paths are centralized here. */
object AuthApiPaths {
    // TODO(BE_CONTRACT): detail uses email-codes; the API index uses email/code.
    const val EMAIL_CODES = "api/auth/email-codes"
    const val VERIFY_EMAIL_CODE = "api/auth/email-codes/verify"
    const val SIGNUP = "api/auth/signup"
    const val LOGIN = "api/auth/login"
    const val TERMS = "api/terms"
}

interface AuthApiService {
    @POST(AuthApiPaths.EMAIL_CODES)
    suspend fun sendEmailCode(@Body request: EmailCodeRequest): Response<AuthEnvelope<EmailCodeResult>>

    @POST(AuthApiPaths.VERIFY_EMAIL_CODE)
    suspend fun verifyEmailCode(@Body request: VerifyEmailCodeRequest): Response<AuthEnvelope<VerifyEmailCodeResult>>

    @POST(AuthApiPaths.SIGNUP)
    suspend fun signUp(@Body request: SignUpRequest): Response<AuthEnvelope<AuthTokenResult>>

    @POST(AuthApiPaths.LOGIN)
    suspend fun login(@Body request: LoginRequest): Response<AuthEnvelope<AuthTokenResult>>

    @GET(AuthApiPaths.TERMS)
    suspend fun terms(): Response<AuthEnvelope<TermsResult>>
}
