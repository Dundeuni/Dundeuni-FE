package com.dundueni.app.feature.phone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dundueni.app.data.auth.AuthSessionRuntime
import com.dundueni.app.data.auth.AuthSessionSource
import com.dundueni.app.data.remote.api.PhoneAnalysisRequestDto
import com.dundueni.app.data.remote.api.PhoneAnalysisResponseDto
import com.dundueni.app.feature.analysis.AnalysisFlowDependencies
import com.dundueni.app.feature.analysisresult.AnalysisResult
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.feature.history.HistoryType
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PhoneInspectionUiState {
    data object Idle : PhoneInspectionUiState
    data object Loading : PhoneInspectionUiState
    data class Success(val response: PhoneAnalysisResponseDto) : PhoneInspectionUiState
    data class Error(val message: String) : PhoneInspectionUiState
    data object LoginRequired : PhoneInspectionUiState
}

class PhoneInspectionViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableUiState = MutableStateFlow<PhoneInspectionUiState>(PhoneInspectionUiState.Idle)
    val uiState: StateFlow<PhoneInspectionUiState> = mutableUiState.asStateFlow()
    private var submittedPhone: String? = null

    fun inspect(phone: String) {
        if (mutableUiState.value == PhoneInspectionUiState.Loading || PhoneInputValidator.phoneError(phone) != null || phone.isBlank()) return
        submittedPhone = phone
        mutableUiState.value = PhoneInspectionUiState.Loading
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val session = AuthSessionRuntime.store(context).read()
                if (session?.source != AuthSessionSource.BACKEND || !session.isValid()) {
                    if (session?.source == AuthSessionSource.BACKEND) AuthSessionRuntime.onExpired(session)
                    mutableUiState.value = PhoneInspectionUiState.LoginRequired
                    return@launch
                }
                val response = AnalysisFlowDependencies.createPhoneApiService(context)
                    .analyzePhone(PhoneAnalysisRequestDto(phone))
                if (response.code() != 201) response.errorBody()?.close()
                mutableUiState.value = when (response.code()) {
                    201 -> {
                        val body = response.body()
                        if (body?.isCompleted() == true) PhoneInspectionUiState.Success(body)
                        else PhoneInspectionUiState.Error("서버 응답에 올바른 분석 ID 또는 완료 상태가 없어요.")
                    }
                    400 -> PhoneInspectionUiState.Error("전화번호 형식을 확인해 주세요.")
                    401 -> PhoneInspectionUiState.LoginRequired
                    500 -> PhoneInspectionUiState.Error("서버 오류로 검사를 완료하지 못했어요. 잠시 후 다시 시도해 주세요.")
                    503 -> PhoneInspectionUiState.Error("전화번호 검사 서비스를 사용할 수 없어요. 잠시 후 다시 시도해 주세요.")
                    else -> PhoneInspectionUiState.Error("검사를 완료하지 못했어요 (HTTP ${response.code()}).")
                }
            } catch (cancelled: CancellationException) {
                mutableUiState.value = PhoneInspectionUiState.Idle
                throw cancelled
            } catch (_: IOException) {
                mutableUiState.value = PhoneInspectionUiState.Error("네트워크 연결을 확인한 뒤 다시 시도해 주세요.")
            } catch (_: Exception) {
                mutableUiState.value = PhoneInspectionUiState.Error("검사를 완료하지 못했어요. 다시 시도해 주세요.")
            }
        }
    }

    fun retry() {
        if (mutableUiState.value is PhoneInspectionUiState.Error) submittedPhone?.let(::inspect)
    }

    fun showInvalidMode() {
        mutableUiState.value = PhoneInspectionUiState.Error("검사 API 모드 설정을 확인해 주세요.")
    }

    fun dismiss() {
        if (mutableUiState.value != PhoneInspectionUiState.Loading) mutableUiState.value = PhoneInspectionUiState.Idle
    }
}

/** Phone POST supplies completion metadata only; no fraud verdict can be inferred. */
internal fun PhoneAnalysisResponseDto.toPb05Result(): AnalysisResult {
    require(isCompleted()) { "Phone analysis is not completed" }
    return AnalysisResult(
        id = requireNotNull(analysisId),
        historyType = HistoryType.PHONE,
        riskLevel = RiskLevel.UNKNOWN,
        summary = "전화번호 검사가 완료되었어요. 서버에서 위험도와 판단 근거를 제공하지 않았어요.",
        targetLabel = "입력한 전화번호",
    )
}
