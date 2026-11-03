package com.dundueni.app.feature.auth

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.dundueni.app.ui.theme.DundueniFETheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SignUpTermsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun httpTermsShowOnlyServerDocumentsAndRequireBothConsents() {
        val state = mutableStateOf(TermsUiState(requiredTerms = setOf(SignUpTerm.SERVICE, SignUpTerm.PRIVACY)))
        val documents = listOf(
            SignUpTermsDocument("service", SignUpTerm.SERVICE, "1.2", "서버 서비스 약관", "서비스 본문", true),
            SignUpTermsDocument("privacy", SignUpTerm.PRIVACY, "2.1", "서버 개인정보 약관", "개인정보 본문", true),
        )
        compose.setContent { DundueniFETheme {
            SignUpTermsScreen(state.value, { term, selected -> state.value = state.value.setSelected(term, selected) },
                { state.value = state.value.setAll(it) }, {}, {}, {}, documents = documents)
        } }
        compose.onNodeWithText("다음 단계로").assertIsNotEnabled()
        compose.onNodeWithText("서버 서비스 약관").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(SignUpTerm.SERVICE.title).performClick()
        compose.onNodeWithText("다음 단계로").assertIsNotEnabled()
        compose.onNodeWithContentDescription(SignUpTerm.PRIVACY.title).performScrollTo().performClick()
        compose.onNodeWithText("다음 단계로").assertIsEnabled()
        compose.onAllNodesWithContentDescription(SignUpTerm.AGE.title).assertCountEquals(0)
        compose.onAllNodesWithContentDescription(SignUpTerm.MARKETING.title).assertCountEquals(0)
    }

    @Test fun selectingOnlyRequiredTermsEnablesNextAndUnselectingOneDisablesIt() {
        val state = mutableStateOf(TermsUiState())
        var next = 0
        compose.setContent { DundueniFETheme {
            SignUpTermsScreen(state.value,
                { term, selected -> state.value = state.value.setSelected(term, selected) },
                { state.value = state.value.setAll(it) }, {}, {}, { next++ })
        } }
        compose.onNodeWithText("다음 단계로").assertIsNotEnabled()
        SignUpTerm.entries.filter { it.required }.forEach {
            compose.onNodeWithContentDescription(it.title).performScrollTo().performClick()
        }
        compose.onNodeWithText("다음 단계로").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, next); assertFalse(state.value.marketing) }
        compose.onNodeWithContentDescription(SignUpTerm.PRIVACY.title).performScrollTo().performClick()
        compose.onNodeWithText("다음 단계로").assertIsNotEnabled()
    }

    @Test fun detailsAndBackDoNotChangeConsent() {
        var details: SignUpTerm? = null
        var back = 0
        compose.setContent { DundueniFETheme {
            SignUpTermsScreen(TermsUiState(), { _, _ -> fail("Consent was changed") },
                {}, { details = it }, { back++ }, {})
        } }
        compose.onNodeWithContentDescription("${SignUpTerm.SERVICE.title} 전문 보기").performScrollTo().performClick()
        compose.onNodeWithContentDescription("로그인으로 돌아가기").performClick()
        compose.runOnIdle { assertEquals(SignUpTerm.SERVICE, details); assertEquals(1, back) }
    }

    @Test fun allConsentCheckboxReflectsIndividualSelectionAndCanClearAll() {
        val state = mutableStateOf(TermsUiState())
        compose.setContent { DundueniFETheme {
            SignUpTermsScreen(state.value,
                { term, selected -> state.value = state.value.setSelected(term, selected) },
                { state.value = state.value.setAll(it) }, {}, {}, {})
        } }
        compose.onNodeWithContentDescription("약관 전체 동의하기").performScrollTo().performClick().assertIsOn()
        compose.onNodeWithContentDescription(SignUpTerm.MARKETING.title).performScrollTo().performClick().assertIsOff()
        compose.onNodeWithContentDescription("약관 전체 동의하기").performScrollTo().assertIsOff().performClick().assertIsOn()
        compose.onNodeWithContentDescription("약관 전체 동의하기").performClick().assertIsOff()
        compose.runOnIdle { assertEquals(TermsUiState(), state.value) }
    }
}
