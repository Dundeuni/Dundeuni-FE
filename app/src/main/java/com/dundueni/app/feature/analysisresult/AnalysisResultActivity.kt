package com.dundueni.app.feature.analysisresult

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dundueni.app.feature.analysis.AnalysisFlowDependencies
import com.dundueni.app.feature.analysis.AnalysisUiState
import com.dundueni.app.feature.analysis.AnalysisViewModel
import com.dundueni.app.feature.analysis.ScreenInspectionResult
import com.dundueni.app.feature.analysis.toPresentedResult
import com.dundueni.app.feature.history.HistoryType
import com.dundueni.app.ui.theme.DundueniFETheme
import com.dundueni.app.feature.home.returnToHome
import com.dundueni.app.feature.floatingbutton.FloatingButtonManager
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnalysisResultActivity : ComponentActivity() {
    private val overlayOwner = "text-result:${java.util.UUID.randomUUID()}"
    private val screenViewModel by viewModels<AnalysisViewModel> {
        viewModelFactory {
            initializer { AnalysisViewModel(AnalysisFlowDependencies.createRepository(applicationContext)) }
        }
    }
    private var loading by mutableStateOf(true)
    private var result by mutableStateOf<AnalysisResult?>(null)
    private var screen by mutableStateOf<AnalysisUiState.ScreenSuccess?>(null)

    companion object {
        const val EXTRA_IMAGE_ERROR = "analysis.imageError"
        const val EXTRA_TEXT_ERROR = "analysis.textError"
        const val EXTRA_RESULT_JSON = "com.dundueni.app.analysis.RESULT_JSON"
        private const val EXTRA_RESULT_KEY = "com.dundueni.app.analysis.RESULT_KEY"
        const val EXTRA_IS_DEMO = "com.dundueni.app.analysis.IS_DEMO"
        const val EXTRA_SUSPEND_FLOATING_BUTTON = "com.dundueni.app.analysis.SUSPEND_FLOATING_BUTTON"

        /** Prefer AnalysisResultScreen(result, onAction) when integrating an existing navigation graph. */
        fun createIntent(
            context: Context,
            resultJson: String,
            isDemo: Boolean = false,
            suspendFloatingButton: Boolean = false,
        ): Intent = createDeliveryIntent(
            context, AnalysisResultDelivery(resultJson = resultJson), isDemo, suspendFloatingButton,
        )

        internal fun createScreenIntent(context: Context, screen: AnalysisUiState.ScreenSuccess): Intent =
            createDeliveryIntent(context, AnalysisResultDelivery(screen = screen), false, true)

        private fun createDeliveryIntent(
            context: Context,
            delivery: AnalysisResultDelivery,
            isDemo: Boolean,
            suspendFloatingButton: Boolean,
        ): Intent {
            val key = AnalysisResultDeliveryStore(context.cacheDir).write(delivery)
            return Intent(context, AnalysisResultActivity::class.java)
                .putExtra(EXTRA_RESULT_KEY, key)
                .putExtra(EXTRA_IS_DEMO, isDemo)
                .putExtra(EXTRA_SUSPEND_FLOATING_BUTTON, suspendFloatingButton)
        }

        internal fun readDelivery(context: Context, intent: Intent): AnalysisResultDelivery? {
            val key = intent.getStringExtra(EXTRA_RESULT_KEY)
            if (key != null) return AnalysisResultDeliveryStore(context.cacheDir).read(key)
            return intent.getStringExtra(EXTRA_RESULT_JSON)?.let { AnalysisResultDelivery(resultJson = it) }
        }
    }

    override fun onStart() {
        super.onStart()
        if (intent.getBooleanExtra(EXTRA_SUSPEND_FLOATING_BUTTON, false)) {
            FloatingButtonManager.suspendForActivity(overlayOwner)
        }
    }

    override fun onStop() {
        if (intent.getBooleanExtra(EXTRA_SUSPEND_FLOATING_BUTTON, false)) {
            FloatingButtonManager.resumeForActivity(overlayOwner)
        }
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch { loadResult() }
        val isDemo = intent.getBooleanExtra(EXTRA_IS_DEMO, false)
        setContent {
            DundueniFETheme {
                val displayed = result
                val screenState = screen
                if (displayed != null && screenState != null) {
                    val status by screenViewModel.uiState.collectAsStateWithLifecycle()
                    ScreenInspectionResult(
                        screenState, displayed, ::finish, ::returnToHome, screenViewModel::refreshPending,
                        refreshing = status == AnalysisUiState.Loading,
                    )
                } else if (displayed != null) {
                    AnalysisResultRoute(displayed, onReturn = ::finish, onHome = ::returnToHome, isDemo = isDemo,
                        imageAnalysisError = intent.getStringExtra(EXTRA_IMAGE_ERROR),
                        screenCropError = intent.getStringExtra(EXTRA_TEXT_ERROR))
                } else if (!loading) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                        Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("분석 결과를 불러오지 못했어요.", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(12.dp))
                            Text("결과 데이터가 없거나 형식이 올바르지 않아요. 이전 화면에서 다시 확인해주세요.")
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = ::returnToHome) { Text("홈으로 돌아가기") }
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadResult() {
        val delivery = withContext(Dispatchers.IO) {
            runCatching { readDelivery(applicationContext, intent) }.getOrNull()
        }
        val initialScreen = delivery?.screen
        if (initialScreen == null) {
            result = withContext(Dispatchers.IO) {
                runCatching { delivery?.resultJson?.let(AnalysisResultJsonAdapter::decode) }.getOrNull()
            }
            loading = false
            return
        }

        screenViewModel.restoreScreenResult(initialScreen)
        val key = requireNotNull(intent.getStringExtra(EXTRA_RESULT_KEY))
        var cached = initialScreen
        screenViewModel.uiState.filterIsInstance<AnalysisUiState.ScreenSuccess>().collect { updated ->
            result = updated.toPresentedResult(false, HistoryType.SCREEN)
            screen = updated
            loading = false
            if (updated != cached) {
                try {
                    withContext(Dispatchers.IO) {
                        AnalysisResultDeliveryStore(cacheDir).write(AnalysisResultDelivery(screen = updated), key)
                    }
                    cached = updated
                } catch (_: IOException) {
                    Toast.makeText(this, "갱신한 결과를 임시 보관하지 못했어요.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
