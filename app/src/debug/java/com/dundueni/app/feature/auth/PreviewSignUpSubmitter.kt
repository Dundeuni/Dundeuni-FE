package com.dundueni.app.feature.auth

import kotlinx.coroutines.delay

/** UI 확인 전용. 서버 요청이나 계정·토큰 생성 없이 샘플 완료 상태를 반환한다. */
class PreviewSignUpSubmitter : SignUpSubmitter {
    override suspend fun submit(email: String, password: String, nickname: String, marketingAgreed: Boolean): SignUpOutcome {
        delay(700)
        return SignUpOutcome.Registered
    }
}
