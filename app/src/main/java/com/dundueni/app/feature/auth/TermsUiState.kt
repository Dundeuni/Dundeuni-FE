package com.dundueni.app.feature.auth

enum class SignUpTerm(val title: String, val required: Boolean) {
    SERVICE("서비스 이용약관 동의", true),
    PRIVACY("개인정보 수집 동의", true),
    AGE("연령 확인 및 보호자 동의", true),
    MARKETING("마케팅 알림 동의", false),
}

data class TermsUiState(
    val service: Boolean = false,
    val privacy: Boolean = false,
    val age: Boolean = false,
    val marketing: Boolean = false,
) {
    val canContinue: Boolean get() = service && privacy && age
    val allSelected: Boolean get() = canContinue && marketing

    fun selected(term: SignUpTerm): Boolean = when (term) {
        SignUpTerm.SERVICE -> service
        SignUpTerm.PRIVACY -> privacy
        SignUpTerm.AGE -> age
        SignUpTerm.MARKETING -> marketing
    }

    fun setSelected(term: SignUpTerm, selected: Boolean): TermsUiState = when (term) {
        SignUpTerm.SERVICE -> copy(service = selected)
        SignUpTerm.PRIVACY -> copy(privacy = selected)
        SignUpTerm.AGE -> copy(age = selected)
        SignUpTerm.MARKETING -> copy(marketing = selected)
    }

    fun setAll(selected: Boolean) = TermsUiState(selected, selected, selected, selected)
}
