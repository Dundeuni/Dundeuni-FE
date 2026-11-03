package com.dundueni.app.feature.auth

import android.os.SystemClock
import kotlinx.coroutines.delay
import java.util.UUID

/** Debug-only local sample: sends no email, changes no password, and creates no login session. */
class PreviewPasswordResetGateway : PasswordResetGateway {
    private var challenge: String? = null
    private var deadline = 0L
    private var grant: String? = null
    override suspend fun sendCode(email: String): SendCodeOutcome {
        delay(500)
        challenge = UUID.randomUUID().toString()
        grant = null
        deadline = SystemClock.elapsedRealtime() + 180_000
        return SendCodeOutcome.Sent(requireNotNull(challenge), 180)
    }
    override suspend fun verifyCode(challengeId: String, code: String): VerifyCodeOutcome {
        delay(500)
        if (challengeId != challenge || SystemClock.elapsedRealtime() >= deadline) return VerifyCodeOutcome.Expired
        if (code != "123456") return VerifyCodeOutcome.Mismatch
        grant = UUID.randomUUID().toString()
        return VerifyCodeOutcome.VerifiedForReset(requireNotNull(grant))
    }
    override suspend fun resetPassword(grant: String, password: String): PasswordResetOutcome {
        delay(500)
        if (grant != this.grant || SystemClock.elapsedRealtime() >= deadline) return PasswordResetOutcome.VerificationExpired
        this.grant = null
        return PasswordResetOutcome.Changed
    }
}
