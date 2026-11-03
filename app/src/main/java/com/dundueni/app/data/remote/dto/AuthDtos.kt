package com.dundueni.app.data.remote.dto

// Nullable response fields deliberately distinguish missing fields from valid defaults.
class AuthEnvelope<T>(val isSuccess: Boolean?, val code: String?, val message: String?, val result: T?)
class EmailCodeRequest(val email: String)
class VerifyEmailCodeRequest(val verificationId: String, val code: String)
class LoginRequest(val email: String, val password: String)
class SignUpRequest(
    val email: String,
    val password: String,
    val nickname: String,
    val emailVerificationToken: String,
    val agreements: List<TermAgreement>,
)
data class TermAgreement(val termsId: String, val version: String, val agreed: Boolean)
class EmailCodeResult(val verificationId: String?, val expiresAt: String?, val resendAt: String?)
class VerifyEmailCodeResult(val emailVerificationToken: String?, val expiresAt: String?)
class AuthUserDto(val userId: String?, val email: String?, val nickname: String?)
class AuthTokenResult(
    val user: AuthUserDto?,
    val accessToken: String?,
    val tokenType: String?,
    val accessTokenExpiresAt: String?,
    val onboardingCompleted: Boolean?,
)
class TermsResult(val terms: List<TermDto>?)
class TermDto(
    val termsId: String?, val type: String?, val version: String?,
    val title: String?, val required: Boolean?, val content: String?,
)
// Secret-bearing DTOs are intentionally ordinary classes: no generated toString/copy.
