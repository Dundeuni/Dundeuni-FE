package com.dundueni.app.feature.analysis

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.dundueni.app.feature.history.HistoryType
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dundueni.app.data.auth.AuthSessionRuntime
import com.dundueni.app.BuildConfig
import com.dundueni.app.feature.auth.LoginActivity
import com.dundueni.app.feature.floatingbutton.FloatingButtonManager
import com.dundueni.app.data.model.ImageInput
import com.dundueni.app.feature.home.returnToHome
import com.dundueni.app.feature.payment.BasicInspectionQuota
import com.dundueni.app.feature.payment.showLimitIfExhausted
import com.dundueni.app.ui.theme.DundueniFETheme
import java.io.File
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import com.dundueni.app.feature.floatingbutton.FloatingButtonService
import com.dundueni.app.feature.analysisresult.AnalysisResultActivity
import com.google.gson.Gson

/** 기존 Activity 전환 방식으로 연결하는 내부 검사 화면입니다. */
/**
 * 전달된 이미지 입력을 확인하고 검사 화면을 시작하는 Activity다.
 * 사진은 원본을 읽을 URI로, 캡처는 이미 저장된 PNG의 파일명으로 받는다.
 * 큰 이미지 자체를 화면 사이에 넘기지 않고 필요한 주소만 전달받는다.
 * 입력 준비 방법을 ViewModel에 넘기고, 화면은 ViewModel 상태를 관찰하게 한다.
 * 재시도 시 같은 입력을 다시 준비하며, 실제 종료 시 캡처 캐시를 정리한다.
 */
class AnalysisActivity : ComponentActivity() {
    private val overlayOwner = "image-analysis:${UUID.randomUUID()}"
    private lateinit var viewModel: AnalysisViewModel
    private var captureFile: File? = null
    private var redirectingToLogin = false

    override fun onStart() {
        super.onStart()
        FloatingButtonService.prepareCompletionOverlay(this)
        if (intent.getBooleanExtra(EXTRA_SUSPEND_FLOATING_BUTTON, false)) {
            FloatingButtonManager.suspendForActivity(overlayOwner)
        }
        android.util.Log.d("FloatingButtonService", "AnalysisActivity.onStart ${FloatingButtonService.debugState(this)}")
    }

