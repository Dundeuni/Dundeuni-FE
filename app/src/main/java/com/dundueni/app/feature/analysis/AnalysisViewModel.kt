package com.dundueni.app.feature.analysis

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dundueni.app.data.repository.AnalysisResultRepository
import com.dundueni.app.data.remote.api.AnalysisHttpException
import com.dundueni.app.data.remote.api.AnalysisProtocolException
import com.dundueni.app.data.remote.api.TextAnalysisApiService
import com.dundueni.app.data.repository.AnalysisLookupException
import com.dundueni.app.data.repository.findResultForDisplay
import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.data.repository.AnalysisFailedException
import com.dundueni.app.data.repository.AnalysisNotCompletedException
import java.io.IOException
import java.io.InterruptedIOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
class AnalysisViewModel(
    private val repository: AnalysisResultRepository,
    private val screenCropApi: (() -> TextAnalysisApiService)? = null,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = mutableUiState.asStateFlow()
    private var requestJob: Job? = null
    private var screenImage: Result<AnalysisResult>? = null
    private var screenText: Result<ScreenCropAnalysis>? = null
    var attemptId: String = UUID.randomUUID().toString()
        private set

    /** UI 메인 스레드에서 호출합니다. 진행 중 재요청은 무시하며 완료 후 재시도할 수 있습니다. */
    @MainThread
    fun analyze(image: MultipartBody.Part) {
        analyzePrepared { image }
    }

    /** Rechecks the ID from POST without uploading the image or creating another analysis. */
    @MainThread
    fun refreshPending() {
        if (screenImage != null) { refreshScreen(); return }
        val id = when (val state = mutableUiState.value) {
            is AnalysisUiState.Pending -> state.analysisId
            is AnalysisUiState.Error -> state.pendingAnalysisId
            else -> null
        } ?: return
        if (requestJob?.isActive == true) return
        requestJob = viewModelScope.launch {
            mutableUiState.value = AnalysisUiState.Loading
            try {
                val result = repository.findResult(id)
                ensureActive()
                mutableUiState.value = AnalysisUiState.Success(result)
            } catch (cancelled: CancellationException) {
                mutableUiState.value = AnalysisUiState.Idle
                throw cancelled
            } catch (exception: Exception) {
                if (!isActive) {
                    mutableUiState.value = AnalysisUiState.Idle
                    ensureActive()
                }
                mutableUiState.value = when (val next = exception.toUiState()) {
                    is AnalysisUiState.Error -> next.copy(pendingAnalysisId = id)
                    else -> next
                }
            }
        }
    }

    /** 준비 단계도 Loading/Error에 포함합니다. 블로킹 변환은 전달 함수에서 IO로 전환합니다. */
    @MainThread
    fun analyzePrepared(prepare: suspend () -> MultipartBody.Part) = analyzePrepared(false, prepare)

    @MainThread
    fun analyzePrepared(screenCrop: Boolean, prepare: suspend () -> MultipartBody.Part) {
        if (screenCrop && screenImage != null && screenHasLookup()) { refreshScreen(); return }
        // Ignore duplicate requests before touching the saved independent outcomes.
        if (requestJob?.isActive == true || mutableUiState.value is AnalysisUiState.Pending) return
        screenImage = null
        screenText = null
        attemptId = UUID.randomUUID().toString()
        requestJob = viewModelScope.launch {
            mutableUiState.value = AnalysisUiState.Loading
            var inputPrepared = false
            try {
                // 먼저 이미지를 준비한 뒤 분석을 요청하며, 어느 단계의 실패든 아래에서 처리한다.
                val image = prepare()
                inputPrepared = true
                if (screenCrop) {
                    val (imageOutcome, textOutcome) = coroutineScope {
                        val imageRequest = async { attempt { checkNotNull(repository.analyzeResult(image)) } }
                        val textRequest = async { attempt {
                            val api = checkNotNull(screenCropApi) { "Screen crop API is unavailable" }.invoke()
                            api.analyzeScreenCrop(image).toCompletedScreenCrop(repository)
                        } }
                        imageRequest.await() to textRequest.await()
                    }
                    ensureActive()
                    screenImage = imageOutcome
                    screenText = textOutcome
                    publishScreenResult()
                } else {
                    val result = checkNotNull(repository.analyzeResult(image))
                    ensureActive()
                    // 서버 값은 보존하고, 최종 판정 표시 여부는 화면 매핑에서 결정합니다.
                    mutableUiState.value = AnalysisUiState.Success(result)
                }
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
                mutableUiState.value = if (!inputPrepared) {
                    AnalysisUiState.Error(AnalysisErrorKind.INPUT)
                } else exception.toUiState()
            }
        }
    }

    private fun Throwable.lookupId(): String? = when (this) {
        is AnalysisLookupException -> analysisId
        is AnalysisNotCompletedException -> analysisId
        else -> null
    }

    private fun screenHasLookup(): Boolean =
        screenImage?.exceptionOrNull()?.lookupId() != null || screenText?.exceptionOrNull()?.lookupId() != null

    /** Recheck only unfinished IDs and keep an already successful side intact. */
    private fun refreshScreen() {
        if (requestJob?.isActive == true || !screenHasLookup()) return
        requestJob = viewModelScope.launch {
            mutableUiState.value = AnalysisUiState.Loading
            try {
                coroutineScope {
                    val imageRequest = async {
                        screenImage?.exceptionOrNull()?.lookupId()?.let { id ->
                            screenImage = attempt { repository.findResultForDisplay(id) }
                        }
                    }
                    val textRequest = async {
                        screenText?.exceptionOrNull()?.lookupId()?.let { id ->
                            screenText = attempt { repository.findResultForDisplay(id).toScreenCropAnalysis() }
                        }
                    }
                    imageRequest.await()
                    textRequest.await()
                }
                ensureActive()
                publishScreenResult()
            } catch (cancelled: CancellationException) {
                mutableUiState.value = AnalysisUiState.Idle
                throw cancelled
            }
        }
    }

    private fun publishScreenResult() {
        val image = checkNotNull(screenImage)
        val text = checkNotNull(screenText)
        val imageError = image.exceptionOrNull()
        val textError = text.exceptionOrNull()
        mutableUiState.value = if (image.isSuccess || text.isSuccess) {
            AnalysisUiState.ScreenSuccess(image.getOrNull(), text.getOrNull(),
                imageError?.toPartialError(), textError?.toPartialError())
        } else {
            val error = listOfNotNull(imageError, textError)
                .firstOrNull { it !is AnalysisNotCompletedException } ?: checkNotNull(imageError)
            (error as Exception).toUiState()
        }
    }

    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (exception: Exception) {
        Result.failure(exception)
    }

    private fun Throwable.toPartialError(): AnalysisUiState.Error =
        when (val state = (this as? Exception)?.toUiState()) {
            is AnalysisUiState.Error -> state
            is AnalysisUiState.Pending -> AnalysisUiState.Error(AnalysisErrorKind.IN_PROGRESS, pendingAnalysisId = state.analysisId)
            else -> AnalysisUiState.Error()
        }

    private fun Exception.toUiState(): AnalysisUiState = when (this) {
        is AnalysisLookupException -> when (val state = (cause as Exception).toUiState()) {
            is AnalysisUiState.Error -> state.copy(pendingAnalysisId = analysisId)
            else -> state
        }
        is AnalysisApiConfigurationException -> AnalysisUiState.Error(AnalysisErrorKind.CONFIGURATION)
        is AnalysisHttpException -> AnalysisUiState.Error(AnalysisErrorKind.HTTP, httpStatus)
        is AnalysisProtocolException -> AnalysisUiState.Error(AnalysisErrorKind.RESPONSE)
        is AnalysisNotCompletedException -> {
            val id = analysisId
            if (id.isNullOrBlank()) AnalysisUiState.Error(AnalysisErrorKind.RESPONSE)
            else AnalysisUiState.Pending(id, status)
        }
        is AnalysisFailedException -> AnalysisUiState.Error(AnalysisErrorKind.SERVER)
        // SocketTimeoutException and OkHttp call timeouts both extend InterruptedIOException.
        is InterruptedIOException -> AnalysisUiState.Error(AnalysisErrorKind.TIMEOUT)
        is IOException -> AnalysisUiState.Error(AnalysisErrorKind.CONNECTION)
        else -> AnalysisUiState.Error()
    }
}
