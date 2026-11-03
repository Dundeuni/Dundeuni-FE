package com.dundueni.app.feature.auth

import com.dundueni.app.data.remote.dto.TermAgreement

enum class AuthFailure(val message: String) {
    INVALID_REQUEST("입력 정보를 확인해 주세요."),
    RATE_LIMITED("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    MAIL_UNAVAILABLE("이메일 발송이 지연되고 있습니다. 잠시 후 다시 시도해 주세요."),
    SERVER("서버에서 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."),
    NETWORK("연결하지 못했습니다. 다시 시도해 주세요."),
    CONTRACT("응답을 확인하지 못했습니다. 잠시 후 다시 시도해 주세요."),
    EMAIL_TAKEN("이미 가입된 이메일입니다. 로그인해 주세요."),
    TERMS_REQUIRED("필수 약관을 확인하고 다시 동의해 주세요."),
    VERIFICATION_EXPIRED("인증이 만료되었습니다. 이메일 인증을 다시 진행해 주세요."),
}

data class SignUpTermsDocument(
    val id: String, val type: SignUpTerm, val version: String,
    val title: String, val content: String, val required: Boolean,
)
sealed interface TermsOutcome {
    data class Loaded(val terms: List<SignUpTermsDocument>) : TermsOutcome
    data class Failed(val reason: AuthFailure) : TermsOutcome
    data object Unavailable : TermsOutcome
}
fun interface TermsGateway { suspend fun loadTerms(): TermsOutcome }

/** Only held in process memory; never put the token in an Intent or SavedStateHandle. */
class SignUpAuthorization(
    val email: String,
    val token: String,
    val expiresAtElapsedMillis: Long,
    val agreements: List<TermAgreement>,
)

/** Draft policy is isolated from the older preview/reset design. */
enum class AuthInputPolicy(val passwordMax: Int, val nicknameMax: Int, val requiresCombination: Boolean) {
    // TODO(BE_CONTRACT): confirm 8..64 password policy and Unicode length rules.
    DRAFT(64, 50, false),
    PREVIEW_RESET(20, 10, true),
}
