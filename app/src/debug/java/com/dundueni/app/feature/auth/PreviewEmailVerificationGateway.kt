package com.dundueni.app.feature.auth

import kotlinx.coroutines.delay

/** debug 미리보기만 사용하는 UI 샘플. 실제 메일·인증 API를 호출하지 않는다. */
class PreviewEmailVerificationGateway : EmailVerificationGateway {
    override suspend fun sendCode(email: String): SendCodeOutcome {
        delay(500)
        return SendCodeOutcome.Sent("preview-challenge", 180)
    }

    override suspend fun verifyCode(challengeId: String, code: String): VerifyCodeOutcome {
        delay(500)
        return if (code == "123456") VerifyCodeOutcome.Verified else VerifyCodeOutcome.Mismatch
    }
}
