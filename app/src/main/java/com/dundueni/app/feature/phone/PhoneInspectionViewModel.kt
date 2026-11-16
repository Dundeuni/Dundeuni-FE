package com.dundueni.app.feature.phone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dundueni.app.data.remote.api.AnalysisHttpException
import com.dundueni.app.data.remote.api.PhoneAnalysisRequestDto
import com.dundueni.app.data.remote.api.PhoneAnalysisResponseDto
import com.dundueni.app.feature.analysis.AnalysisFlowDependencies
import com.dundueni.app.feature.analysisresult.AnalysisResult
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.feature.history.HistoryType
import com.dundueni.app.data.remote.api.HistoryEntryDto
import com.dundueni.app.feature.analysisresult.RiskEvidence
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PhoneInspectionUiState {
    data object Idle : PhoneInspectionUiState
    data object Loading : PhoneInspectionUiState
    data class Success(val response: PhoneAnalysisResponseDto, val result: AnalysisResult = response.toPb05Result()) : PhoneInspectionUiState
    data class Error(val message: String) : PhoneInspectionUiState
}

class PhoneInspectionViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableUiState = MutableStateFlow<PhoneInspectionUiState>(PhoneInspectionUiState.Idle)
    val uiState: StateFlow<PhoneInspectionUiState> = mutableUiState.asStateFlow()
    private var submittedPhone: String? = null
    private var completedAnalysisId: String? = null

    fun inspect(phone: String) {
        if (mutableUiState.value == PhoneInspectionUiState.Loading || PhoneInputValidator.phoneError(phone) != null || phone.isBlank()) return
        submittedPhone = phone
        completedAnalysisId = null
        mutableUiState.value = PhoneInspectionUiState.Loading
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val response = AnalysisFlowDependencies.createPhoneApiService(context)
                    .analyzePhone(PhoneAnalysisRequestDto(phone))
                val serverCode = if (response.code() != 201) response.errorBody()?.use { body ->
                    runCatching { org.json.JSONObject(body.string()).optString("code") }.getOrNull()
                } else null
                mutableUiState.value = when (response.code()) {
                    201 -> {
                        val body = response.body()
                        if (body?.isCompleted() == true) {
                            completedAnalysisId = body.analysisId
                            val detail = AnalysisFlowDependencies.createHistoryRepository(context)
                                .detail(requireNotNull(body.analysisId))
                            PhoneInspectionUiState.Success(body, detail.toPhoneResult())
                        }
                        else PhoneInspectionUiState.Error("서버 응답에 올바른 분석 ID 또는 완료 상태가 없어요.")
                    }
                    400 -> PhoneInspectionUiState.Error("전화번호 형식을 확인해 주세요.")
                    401 -> PhoneInspectionUiState.Error("전화번호 검사 서버 인증 오류 (HTTP 401). 잠시 후 다시 시도해 주세요.")
                    500 -> PhoneInspectionUiState.Error(if (serverCode == "APICK_CONFIGURATION_ERROR")
                        "전화번호 검사 서버 설정을 확인해 주세요." else "서버 오류로 검사를 완료하지 못했어요. 잠시 후 다시 시도해 주세요.")
                    503 -> PhoneInspectionUiState.Error(when (serverCode) {
                        "APICK_QUOTA_EXCEEDED" -> "전화번호 검사 서비스 사용량이 초과되었어요."
                        "APICK_AUTHENTICATION_FAILED" -> "전화번호 검사 서비스 인증에 실패했어요."
                        else -> "전화번호 검사 서비스를 사용할 수 없어요. 잠시 후 다시 시도해 주세요."
                    })
                    else -> PhoneInspectionUiState.Error("검사를 완료하지 못했어요 (HTTP ${response.code()}).")
                }
            } catch (cancelled: CancellationException) {
                mutableUiState.value = PhoneInspectionUiState.Idle
                throw cancelled
            } catch (error: AnalysisHttpException) {
                mutableUiState.value = PhoneInspectionUiState.Error(
                    "검사 결과를 조회하지 못했어요 (HTTP ${error.httpStatus}${error.code?.let { ", $it" }.orEmpty()}). 다시 시도해 주세요."
                )
            } catch (_: IOException) {
                mutableUiState.value = PhoneInspectionUiState.Error("네트워크 연결을 확인한 뒤 다시 시도해 주세요.")
            } catch (_: Exception) {
                mutableUiState.value = PhoneInspectionUiState.Error("검사를 완료하지 못했어요. 다시 시도해 주세요.")
            }
        }
    }

    fun retry() {
        if (mutableUiState.value !is PhoneInspectionUiState.Error) return
        val id = completedAnalysisId
        if (id == null) { submittedPhone?.let(::inspect); return }
        mutableUiState.value = PhoneInspectionUiState.Loading
        viewModelScope.launch {
            try {
                val detail = AnalysisFlowDependencies.createHistoryRepository(getApplication<Application>())
                    .detail(id)
                mutableUiState.value = PhoneInspectionUiState.Success(
                    PhoneAnalysisResponseDto(id, "COMPLETED"), detail.toPhoneResult(),
                )
            } catch (cancelled: CancellationException) {
                mutableUiState.value = PhoneInspectionUiState.Idle
                throw cancelled
            } catch (error: Exception) {
                mutableUiState.value = PhoneInspectionUiState.Error(
                    "검사 결과를 조회하지 못했어요. 다시 시도해 주세요."
                )
            }
        }
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

/** Phone verdict and evidence come only from the stored BE detail response. */
internal fun HistoryEntryDto.toPhoneResult(): AnalysisResult {
    require(type == "PHONE" && status == "COMPLETED") { "Unexpected phone detail" }
    val phone = requireNotNull(phoneResult) { "Phone result is missing" }
    val risk = when (phone.riskLevel) {
        "SAFE" -> RiskLevel.SAFE
        "CAUTION" -> RiskLevel.CAUTION
        "DANGER" -> RiskLevel.DANGER
        "UNKNOWN" -> RiskLevel.UNKNOWN
        else -> throw IllegalArgumentException("Unknown phone risk level")
    }
    val reasons = phone.reasons.orEmpty().filter { it.isNotBlank() }
    return AnalysisResult(
        id = requireNotNull(id),
        historyType = HistoryType.PHONE,
        riskLevel = risk,
        summary = reasons.firstOrNull() ?: "전화번호 분석 결과를 조회했어요. 판단 근거는 제공되지 않았어요.",
        keyFinding = listOfNotNull(phone.spamType?.takeIf { it.isNotBlank() }?.let { "스팸 유형: $it" },
            phone.reportCount?.let { "신고 건수: ${it}건" }).joinToString(" · ").ifBlank { null },
        evidence = reasons.map { RiskEvidence("전화번호 분석 근거", it) },
        targetLabel = phone.phoneNumber ?: "검사한 전화번호",
    )
}
