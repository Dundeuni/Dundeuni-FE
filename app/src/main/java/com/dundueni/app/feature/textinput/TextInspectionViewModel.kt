package com.dundueni.app.feature.textinput

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dundueni.app.feature.analysisresult.AiLikelihood
import com.dundueni.app.feature.analysisresult.AnalysisResult
import com.dundueni.app.feature.analysisresult.RiskEvidence
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface TextInspectionUiState {
    data object Idle : TextInspectionUiState
    data object Loading : TextInspectionUiState
    data class Success(val resultJson: String) : TextInspectionUiState
    data object Error : TextInspectionUiState
}

/** Keeps the submitted text and inspection state across Activity recreation. */
class TextInspectionViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow<TextInspectionUiState>(TextInspectionUiState.Idle)
    val uiState: StateFlow<TextInspectionUiState> = mutableUiState.asStateFlow()
    private val mutableInputText = MutableStateFlow("")
    val inputText: StateFlow<String> = mutableInputText.asStateFlow()
    private var requestJob: Job? = null
    private var submittedText: String? = null

    @MainThread
    fun updateInputText(text: String) {
        if (mutableUiState.value != TextInspectionUiState.Loading) mutableInputText.value = text
    }

    @MainThread
    fun inspect(text: String) {
        if (text.isBlank() || requestJob?.isActive == true) return
        submittedText = text
        startInspection(text)
    }

    @MainThread
    fun retry() {
        if (mutableUiState.value != TextInspectionUiState.Error) return
        submittedText?.let(::startInspection)
    }

    @MainThread
    fun cancelError() {
        if (mutableUiState.value == TextInspectionUiState.Error) {
            mutableUiState.value = TextInspectionUiState.Idle
        }
    }

    @MainThread
    fun onResultOpened() {
        mutableUiState.value = TextInspectionUiState.Idle
    }

    @MainThread
    fun onResultOpenFailure() {
        mutableUiState.value = TextInspectionUiState.Error
    }

    private fun startInspection(text: String) {
        if (requestJob?.isActive == true) return
        mutableUiState.value = TextInspectionUiState.Loading
        requestJob = viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.Default) { Gson().toJson(createMockResult(text)) }
                mutableUiState.value = TextInspectionUiState.Success(json)
            } catch (cancelled: CancellationException) {
                mutableUiState.value = TextInspectionUiState.Idle
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.value = TextInspectionUiState.Error
            }
        }
    }

    private fun createMockResult(text: String) = AnalysisResult(
        id = "text-scan-${System.currentTimeMillis()}",
        riskLevel = RiskLevel.CAUTION,
        summary = "입력하신 텍스트에 대한 분석 결과입니다 (Mock 데이터).",
        keyFinding = "작성하신 내용에서 주의가 필요한 표현이 감지되었습니다.",
        fraudScore = 50.0,
        aiLikelihood = AiLikelihood.MEDIUM,
        sourceText = text,
        evidence = listOf(
            RiskEvidence("입력 텍스트 점검 (Mock)", "사용자가 직접 입력한 텍스트 내용을 기반으로 사기 및 피싱 위험도를 가상 분석했습니다."),
            RiskEvidence("주의 필요 표현", "금전 요구나 개인정보 요청 문구가 있는지 주의 깊게 확인하세요."),
        ),
        targetLabel = "입력한 텍스트 내용",
    )
}
