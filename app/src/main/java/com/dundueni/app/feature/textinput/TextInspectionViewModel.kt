package com.dundueni.app.feature.textinput

import android.app.Application
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dundueni.app.data.remote.api.AnalysisHttpException
import com.dundueni.app.data.remote.api.AnalysisProtocolException
import com.dundueni.app.data.remote.api.TextAnalysisRequest
import com.dundueni.app.feature.analysis.completedTextAnalysisId
import com.dundueni.app.feature.analysis.toTextResult
import com.dundueni.app.feature.analysis.AnalysisFlowDependencies
import com.dundueni.app.data.repository.AnalysisNotCompletedException
import com.dundueni.app.data.repository.AnalysisFailedException
import java.io.IOException
import com.dundueni.app.feature.analysisresult.AnalysisResult
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TextInspectionUiState {
    data object Idle : TextInspectionUiState
    data object InvalidInput : TextInspectionUiState
    data object Loading : TextInspectionUiState
    data class Success(val resultJson: String) : TextInspectionUiState
    data class Pending(val analysisId: String) : TextInspectionUiState
    data object Error : TextInspectionUiState
}

/** Keeps the submitted text and inspection state across Activity recreation. */
class TextInspectionViewModel @JvmOverloads constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val textApi: () -> com.dundueni.app.data.remote.api.TextAnalysisApiService = {
        AnalysisFlowDependencies.createTextApiService(application)
    },
    private val repository: () -> com.dundueni.app.data.repository.AnalysisResultRepository = {
        AnalysisFlowDependencies.createRepository(application)
    },
) : AndroidViewModel(application) {
    val completionDelivery = com.dundueni.app.feature.analysis.InspectionCompletionDelivery()
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
            mutableUiState.value !is TextInspectionUiState.Pending) {
            savedStateHandle[INPUT_TEXT_KEY] = text
            if (mutableUiState.value == TextInspectionUiState.InvalidInput)
                mutableUiState.value = TextInspectionUiState.Idle
        }
    }

    @MainThread
    fun inspect(text: String) {
        if (requestJob?.isActive == true ||
            mutableUiState.value is TextInspectionUiState.Pending) return
        val normalized = text.trim()
        if (normalized.codePointCount(0, normalized.length) !in 5..3_000) {
            mutableUiState.value = TextInspectionUiState.InvalidInput
            return
        }
        submittedText = normalized
        startInspection(normalized)
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
                val result = inspectWithApi(text)
                val json = Gson().toJson(result)
                resultPendingOpen = json
                mutableUiState.value = TextInspectionUiState.Success(json)
            } catch (pending: AnalysisNotCompletedException) {
                pendingAnalysisId = pending.analysisId
                mutableUiState.value = TextInspectionUiState.Pending(requireNotNull(pending.analysisId))
            } catch (cancelled: CancellationException) {
                mutableUiState.value = TextInspectionUiState.Idle
                throw cancelled
            } catch (error: Exception) {
                logInspectionFailure(error)
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
            } catch (error: Exception) {
                logInspectionFailure(error)
                mutableUiState.value = TextInspectionUiState.Error
            }
        }
    }

    private fun logInspectionFailure(error: Exception) {
        val causes = generateSequence(error as Throwable?) { it.cause }.toList()
        val httpError = causes.filterIsInstance<AnalysisHttpException>().firstOrNull()
        val protocolError = causes.filterIsInstance<AnalysisProtocolException>().firstOrNull()
        val analysisError = causes.filterIsInstance<AnalysisFailedException>().firstOrNull()
        val ioError = causes.filterIsInstance<IOException>().firstOrNull()
        val detail = when {
            httpError != null -> "HTTP ${httpError.httpStatus}, code=${httpError.code ?: "unknown"}"
            protocolError != null -> "API protocol error, status=${protocolError.serverStatus ?: "unknown"}"
            analysisError != null -> "Analysis status=FAILED, code=${analysisError.errorCode ?: "unknown"}"
            ioError != null -> "I/O or network error (${ioError.javaClass.simpleName})"
            else -> "Unexpected error (${error.javaClass.simpleName})"
        }
        Log.e(TAG, "Text inspection failed: $detail", error)
    }

    private companion object {
        const val TAG = "TextInspection"
        const val INPUT_TEXT_KEY = "inputText"
    }
}
