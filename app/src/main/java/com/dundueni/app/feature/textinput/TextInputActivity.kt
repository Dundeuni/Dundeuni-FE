package com.dundueni.app.feature.textinput

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import com.dundueni.app.feature.floatingbutton.FloatingButtonService
import com.dundueni.app.BuildConfig
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dundueni.app.feature.analysisresult.AnalysisResultActivity
import com.dundueni.app.feature.floatingbutton.FloatingButtonManager
import com.dundueni.app.feature.payment.BasicInspectionQuota
import com.dundueni.app.feature.payment.showLimitIfExhausted
import com.google.gson.JsonParser
import com.dundueni.app.ui.component.InspectionErrorDialog
import com.dundueni.app.ui.component.InspectionLoadingDialog
import com.dundueni.app.ui.theme.DundueniFETheme

/**
 * 텍스트 입력 및 검사 화면을 관리하는 Activity.
 * 검사 상태를 표시하고 결과가 준비되면 결과 화면으로 이동한다.
 */
class TextInputActivity : ComponentActivity() {
    private val overlayOwner = "text-input:${java.util.UUID.randomUUID()}"
    private lateinit var viewModel: TextInspectionViewModel

    override fun onStart() {
        super.onStart()
        FloatingButtonService.prepareCompletionOverlay(this)
        FloatingButtonManager.suspendForActivity(overlayOwner)
    }

    override fun onStop() {
        FloatingButtonManager.resumeForActivity(overlayOwner)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel = ViewModelProvider(this)[TextInspectionViewModel::class.java]

        lifecycleScope.launch {
            combine(viewModel.uiState, lifecycle.currentStateFlow) { state, visibility -> state to visibility }
                .collect { (state, visibility) ->
                    val success = state as? TextInspectionUiState.Success ?: return@collect
                    if (isFinishing || visibility == Lifecycle.State.DESTROYED) return@collect
                    try {
                        val id = JsonParser.parseString(success.resultJson).asJsonObject.get("id").asString
                        val key = "text:$id"
                        val foreground = visibility == Lifecycle.State.RESUMED
                        if (!foreground && !viewModel.completionDelivery.claim(key)) return@collect
                        if (!BasicInspectionQuota(this@TextInputActivity).recordCompleted(id)) {
                            if (foreground) showLimitIfExhausted(this@TextInputActivity)
                            return@collect
                        }
                        val destination = AnalysisResultActivity.createIntent(this@TextInputActivity,
                            success.resultJson,
                            isDemo = BuildConfig.ANALYSIS_API_MODE.trim().equals("FAKE", ignoreCase = true),
                            suspendFloatingButton = true)
                        if (foreground) {
                            FloatingButtonService.dismissInspectionCompleted(key)
                            startActivity(destination)
                            viewModel.onResultOpened()
                        } else {
                            FloatingButtonService.showInspectionCompleted(key, destination, viewModel::onResultOpened)
                        }
                    } catch (_: Exception) {
                        viewModel.onResultOpenFailure()
                    }
                }
        }

        setContent {
            DundueniFETheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val inputText by viewModel.inputText.collectAsStateWithLifecycle()
                var loadingPopupDismissed by rememberSaveable { mutableStateOf(false) }
                Box(modifier = Modifier.safeDrawingPadding()) {
                    TextInputScreen(
                        textInput = inputText,
                        onTextChange = viewModel::updateInputText,
                        onInspect = { text ->
                            if (showLimitIfExhausted(this@TextInputActivity)) return@TextInputScreen
                            loadingPopupDismissed = false
                            viewModel.inspect(text)
                        },
                        onClose = ::finish,
                        isInspecting = state == TextInspectionUiState.Loading || state is TextInspectionUiState.Pending,
                    )
                }
                when (state) {
                    TextInspectionUiState.Loading -> if (!loadingPopupDismissed) {
                        InspectionLoadingDialog(onDismissPopup = { loadingPopupDismissed = true })
                    }
                    TextInspectionUiState.Error -> InspectionErrorDialog(
                        message = if (viewModel.hasPendingLookup)
                            "결과를 조회하지 못했어요. 다시 시도하면 같은 분석 ID로 확인합니다."
                        else "텍스트 검사를 완료하지 못했어요. 다시 시도하면 같은 텍스트로 검사합니다.",
                        onRetry = {
                            if (!viewModel.hasResultPendingOpen && showLimitIfExhausted(this@TextInputActivity)) return@InspectionErrorDialog
                            loadingPopupDismissed = false
                            viewModel.retry()
                        },
                        onCancelInspection = viewModel::cancelError,
                    )
                    is TextInspectionUiState.Pending -> AlertDialog(
                        onDismissRequest = {},
                        title = { Text("분석 진행 중") },
                        text = { Text("요청이 접수되었어요. 잠시 후 결과를 다시 확인해 주세요.") },
                        confirmButton = {
                            TextButton(onClick = {
                                loadingPopupDismissed = false
                                viewModel.retry()
                            }) { Text("결과 다시 확인") }
                        },
                        dismissButton = {
                            TextButton(onClick = viewModel::cancelPending) { Text("닫기") }
                        },
                    )
                    else -> Unit
                }
            }
        }
    }
}
