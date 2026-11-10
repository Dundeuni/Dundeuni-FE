package com.dundueni.app.feature.textinput

import android.app.Application
import androidx.annotation.MainThread
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dundueni.app.BuildConfig
import com.dundueni.app.data.remote.api.TextAnalysisRequest
import com.dundueni.app.feature.analysis.completedTextAnalysisId
import com.dundueni.app.feature.analysis.toTextResult
import com.dundueni.app.feature.analysis.AnalysisFlowDependencies
import com.dundueni.app.data.repository.AnalysisNotCompletedException
import com.dundueni.app.feature.analysisresult.AiLikelihood
import com.dundueni.app.feature.analysisresult.AnalysisResult
import com.dundueni.app.feature.analysisresult.RiskEvidence
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.feature.history.HistoryType
import com.google.gson.Gson
import java.util.UUID
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
    data class Pending(val analysisId: String) : TextInspectionUiState
    data object Error : TextInspectionUiState
}

/** Keeps the submitted text and inspection state across Activity recreation. */
class TextInspectionViewModel @JvmOverloads constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val apiMode: String = BuildConfig.ANALYSIS_API_MODE,
    private val textApi: () -> com.dundueni.app.data.remote.api.TextAnalysisApiService = {
        AnalysisFlowDependencies.createTextApiService(application)
    },
    private val repository: () -> com.dundueni.app.data.repository.AnalysisResultRepository = {
        AnalysisFlowDependencies.createRepository(application)
    },
) : AndroidViewModel(application) {
    private val mutableUiState = MutableStateFlow<TextInspectionUiState>(TextInspectionUiState.Idle)
    val uiState: StateFlow<TextInspectionUiState> = mutableUiState.asStateFlow()
    val inputText: StateFlow<String> = savedStateHandle.getStateFlow(INPUT_TEXT_KEY, "")
    private var requestJob: Job? = null
    private var submittedText: String? = null
    private var resultPendingOpen: String? = null
    private var pendingAnalysisId: String? = null
    val hasResultPendingOpen: Boolean get() = resultPendingOpen != null
    val hasPendingLookup: Boolean get() = pendingAnalysisId != null

    @MainThread
    fun updateInputText(text: String) {
        if (mutableUiState.value != TextInspectionUiState.Loading &&
            mutableUiState.value !is TextInspectionUiState.Pending) savedStateHandle[INPUT_TEXT_KEY] = text
    }

    @MainThread
    fun inspect(text: String) {
        if (text.isBlank() || requestJob?.isActive == true ||
            mutableUiState.value is TextInspectionUiState.Pending) return
        submittedText = text
        startInspection(text)
    }

    @MainThread
    fun retry() {
        if (mutableUiState.value !is TextInspectionUiState.Error &&
            mutableUiState.value !is TextInspectionUiState.Pending) return
        resultPendingOpen?.let {
            mutableUiState.value = TextInspectionUiState.Success(it)
            return
        }
        pendingAnalysisId?.let { refreshPending(it); return }
        submittedText?.let(::startInspection)
    }

    @MainThread
    fun cancelError() {
        if (mutableUiState.value == TextInspectionUiState.Error) {
            pendingAnalysisId = null
            mutableUiState.value = TextInspectionUiState.Idle
        }
    }

    @MainThread
    fun cancelPending() {
        if (mutableUiState.value is TextInspectionUiState.Pending) {
            pendingAnalysisId = null
            mutableUiState.value = TextInspectionUiState.Idle
        }
    }

    @MainThread
    fun onResultOpened() {
        resultPendingOpen = null
        mutableUiState.value = TextInspectionUiState.Idle
    }

    @MainThread
    fun onResultOpenFailure() {
        mutableUiState.value = TextInspectionUiState.Error
    }

    private fun startInspection(text: String) {
        if (requestJob?.isActive == true) return
        pendingAnalysisId = null
        resultPendingOpen = null
        mutableUiState.value = TextInspectionUiState.Loading
        requestJob = viewModelScope.launch {
            try {
                val result = when (apiMode.trim().uppercase()) {
                    "FAKE" -> withContext(Dispatchers.Default) { createMockResult(text) }
                    "REAL" -> inspectWithApi(text)
                    else -> error("Unknown analysis API mode")
                }
                val json = Gson().toJson(result)
                resultPendingOpen = json
                mutableUiState.value = TextInspectionUiState.Success(json)
            } catch (pending: AnalysisNotCompletedException) {
                pendingAnalysisId = pending.analysisId
                mutableUiState.value = TextInspectionUiState.Pending(requireNotNull(pending.analysisId))
            } catch (cancelled: CancellationException) {
                mutableUiState.value = TextInspectionUiState.Idle
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.value = TextInspectionUiState.Error
            }
        }
    }

    private suspend fun inspectWithApi(text: String): AnalysisResult {
        val id = textApi()
            .analyzeText(TextAnalysisRequest(text = text)).completedTextAnalysisId()
        pendingAnalysisId = id
        return lookupTextResult(id, text)
    }

    private suspend fun lookupTextResult(id: String, text: String): AnalysisResult {
        val detail = repository().findResult(id)
        if (detail.type != "TEXT" || detail.sourceType !in setOf(null, "TEXT", "DIRECT")) {
            error("Unexpected text analysis type")
        }
        val result = detail.toTextResult(text)
        pendingAnalysisId = null
        return result
    }

    private fun refreshPending(id: String) {
        if (requestJob?.isActive == true) return
        mutableUiState.value = TextInspectionUiState.Loading
        requestJob = viewModelScope.launch {
            try {
                val result = lookupTextResult(id, submittedText.orEmpty())
                val json = Gson().toJson(result)
                pendingAnalysisId = null
                resultPendingOpen = json
                mutableUiState.value = TextInspectionUiState.Success(json)
            } catch (pending: AnalysisNotCompletedException) {
                mutableUiState.value = TextInspectionUiState.Pending(id)
            } catch (cancelled: CancellationException) {
                mutableUiState.value = TextInspectionUiState.Idle
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.value = TextInspectionUiState.Error
            }
        }
    }

    private fun createMockResult(text: String) = AnalysisResult(
        historyType = HistoryType.TEXT,
        id = "text-scan-${UUID.randomUUID()}",
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

    private companion object {
        const val INPUT_TEXT_KEY = "inputText"
    }
}
