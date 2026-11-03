package com.dundueni.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dundueni.app.data.remote.dto.TermAgreement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SignUpTermsLoadState(
    val terms: List<SignUpTermsDocument> = emptyList(),
    val selection: TermsUiState = TermsUiState(requiredTerms = setOf(SignUpTerm.SERVICE, SignUpTerm.PRIVACY)),
    val loading: Boolean = false,
    val message: String? = null,
) {
    val ready get() = terms.map { it.type }.toSet() == setOf(SignUpTerm.SERVICE, SignUpTerm.PRIVACY) && terms.all { it.required }
}

class SignUpTermsViewModel(private val gateway: TermsGateway) : ViewModel() {
    private val state = MutableStateFlow(SignUpTermsLoadState())
    val uiState = state.asStateFlow()
    private var flowId: String? = null
    init { load() }

    fun load() {
        if (state.value.loading) return
        SignUpFlowMemory.remove(flowId)
        flowId = null
        state.value = SignUpTermsLoadState(loading = true)
        viewModelScope.launch {
            val result = try { gateway.loadTerms() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { TermsOutcome.Failed(AuthFailure.NETWORK) }
            state.value = when (result) {
                is TermsOutcome.Loaded -> SignUpTermsLoadState(terms = result.terms).let {
                    if (it.ready) it else it.copy(message = "가입에 필요한 약관이 준비되지 않았습니다. 다시 시도해 주세요.")
                }
                is TermsOutcome.Failed -> SignUpTermsLoadState(message = result.reason.message)
                TermsOutcome.Unavailable -> SignUpTermsLoadState(message = "약관 조회 연결을 준비 중입니다.")
            }
        }
    }
    fun select(term: SignUpTerm, selected: Boolean) {
        if (!state.value.ready) return
        state.value = state.value.copy(selection = state.value.selection.setSelected(term, selected))
    }
    fun selectAll(selected: Boolean) {
        if (!state.value.ready) return
        state.value = state.value.copy(selection = state.value.selection.setAll(selected))
    }
    fun beginFlow(): String? {
        val current = state.value
        if (!current.ready || !current.selection.canContinue) return null
        SignUpFlowMemory.remove(flowId)
        return SignUpFlowMemory.create(current.terms.map { TermAgreement(it.id, it.version, current.selection.selected(it.type)) })
            .also { flowId = it }
    }
    override fun onCleared() { SignUpFlowMemory.remove(flowId) }
}
