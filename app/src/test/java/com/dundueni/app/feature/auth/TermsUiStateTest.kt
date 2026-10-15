package com.dundueni.app.feature.auth

import org.junit.Assert.*
import org.junit.Test

class TermsUiStateTest {
    @Test fun optionalConsentIsNotRequiredAndMissingRequiredConsentBlocksNext() {
        val required = TermsUiState(service = true, privacy = true, age = true)
        assertTrue(required.canContinue)
        assertFalse(required.marketing)
        assertFalse(required.allSelected)
        assertFalse(required.copy(privacy = false, marketing = true).canContinue)
        assertFalse(TermsUiState().canContinue)
    }

    @Test fun allConsentCanBeSelectedAndClearedWithoutLeavingOptionalConsentBehind() {
        val all = TermsUiState().setAll(true)
        assertTrue(all.allSelected)
        assertTrue(all.marketing)
        val partial = all.setSelected(SignUpTerm.MARKETING, false)
        assertFalse(partial.allSelected)
        assertTrue(partial.canContinue)
        assertEquals(TermsUiState(), partial.setAll(false))
    }
}
