package com.dundueni.app.feature.analysis

import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.data.repository.AnalysisExecutionStatus
import com.dundueni.app.data.remote.api.PhotoUpload
import android.graphics.Bitmap

/**
 * 검사 화면이 무엇을 보여 줄지 결정하는 현재 상태를 표현한다.
 * Idle은 대기, Loading은 입력 준비와 분석 진행 중인 상태다.
 * Success에는 표시할 결과를 담고, Error는 실패 안내와 재시도를 위한 상태다.
 * ViewModel이 상태를 바꾸고 AnalysisFlowScreen이 이를 받아 화면을 구성한다.
 */
sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data object Loading : AnalysisUiState
    data class Selection(val upload: PhotoUpload, val preview: Bitmap) : AnalysisUiState
    data class Pending(val analysisId: String, val status: AnalysisExecutionStatus) : AnalysisUiState
    data class Success(val result: AnalysisResult) : AnalysisUiState
    data class ScreenSuccess(
        val image: AnalysisResult?,
        val text: ScreenCropAnalysis?,
        val imageError: Error?,
        val textError: Error?,
    ) : AnalysisUiState
    // Preserve a safe category, never the exception or raw server message.
    data class Error(
        val kind: AnalysisErrorKind = AnalysisErrorKind.UNKNOWN,
        val httpStatus: Int? = null,
        val pendingAnalysisId: String? = null,
        val reuploadRequired: Boolean = false,
    ) : AnalysisUiState {
        val message: String
            get() = if (reuploadRequired) "업로드가 만료되었거나 서버 재시작으로 사라졌습니다. 이미지를 다시 업로드해 주세요."
            else if (kind == AnalysisErrorKind.HTTP) {
                when (httpStatus) {
                    400 -> "잘못된 분석 요청입니다."
                    401 -> "로그인이 필요합니다."
                    404 -> "분석 결과를 찾을 수 없거나 만료되었습니다."
                    500 -> "분석 서버 오류가 발생했습니다."
                    409 -> "분석 요청이 충돌했습니다."
                    410 -> "분석 세션이 만료되었습니다."
                    413 -> "이미지 크기가 너무 큽니다."
                    415 -> "지원하지 않는 이미지 형식입니다."
                    429 -> "분석 이용량을 초과했습니다."
                    503 -> "현재 분석 서버를 이용할 수 없습니다."
                    else -> kind.message
                }
            } else kind.message
    }
}

enum class AnalysisErrorKind(val message: String) {
    PHOTO_DETECTION("사진 후보를 찾는 중 오류가 발생했습니다. 이미지를 다시 업로드해 주세요."),
    RESOLUTION("이미지는 가로·세로 각각 32~8,192px, 전체 1,600만 픽셀 이하여야 합니다."),
    INPUT("이미지를 읽거나 변환하지 못했습니다. 다시 시도하거나 이미지를 다시 선택해 주세요."),
    CONFIGURATION("분석 서버 연결 설정을 확인해 주세요."),
    CONNECTION("분석 서버에 연결하지 못했습니다. 네트워크와 서버 실행 상태를 확인해 주세요."),
    HTTP("분석 서버가 요청을 정상 처리하지 못했습니다."),
    TIMEOUT("분석 서버 응답 시간이 초과되었습니다. 다시 시도해 주세요."),
    RESPONSE("분석 서버 응답 형식을 확인할 수 없습니다."),
    IN_PROGRESS("서버 분석이 아직 완료되지 않았습니다. 같은 분석 ID로 결과를 다시 확인해 주세요."),
    SERVER("서버에서 이미지 분석에 실패했습니다. 다시 시도해 주세요."),
    UNKNOWN("검사에 실패했습니다. 다시 시도하거나 이미지를 다시 선택해 주세요."),
}
