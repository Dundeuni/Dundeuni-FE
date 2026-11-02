package com.dundueni.app.feature.textinput

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
 * 검사 상태를 표시하고 Mock 결과가 준비되면 결과 화면으로 이동한다.
 */
class TextInputActivity : ComponentActivity() {
    private val overlayOwner = "text-input:${java.util.UUID.randomUUID()}"
    private lateinit var viewModel: TextInspectionViewModel

    override fun onStart() {
        super.onStart()
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
                        isInspecting = state == TextInspectionUiState.Loading,
                    )
                }
                when (state) {
                    TextInspectionUiState.Loading -> if (!loadingPopupDismissed) {
                        InspectionLoadingDialog(onDismissPopup = { loadingPopupDismissed = true })
                    }
                    TextInspectionUiState.Error -> InspectionErrorDialog(
                        message = "텍스트 검사를 완료하지 못했어요. 다시 시도하면 같은 텍스트로 검사합니다.",
                        onRetry = {
                            if (!viewModel.hasResultPendingOpen && showLimitIfExhausted(this@TextInputActivity)) return@InspectionErrorDialog
                            loadingPopupDismissed = false
                            viewModel.retry()
                        },
                        onCancelInspection = viewModel::cancelError,
                    )
                    else -> Unit
                }
                LaunchedEffect(state) {
                    val success = state as? TextInspectionUiState.Success ?: return@LaunchedEffect
                    try {
                        val id = JsonParser.parseString(success.resultJson).asJsonObject.get("id").asString
                        if (!BasicInspectionQuota(this@TextInputActivity).recordCompleted(id)) {
                            showLimitIfExhausted(this@TextInputActivity)
                            viewModel.onResultOpened()
                            return@LaunchedEffect
                        }
                        startActivity(AnalysisResultActivity.createIntent(
                            this@TextInputActivity, success.resultJson, isDemo = true, suspendFloatingButton = true,
                        ))
                        viewModel.onResultOpened()
                    } catch (_: Exception) {
                        viewModel.onResultOpenFailure()
                    }
                }
            }
        }
    }
}
