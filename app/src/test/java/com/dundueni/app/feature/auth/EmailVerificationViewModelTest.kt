package com.dundueni.app.feature.auth

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var clock = 0L
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }
    private fun model(gateway: EmailVerificationGateway) = EmailVerificationViewModel(gateway) {
        clock + dispatcher.scheduler.currentTime
    }.also { store.put("email", it) }
    private fun gateway(verify: VerifyCodeOutcome = VerifyCodeOutcome.Mismatch) = object : EmailVerificationGateway {
        override suspend fun sendCode(email: String) = SendCodeOutcome.Sent("id", 180)
        override suspend fun verifyCode(challengeId: String, code: String) = verify
    }

    @Test fun invalidEmailDoesNotSendAndOtpOnlyAcceptsSixAsciiDigits() = runTest {
        var calls = 0
        val vm = model(object : EmailVerificationGateway {
            override suspend fun sendCode(email: String): SendCodeOutcome { calls++; return SendCodeOutcome.Unavailable }
            override suspend fun verifyCode(challengeId: String, code: String) = VerifyCodeOutcome.Unavailable
        })
        vm.changeEmail("example")
        vm.sendCode()
        runCurrent()
        assertEquals(0, calls)
        assertNotNull(vm.uiState.value.emailError)
        vm.changeCode("12a345")
        vm.changeCode("1234567")
        assertEquals("", vm.uiState.value.code)
        vm.changeCode("000001")
        assertEquals("000001", vm.uiState.value.code)
    }

    @Test fun timerUsesDeadlineAndResendingClearsOldInputAndError() = runTest {
        val vm = model(gateway())
        vm.changeEmail("example@kau.kr")
        vm.sendCode(); runCurrent()
        assertEquals("03:00", vm.uiState.value.timerText)
        clock = 70000
        vm.refreshTimer()
        assertEquals("01:50", vm.uiState.value.timerText)
        vm.changeCode("654321")
        vm.checkCode {}; runCurrent()
        assertEquals("인증번호가 일치하지 않습니다.", vm.uiState.value.codeError)
        vm.sendCode(); runCurrent()
        assertEquals("03:00", vm.uiState.value.timerText)
        assertEquals("", vm.uiState.value.code)
        assertNull(vm.uiState.value.codeError)
    }

    @Test fun expirationBlocksVerificationEvenIfBackgroundTickerDidNotRun() = runTest {
        val vm = model(gateway(VerifyCodeOutcome.Verified))
        vm.changeEmail("example@kau.kr"); vm.sendCode(); runCurrent()
        vm.changeCode("123456")
        clock = 180000
        vm.checkCode { fail("Expired code accepted") }; runCurrent()
        assertEquals("00:00", vm.uiState.value.timerText)
        assertFalse(vm.uiState.value.canContinue)
        assertTrue(vm.uiState.value.codeError!!.contains("만료"))
    }

    @Test fun emailChangeInvalidatesChallengeAndLateSendResponse() = runTest {
        val pending = CompletableDeferred<SendCodeOutcome>()
        val vm = model(object : EmailVerificationGateway {
            override suspend fun sendCode(email: String) = pending.await()
            override suspend fun verifyCode(challengeId: String, code: String) = VerifyCodeOutcome.Verified
        })
        vm.changeEmail("old@kau.kr"); vm.sendCode(); runCurrent()
        vm.changeEmail("new@kau.kr")
        pending.complete(SendCodeOutcome.Sent("old", 180)); runCurrent()
        assertEquals("new@kau.kr", vm.uiState.value.email)
        assertFalse(vm.uiState.value.hasSentCode)
        assertFalse(vm.uiState.value.busy)
    }

    @Test fun duplicateRequestsAreBlockedAndOnlyVerifiedResponseAllowsSuccess() = runTest {
        var calls = 0
        val pending = CompletableDeferred<SendCodeOutcome>()
        val vm = model(object : EmailVerificationGateway {
            override suspend fun sendCode(email: String): SendCodeOutcome { calls++; return pending.await() }
            override suspend fun verifyCode(challengeId: String, code: String) = VerifyCodeOutcome.Verified
        })
        vm.changeEmail("example@kau.kr"); vm.sendCode(); vm.sendCode(); runCurrent()
        assertEquals(1, calls)
        pending.complete(SendCodeOutcome.Sent("id", 180)); runCurrent()
        vm.changeCode("123456")
        var successes = 0
        vm.checkCode { successes++ }; runCurrent()
        assertEquals(1, successes)
        assertTrue(vm.uiState.value.verified)
        assertEquals("", vm.uiState.value.code)
        vm.changeEmail("another@kau.kr")
        assertFalse(vm.uiState.value.verified)
    }

    @Test fun unavailableApiNeverStartsTimerOrClaimsMailWasSent() = runTest {
        val vm = model(object : EmailVerificationGateway {
            override suspend fun sendCode(email: String) = SendCodeOutcome.Unavailable
            override suspend fun verifyCode(challengeId: String, code: String) = VerifyCodeOutcome.Unavailable
        })
        vm.changeEmail("example@kau.kr"); vm.sendCode(); runCurrent()
        assertFalse(vm.uiState.value.hasSentCode)
        assertFalse(vm.uiState.value.verified)
        assertNotNull(vm.uiState.value.notice)
    }

    @Test fun verifiedNavigationWaitsForScreenAndIsNotReplayedAfterConsumption() = runTest {
        val response = CompletableDeferred<VerifyCodeOutcome>()
        val vm = model(object : EmailVerificationGateway {
            override suspend fun sendCode(email: String) = SendCodeOutcome.Sent("id", 180)
            override suspend fun verifyCode(challengeId: String, code: String) = response.await()
        })
        vm.changeEmail("example@kau.kr"); vm.sendCode(); runCurrent()
        vm.changeCode("123456"); vm.checkCode {}; runCurrent()
        assertFalse(vm.uiState.value.navigationPending)
        response.complete(VerifyCodeOutcome.Verified); runCurrent()
        assertTrue(vm.uiState.value.navigationPending)
        vm.consumeNavigation()
        vm.refreshTimer()
        assertTrue(vm.uiState.value.verified)
        assertFalse(vm.uiState.value.navigationPending)
        // 3단계에서 뒤로 온 뒤 사용자가 다음을 누를 때만 다시 이동한다.
        vm.checkCode {}
        assertTrue(vm.uiState.value.navigationPending)
        vm.changeEmail("another@kau.kr")
        assertFalse(vm.uiState.value.navigationPending)
    }
}
