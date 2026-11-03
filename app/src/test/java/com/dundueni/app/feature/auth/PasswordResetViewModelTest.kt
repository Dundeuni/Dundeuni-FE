package com.dundueni.app.feature.auth

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordResetViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun model(outcome: PasswordResetOutcome = PasswordResetOutcome.Changed,
        reset: (suspend (String, String) -> PasswordResetOutcome)? = null) = PasswordResetViewModel(object : PasswordResetGateway {
        override suspend fun sendCode(email: String) = SendCodeOutcome.Unavailable
        override suspend fun verifyCode(challengeId: String, code: String) = VerifyCodeOutcome.Unavailable
        override suspend fun resetPassword(grant: String, password: String) = reset?.invoke(grant, password) ?: outcome
    }).also { store.put("reset", it) }
    private fun valid(vm: PasswordResetViewModel) {
        vm.begin("example@kau.kr", "verification-grant")
        vm.changePassword("ValidPass123!"); vm.changeConfirmation("ValidPass123!")
    }
    @Test fun noGrantCannotOpenPasswordStepOrSubmit() = runTest {
        var calls = 0
        val vm = model(reset = { _, _ -> calls++; PasswordResetOutcome.Changed })
        vm.begin("example@kau.kr", null)
        vm.changePassword("ValidPass123!"); vm.changeConfirmation("ValidPass123!"); vm.submit(); runCurrent()
        assertFalse(vm.uiState.value.settingPassword)
        assertEquals(0, calls)
        val fresh = model()
        assertFalse(fresh.uiState.value.settingPassword)
    }
    @Test fun rulesAndMismatchBlockSubmissionUntilCorrected() = runTest {
        val vm = model(); vm.begin("example@kau.kr", "grant")
        vm.changePassword("abc"); vm.changeConfirmation("abc"); vm.submit(); runCurrent()
        assertFalse(vm.uiState.value.canSubmit)
        assertNotNull(vm.uiState.value.fields.passwordError)
        vm.changePassword("ValidPass123!"); vm.changeConfirmation("ValidPass321!")
        assertNotNull(vm.uiState.value.fields.confirmationError)
        assertFalse(vm.uiState.value.canSubmit)
        vm.changeConfirmation("ValidPass123!")
        assertTrue(vm.uiState.value.canSubmit)
    }
    @Test fun verifiedGrantIsPassedOnceAndSecretsAreClearedOnCompletion() = runTest {
        var calls = 0
        val pending = CompletableDeferred<PasswordResetOutcome>()
        val vm = model(reset = { grant, password ->
            calls++; assertEquals("verification-grant", grant); assertEquals("ValidPass123!", password); pending.await()
        })
        valid(vm); vm.submit(); vm.submit(); runCurrent()
        assertEquals(1, calls); assertTrue(vm.uiState.value.submitting)
        pending.complete(PasswordResetOutcome.Changed); runCurrent()
        assertTrue(vm.uiState.value.completed)
        assertEquals("", vm.uiState.value.fields.password)
        assertEquals("", vm.uiState.value.fields.confirmation)
        vm.submit(); runCurrent(); assertEquals(1, calls)
    }
    @Test fun unavailableAndFailedDoNotReportSuccessAndAllowRetry() = runTest {
        for (outcome in listOf(PasswordResetOutcome.Unavailable, PasswordResetOutcome.Failed)) {
            val vm = model(outcome); valid(vm); vm.submit(); runCurrent()
            assertFalse(vm.uiState.value.completed)
            assertNotNull(vm.uiState.value.message)
            assertTrue(vm.uiState.value.canSubmit)
        }
    }
    @Test fun expiredGrantClearsPasswordsAndRequiresEmailVerification() = runTest {
        val vm = model(PasswordResetOutcome.VerificationExpired); valid(vm); vm.submit(); runCurrent()
        assertTrue(vm.uiState.value.needsVerification)
        assertFalse(vm.uiState.value.canSubmit)
        assertEquals("", vm.uiState.value.fields.password)
        vm.returnToVerification()
        assertFalse(vm.uiState.value.settingPassword)
    }
    @Test fun backCancelsInFlightSubmissionAndDoesNotReopenCompletedStep() = runTest {
        val pending = CompletableDeferred<PasswordResetOutcome>()
        val vm = model(reset = { _, _ -> pending.await() }); valid(vm); vm.submit(); runCurrent()
        vm.returnToVerification(); pending.complete(PasswordResetOutcome.Changed); runCurrent()
        assertFalse(vm.uiState.value.completed)
        assertFalse(vm.uiState.value.settingPassword)
        assertEquals("", vm.uiState.value.fields.password)
    }
    @Test fun emailChangeInvalidatesResetVerificationGrant() = runTest {
        val verification = EmailVerificationViewModel(object : EmailVerificationGateway {
            override suspend fun sendCode(email: String) = SendCodeOutcome.Sent("challenge", 180)
            override suspend fun verifyCode(challengeId: String, code: String) = VerifyCodeOutcome.VerifiedForReset("grant")
        }, now = { dispatcher.scheduler.currentTime }).also { store.put("email", it) }
        verification.changeEmail("example@kau.kr"); verification.sendCode(); runCurrent()
        verification.changeCode("123456"); verification.checkCode {}; runCurrent()
        assertEquals("grant", verification.resetGrant)
        verification.changeEmail("new@kau.kr")
        assertNull(verification.resetGrant)
        assertFalse(verification.uiState.value.verified)
    }
}
