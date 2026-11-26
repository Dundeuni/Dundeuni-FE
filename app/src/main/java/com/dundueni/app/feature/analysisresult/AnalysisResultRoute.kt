package com.dundueni.app.feature.analysisresult

import android.content.ActivityNotFoundException
import android.content.Context
import com.dundueni.app.BuildConfig
import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dundueni.app.data.repository.ImageSummaryResult
import com.dundueni.app.feature.analysis.AnalysisFlowDependencies
import com.dundueni.app.feature.analysis.ScreenResultType
import com.dundueni.app.feature.phone.PhoneInspectionUiState
import com.dundueni.app.feature.phone.PhoneInspectionViewModel
import com.dundueni.app.ui.component.InspectionErrorDialog
import com.dundueni.app.ui.component.InspectionLoadingDialog
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import com.dundueni.app.feature.history.HistoryStore
import com.dundueni.app.feature.history.HistoryType

@Composable
fun AnalysisResultRoute(
    result: AnalysisResult,
    onReturn: () -> Unit,
    onHome: () -> Unit,
    isDemo: Boolean = false,
    imageAnalysisError: String? = null,
    screenCropError: String? = null,
    screenResultType: ScreenResultType? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val canInspectPhone = !isDemo && screenCropError == null &&
        result.selectablePhoneNumbers(screenResultType).isNotEmpty()
    val phoneModel = rememberExtractedPhoneViewModel(canInspectPhone)
    val phoneState = phoneModel?.uiState?.collectAsStateWithLifecycle()?.value
    val phoneBusy = phoneState == PhoneInspectionUiState.Loading || phoneState is PhoneInspectionUiState.Success
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var phoneLoadingDismissed by rememberSaveable { mutableStateOf(false) }

    fun leave(exit: () -> Unit) {
        phoneModel?.cancel()
        exit()
    }
    BackHandler { leave(onReturn) }
    val initialSummary = result.initialImageSummary().takeIf {
        !isDemo && imageAnalysisError == null && screenResultType != ScreenResultType.TEXT
    }
    val summaryId = result.id.takeIf { initialSummary == ImageSummaryResult.Pending }
    val summaryModel = rememberImageSummaryViewModel(summaryId)
    val summaryState = summaryModel?.uiState?.collectAsStateWithLifecycle()?.value
        ?: initialSummary?.let { ImageSummaryUiState(result = it, checking = it == ImageSummaryResult.Pending) }
    val scope = rememberCoroutineScope()
    val appSettings = remember(context) { context.getSharedPreferences("dundueni_settings", Context.MODE_PRIVATE) }
    var riskHapticPlayed by rememberSaveable(result.id) { mutableStateOf(false) }
    val preferences = remember(context) { context.getSharedPreferences("analysis_result_history", Context.MODE_PRIVATE) }
    val historyStore = remember(context) { HistoryStore(context) }
    var saved by rememberSaveable(result.id) { mutableStateOf((!isDemo && BuildConfig.ANALYSIS_API_MODE.equals("REAL", ignoreCase = true)) || hasSavedResult(preferences.getString("results", "[]"), result.id)) }
    var saving by remember { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(result.id, isDemo) {
        val isTextInspection = result.historyType == HistoryType.TEXT || screenResultType == ScreenResultType.TEXT
        val isWarning = result.riskLevel == RiskLevel.CAUTION || result.riskLevel == RiskLevel.DANGER
        if (!isDemo && !isTextInspection && isWarning && !riskHapticPlayed && appSettings.getBoolean("haptics", true)) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            riskHapticPlayed = true
        }
    }

    fun open(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "이 동작을 실행할 앱이 없어요.", Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(context, "이 동작을 실행할 수 없어요.", Toast.LENGTH_SHORT).show()
        }
    }

    fun share() {
        val content = buildString {
            if (isDemo) append("[디자인 미리보기 · 샘플 데이터]\n")
            append("든든이 분석 결과: ${result.riskLevel.label}\n${result.summary}\n")
            result.evidence.forEach { append("• ${it.title}\n") }
            append("\n분석 결과는 참고 정보입니다. 의심스러운 요청은 공식 연락처로 다시 확인해주세요.")
        }
        open(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }, "분석 결과 공유"))
    }

    AnalysisResultScreen(result, isDemo = isDemo,
        imageAnalysisError = imageAnalysisError,
        screenCropError = screenCropError,
        screenResultType = screenResultType,
        imageSummary = summaryState,
        onCheckImageSummary = { summaryModel?.checkAgain() },
        onInspectPhone = phoneModel?.let { model ->
            { phoneNumber ->
                if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                    phoneLoadingDismissed = false
                    model.inspect(phoneNumber)
                }
            }
        },
        phoneInspectionInProgress = phoneBusy,
        onAction = { action ->
        when (action) {
            ResultAction.RETURN -> leave(onReturn)
            ResultAction.HOME -> leave(onHome)
            ResultAction.PROFILE -> Toast.makeText(context, "계정 메뉴를 사용할 수 없어요.", Toast.LENGTH_SHORT).show()
            ResultAction.CALL_FAMILY -> open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:")))
            ResultAction.SHARE, ResultAction.ASK_FAMILY -> share()
            ResultAction.REPORT -> dialog = "report"
            ResultAction.PREVIEW_DOMAIN -> dialog = "domain"
            ResultAction.SAVE_HISTORY -> {
                if (isDemo) {
                    Toast.makeText(context, "샘플 결과는 실제 검사 기록에 보관하지 않아요.", Toast.LENGTH_SHORT).show()
                } else if (!saved && !saving) {
                    saving = true
                    scope.launch(Dispatchers.Main.immediate) {
                        val success = withContext(Dispatchers.IO) {
                            runCatching {
                                historyStore.save(result)
                            }.getOrDefault(false)
                        }
                        saved = success
                        saving = false
                        Toast.makeText(context, if (success) "검사 결과를 보관했어요." else "보관하지 못했어요. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    })

    when (phoneState) {
        PhoneInspectionUiState.Loading -> if (!phoneLoadingDismissed) {
            InspectionLoadingDialog(
                onDismissPopup = { phoneLoadingDismissed = true },
                message = "선택한 전화번호를 검사하고 있어요.",
            )
        }
        is PhoneInspectionUiState.Error -> InspectionErrorDialog(
            message = phoneState.message,
            onRetry = {
                phoneLoadingDismissed = false
                phoneModel?.retry()
            },
            onCancelInspection = { phoneModel?.dismiss() },
        )
        else -> Unit
    }

    if (dialog == "report") {
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("신고 연락처 선택") },
            text = { Text("전화 앱에서 번호를 확인한 뒤 직접 통화할 수 있어요.") },
            confirmButton = { TextButton(onClick = { dialog = null; open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) }) { Text("112 전화 앱 열기") } },
            dismissButton = { TextButton(onClick = { dialog = null; open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:118"))) }) { Text("118 전화 앱 열기") } },
        )
    }
    if (dialog == "domain") {
        val host = remember(result.sourceUrl) { domainForPreview(result.sourceUrl) }
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("인터넷 주소 보기") },
            text = { Text(if (host == null) "인터넷 주소를 확인할 수 없어요." else "$host\n\n사이트를 열지 않고 주소만 보여드려요. 안전한 사이트인지 확인한 결과는 아니에요.\n\n짧게 줄인 주소가 실제로 연결되는 곳은 확인하지 않았어요.") },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("확인") } },
        )
    }
}

@Composable
private fun rememberExtractedPhoneViewModel(enabled: Boolean): PhoneInspectionViewModel? {
    if (!enabled || LocalInspectionMode.current) return null
    val owner = LocalView.current.findViewTreeViewModelStoreOwner() ?: return null
    val activity = LocalActivity.current ?: return null
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val model = remember(owner) {
        val factory = viewModelFactory {
            initializer { PhoneInspectionViewModel.forExtractedPhones(activity.application) }
        }
        ViewModelProvider(owner, factory)["extracted-phone-inspection", PhoneInspectionViewModel::class.java]
    }
    DisposableEffect(model) {
        onDispose { model.cancel() }
    }
    LaunchedEffect(model, lifecycle, activity) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            model.uiState.collectLatest { state ->
                if (state is PhoneInspectionUiState.Success && !activity.isFinishing) {
                    try {
                        val intent = AnalysisResultActivity.createPhoneResultIntent(activity, state.result)
                        if (!activity.isFinishing && lifecycle.currentState == Lifecycle.State.RESUMED &&
                            model.uiState.value === state
                        ) {
                            activity.startActivity(intent)
                            model.dismiss()
                        }
                    } catch (_: IOException) {
                        if (model.uiState.value === state) model.onResultOpenFailure()
                    } catch (_: ActivityNotFoundException) {
                        model.onResultOpenFailure()
                    } catch (_: SecurityException) {
                        model.onResultOpenFailure()
                    }
                }
            }
        }
    }
    return model
}

@Composable
internal fun rememberImageSummaryViewModel(analysisId: String?): ImageSummaryViewModel? {
    if (analysisId == null || LocalInspectionMode.current) return null
    val owner = LocalView.current.findViewTreeViewModelStoreOwner() ?: return null
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val model = remember(owner, analysisId) {
        val factory = viewModelFactory {
            initializer {
                ImageSummaryViewModel.forResult(
                    analysisId, AnalysisFlowDependencies.createRepository(context), ImageSummaryResult.Pending,
                )
            }
        }
        ViewModelProvider(owner, factory)["image-summary:$analysisId", ImageSummaryViewModel::class.java]
    }
    LaunchedEffect(lifecycle, model) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { model.refreshWhileVisible() }
    }
    return model
}

private fun hasSavedResult(json: String?, id: String): Boolean = runCatching {
    val results = JSONArray(json ?: "[]")
    (0 until results.length()).any { results.getJSONObject(it).optString("id") == id }
}.getOrDefault(false)
