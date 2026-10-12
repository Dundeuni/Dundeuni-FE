package com.dundueni.app.feature.analysis

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dundueni.app.data.model.ImageInput
import com.dundueni.app.ui.theme.DundueniFETheme
import java.io.File

/** 기존 Activity 전환 방식으로 연결하는 내부 검사 화면입니다. */
/**
 * 전달된 이미지 입력을 확인하고 검사 화면을 시작하는 Activity다.
 * 사진은 원본을 읽을 URI로, 캡처는 이미 저장된 PNG의 파일명으로 받는다.
 * 큰 이미지 자체를 화면 사이에 넘기지 않고 필요한 주소만 전달받는다.
 * 입력 준비 방법을 ViewModel에 넘기고, 화면은 ViewModel 상태를 관찰하게 한다.
 * 재시도 시 같은 입력을 다시 준비하며, 실제 종료 시 캡처 캐시를 정리한다.
 */
class AnalysisActivity : ComponentActivity() {
    private lateinit var viewModel: AnalysisViewModel
    private var captureFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        captureFile = intent.getStringExtra(EXTRA_CAPTURE_FILENAME)?.let { name ->
            // 내부 캐시 하위의 파일명만 허용합니다. 외부 경로는 받지 않습니다.
            if (name == File(name).name) File(captureDirectory(this), name) else null
        }
        // 회전으로 Activity가 다시 만들어져도 기존 ViewModel을 얻어 진행 상태를 이어 간다.
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == AnalysisViewModel::class.java)
                @Suppress("UNCHECKED_CAST")
                return AnalysisViewModel(AnalysisFlowDependencies.createRepository()) as T
            }
        })[AnalysisViewModel::class.java]

        // 회전 시 진행 중 요청/결과는 ViewModel에서 유지합니다. 프로세스 재생성 시에는 재시도합니다.
        if (viewModel.uiState.value == AnalysisUiState.Idle) startAnalysis()
        setContent {
            DundueniFETheme {
                // 화면 수명에 맞춰 상태를 관찰하고, 변경된 상태를 Compose 화면에 전달한다.
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                Box(Modifier.safeDrawingPadding()) {
                    AnalysisFlowScreen(state, onRetry = ::startAnalysis, onClose = ::finish)
                }
            }
        }
    }

    private fun startAnalysis() {
        val processor = AnalysisInputProcessor(applicationContext.contentResolver, applicationContext.cacheDir)
        val file = captureFile
        val uri = intent.data
        // 최초 실행과 재시도 모두 입력 준비부터 시작해 준비 실패도 검사 상태에 포함한다.
        viewModel.analyzePrepared {
            if (file != null) processor.prepareCaptureFile(file)
            else processor.prepare(ImageInput.PhotoPicker(requireNotNull(uri)))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 회전 중에는 파일을 유지하고, 사용자가 검사 화면을 끝낼 때 삭제한다.
        if (isFinishing) captureFile?.delete()
    }

    companion object {
        private const val EXTRA_CAPTURE_FILENAME = "analysis_capture_filename"
        fun captureDirectory(context: Context): File = File(context.cacheDir, "analysis_captures").apply { mkdirs() }

        fun forPhoto(context: Context, uri: Uri): Intent = Intent(context, AnalysisActivity::class.java).apply {
            data = uri
            clipData = ClipData.newRawUri("analysis image", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        fun forCapture(context: Context, file: File): Intent = Intent(context, AnalysisActivity::class.java).apply {
            require(file.parentFile?.canonicalFile == captureDirectory(context).canonicalFile)
            putExtra(EXTRA_CAPTURE_FILENAME, file.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