    override fun onStop() {
        if (intent.getBooleanExtra(EXTRA_SUSPEND_FLOATING_BUTTON, false)) {
            FloatingButtonManager.resumeForActivity(overlayOwner)
        }
        android.util.Log.d("FloatingButtonService", "AnalysisActivity.onStop ${FloatingButtonService.debugState(this)}")
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialUnauthorizedRevision = AuthSessionRuntime.unauthorizedRevision.value
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AuthSessionRuntime.unauthorizedRevision.collect { revision ->
                    if (revision > initialUnauthorizedRevision) redirectToLogin()
                }
            }
        }
        captureFile = intent.getStringExtra(EXTRA_CAPTURE_FILENAME)?.let { name ->
            // 내부 캐시 하위의 파일명만 허용합니다. 외부 경로는 받지 않습니다.
            if (name == File(name).name) File(captureDirectory(this), name) else null
        }
        // 회전으로 Activity가 다시 만들어져도 기존 ViewModel을 얻어 진행 상태를 이어 간다.
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == AnalysisViewModel::class.java)
                @Suppress("UNCHECKED_CAST")
                val results = AnalysisFlowDependencies.createRepository(applicationContext)
                return AnalysisViewModel(
                    results,
                    screenCropApi = { AnalysisFlowDependencies.createTextApiService(applicationContext) },
                    photoSelectionProvider = if (BuildConfig.ANALYSIS_API_MODE.trim().equals("REAL", ignoreCase = true))
                        ({ AnalysisFlowDependencies.createPhotoSelectionRepository(applicationContext, results) }) else null,
                ) as T
            }
        })[AnalysisViewModel::class.java]

        // 회전 시 진행 중 요청/결과는 ViewModel에서 유지합니다. 프로세스 재생성 시에는 재시도합니다.
        if (viewModel.uiState.value == AnalysisUiState.Idle) {
            if (showLimitIfExhausted(this)) { finish(); return }
            startAnalysis()
        }
        lifecycleScope.launch {
            combine(viewModel.uiState, lifecycle.currentStateFlow) { state, visibility -> state to visibility }
                .collect { (state, visibility) ->
                    val isDemo = BuildConfig.ANALYSIS_API_MODE.trim().equals("FAKE", ignoreCase = true)
                    val result = state.toPresentedResult(isDemo,
                        if (captureFile != null) HistoryType.SCREEN else HistoryType.IMAGE) ?: return@collect
                    val key = "image:${viewModel.attemptId}"
                    if (visibility == Lifecycle.State.RESUMED) {
                        viewModel.completionDelivery.claim(key)
                        FloatingButtonService.dismissInspectionCompleted(key)
                    } else if (visibility != Lifecycle.State.DESTROYED && !isFinishing &&
                        viewModel.completionDelivery.claim(key)) {
                        if (!BasicInspectionQuota(this@AnalysisActivity).recordCompleted(viewModel.attemptId)) return@collect
                        val screen = state as? AnalysisUiState.ScreenSuccess
                        val destination = AnalysisResultActivity.createIntent(this@AnalysisActivity,
                            Gson().toJson(result), isDemo, suspendFloatingButton = true)
                            .putExtra(AnalysisResultActivity.EXTRA_SCREEN_CROP_DECISION,
                                screen?.text?.let { "${it.label}\n위험 수준: ${it.detail.toTextResult().riskLevel.label}" })
                            .putExtra(AnalysisResultActivity.EXTRA_IMAGE_ERROR, screen?.imageError?.message)
                            .putExtra(AnalysisResultActivity.EXTRA_TEXT_ERROR, screen?.textError?.message)
                        FloatingButtonService.showInspectionCompleted(key, destination)
                    }
                }
        }

        setContent {
            DundueniFETheme {
                // 화면 수명에 맞춰 상태를 관찰하고, 변경된 상태를 Compose 화면에 전달한다.
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val visibility by lifecycle.currentStateFlow.collectAsState()
                var allowedResultId by rememberSaveable { mutableStateOf<String?>(null) }
                val isDemo = BuildConfig.ANALYSIS_API_MODE.trim().equals("FAKE", ignoreCase = true)
                val resultId = if (state.toPresentedResult(isDemo,
                        if (captureFile != null) HistoryType.SCREEN else HistoryType.IMAGE) != null)
                    viewModel.attemptId else null
                LaunchedEffect(resultId, visibility) {
                    if (visibility != Lifecycle.State.RESUMED) return@LaunchedEffect
                    if (resultId == null || allowedResultId == resultId) return@LaunchedEffect
                    if (BasicInspectionQuota(this@AnalysisActivity).recordCompleted(resultId)) {
                        allowedResultId = resultId
                    } else {
                        showLimitIfExhausted(this@AnalysisActivity)
                        finish()
                    }
                }
                Box(Modifier.safeDrawingPadding()) {
                    if (resultId == null || allowedResultId == resultId) {
                        AnalysisFlowScreen(state, onRetry = ::startAnalysis, onClose = ::closeAnalysis, onHome = ::returnToHome,
                            historyType = if (captureFile != null) HistoryType.SCREEN else HistoryType.IMAGE,
                            onRefresh = viewModel::refreshPending, onChoosePhoto = viewModel::choosePhoto)
                    }
                }
            }
        }
    }

    private fun startAnalysis() {
        if (showLimitIfExhausted(this)) { finish(); return }
        val processor = AnalysisInputProcessor(applicationContext.contentResolver, applicationContext.cacheDir)
        val file = captureFile
        val uri = intent.data
        // 최초 실행과 재시도 모두 입력 준비부터 시작해 준비 실패도 검사 상태에 포함한다.
        val real = BuildConfig.ANALYSIS_API_MODE.trim().equals("REAL", ignoreCase = true)
        if (real) viewModel.uploadPrepared(screenCrop = file != null) {
            if (file != null) processor.prepareCaptureUpload(file)
            else processor.preparePhotoUpload(ImageInput.PhotoPicker(requireNotNull(uri)))
        } else viewModel.analyzePrepared {
            if (file != null) processor.prepareCaptureFile(file)
            else processor.prepare(ImageInput.PhotoPicker(requireNotNull(uri)))
        }
    }

    private fun closeAnalysis() {
        lifecycleScope.launch {
            try {
                if (!viewModel.cancelUpload())
                    Toast.makeText(this@AnalysisActivity, "이미지 업로드 취소를 확인하지 못했어요.", Toast.LENGTH_SHORT).show()
            } finally {
                finish()
            }
        }
    }

    @Deprecated("Uses the Activity back callback for upload cleanup")
    override fun onBackPressed() {
        closeAnalysis()
    }

    private fun redirectToLogin() {
        if (redirectingToLogin || isFinishing) return
        redirectingToLogin = true
        FloatingButtonManager.stop(this)
        stopService(Intent(this, com.dundueni.app.feature.capture.MediaProjectionService::class.java))
        startActivity(Intent(this, LoginActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
        ))
        finishAffinity()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 회전 중에는 파일을 유지하고, 사용자가 검사 화면을 끝낼 때 삭제한다.
        if (isFinishing) captureFile?.delete()
    }

    companion object {
        private const val EXTRA_CAPTURE_FILENAME = "analysis_capture_filename"
        private const val EXTRA_SUSPEND_FLOATING_BUTTON = "suspend_floating_button"
        fun captureDirectory(context: Context): File = File(context.cacheDir, "analysis_captures").apply { mkdirs() }

        fun forPhoto(context: Context, uri: Uri, suspendFloatingButton: Boolean = false): Intent = Intent(context, AnalysisActivity::class.java).apply {
            data = uri
            clipData = ClipData.newRawUri("analysis image", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(EXTRA_SUSPEND_FLOATING_BUTTON, suspendFloatingButton)
        }

        fun forCapture(context: Context, file: File): Intent = Intent(context, AnalysisActivity::class.java).apply {
            require(file.parentFile?.canonicalFile == captureDirectory(context).canonicalFile)
            putExtra(EXTRA_CAPTURE_FILENAME, file.name)
            putExtra(EXTRA_SUSPEND_FLOATING_BUTTON, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
    }
}
