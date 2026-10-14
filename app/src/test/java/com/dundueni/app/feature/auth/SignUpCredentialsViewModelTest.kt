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
class SignUpCredentialsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }
    private fun model(submitter: SignUpSubmitter) = SignUpCredentialsViewModel("example@kau.kr", false, submitter).also { store.put("credentials", it) }

    @Test fun passwordLengthCombinationAndMatchingAreRequiredButSpecialCharacterIsOptional() {
        val valid = SignUpCredentialsUiState(password = "ValidPass123", confirmation = "ValidPass123", nickname = "든든한지킴이")
        assertTrue(valid.canSubmit)
        assertFalse(valid.specialSatisfied)
        assertFalse(valid.copy(password = "abcdefghi", confirmation = "abcdefghi").canSubmit)
        assertFalse(valid.copy(password = "123456789", confirmation = "123456789").canSubmit)
        assertFalse(valid.copy(password = "Ab12345", confirmation = "Ab12345").canSubmit)
        assertFalse(valid.copy(password = "A1".repeat(11), confirmation = "A1".repeat(11)).canSubmit)
        assertFalse(valid.copy(confirmation = "ValidPass123!").canSubmit)
        assertTrue(valid.copy(password = "ValidPass123!", confirmation = "ValidPass123!").specialSatisfied)
    }

    @Test fun nicknameLengthCountsUnicodeCodePointsAndRejectsBlankOrOverLimit() {
        val state = SignUpCredentialsUiState(nickname = "든든한지킴이")
        assertEquals(6, state.nicknameLength)
        assertEquals(1, state.copy(nickname = "😀").nicknameLength)
        assertNotNull(state.copy(nickname = "가".repeat(11), nicknameTouched = true).nicknameError)
        assertNotNull(state.copy(nickname = "  ", nicknameTouched = true).nicknameError)
    }

    @Test fun pendingRegistrationBlocksDuplicateCallsAndCompletionClearsPasswords() = runTest {
        val response = CompletableDeferred<SignUpOutcome>()
        var calls = 0
        val vm = model { email, password, nickname, marketing ->
            calls++
            assertEquals("example@kau.kr", email)
            assertEquals("ValidPass123!", password)
            assertEquals("tester", nickname)
            assertFalse(marketing)
            response.await()
        }
        vm.changePassword("ValidPass123!"); vm.changeConfirmation("ValidPass123!"); vm.changeNickname("tester")
        vm.submit(); vm.submit(); runCurrent()
        assertEquals(1, calls)
        vm.changeNickname("replacement")
        assertEquals("tester", vm.uiState.value.nickname)
        response.complete(SignUpOutcome.Registered); runCurrent()
        assertTrue(vm.uiState.value.completed)
        assertEquals("", vm.uiState.value.password)
        assertEquals("", vm.uiState.value.confirmation)
        vm.submit(); runCurrent()
        assertEquals(1, calls)
    }

    @Test fun invalidFormDoesNotSubmitAndUnavailableApiDoesNotComplete() = runTest {
        var calls = 0
        val vm = model { _, _, _, _ -> calls++; SignUpOutcome.Unavailable }
        vm.submit(); runCurrent()
        assertEquals(0, calls)
        assertNotNull(vm.uiState.value.passwordError)
        vm.changePassword("ValidPass123!"); vm.changeConfirmation("ValidPass123!"); vm.changeNickname("tester")
        vm.submit(); runCurrent()
        assertFalse(vm.uiState.value.completed)
        assertFalse(vm.uiState.value.submitting)
        assertEquals("회원가입 연결을 준비 중입니다.", vm.uiState.value.message)
    }
}
