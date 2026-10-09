package com.dundueni.app.feature.analysis

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dundueni.app.data.repository.AnalysisResultRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

/**
 * 이미지 준비부터 분석 결과 수신까지 검사 실행과 상태를 관리한다.
 * Activity는 입력 준비 동작을 넘기고, ViewModel은 Repository에 분석을 요청한다.
 * 준비와 요청을 하나의 Loading으로 묶어 완료 전까지 진행 중임을 알린다.
 * 결과나 실패를 StateFlow로 전달해 화면이 현재 상태에 맞춰 표시하게 한다.
 * 요청은 ViewModel 수명에 묶여 화면이 회전해도 유지되고, ViewModel 종료 시 취소된다.
 * 화면 구성과 파일 삭제는 Activity에 맡겨 검사 처리와 분리한다.
 */
class AnalysisViewModel(private val repository: AnalysisResultRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = mutableUiState.asStateFlow()
    private var requestJob: Job? = null

    /** UI 메인 스레드에서 호출합니다. 진행 중 재요청은 무시하며 완료 후 재시도할 수 있습니다. */
    @MainThread
    fun analyze(image: MultipartBody.Part) {
        analyzePrepared { image }
    }

    /** 준비 단계도 Loading/Error에 포함합니다. 블로킹 변환은 전달 함수에서 IO로 전환합니다. */
    @MainThread
    fun analyzePrepared(prepare: suspend () -> MultipartBody.Part) {
        // 빠른 재클릭으로 같은 이미지를 중복 준비하거나 분석 요청이 겹치는 것을 막는다.
        if (requestJob?.isActive == true) return
        requestJob = viewModelScope.launch {
            mutableUiState.value = AnalysisUiState.Loading
            try {
                // 먼저 이미지를 준비한 뒤 분석을 요청하며, 어느 단계의 실패든 아래에서 처리한다.
                val result = checkNotNull(repository.analyzeResult(prepare()))
                ensureActive()
                // 위험도를 해석할 수 없는 응답은 정상 분석 결과로 표시하지 않습니다.
                mutableUiState.value = AnalysisUiState.Success(result)
            // 화면 종료 등에 따른 작업 취소는 검사 실패가 아니므로 Error와 구분한다.
            } catch (cancelled: CancellationException) {
                mutableUiState.value = AnalysisUiState.Idle
                throw cancelled
            } catch (exception: Exception) {
                // Repository가 취소를 다른 예외로 변환해도 Error를 발행하지 않습니다.
                if (!isActive) {
                    mutableUiState.value = AnalysisUiState.Idle
                    ensureActive()
                }
                mutableUiState.value = AnalysisUiState.Error
            }
        }
    }
}
