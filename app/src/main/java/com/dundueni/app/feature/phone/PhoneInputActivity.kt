package com.dundueni.app.feature.phone

import android.os.Bundle
import android.content.Intent
import com.dundueni.app.BuildConfig
import com.dundueni.app.feature.history.HistoryType
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dundueni.app.feature.auth.LoginActivity
import com.dundueni.app.feature.analysisresult.AnalysisResult
import com.dundueni.app.feature.analysisresult.AnalysisResultActivity
import com.dundueni.app.feature.analysisresult.RiskEvidence
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.feature.analysisresult.AiLikelihood
import com.dundueni.app.feature.floatingbutton.FloatingButtonManager
import com.dundueni.app.feature.payment.BasicInspectionQuota
import com.dundueni.app.feature.payment.showLimitIfExhausted
import com.dundueni.app.ui.theme.DundueniFETheme
import com.dundueni.app.ui.component.InspectionErrorDialog
import com.dundueni.app.ui.component.InspectionLoadingDialog
import com.google.gson.Gson
import java.util.UUID

class PhoneInputActivity : ComponentActivity() {
    private val overlayOwner = "phone-input:${UUID.randomUUID()}"
    private lateinit var viewModel: PhoneInspectionViewModel
    private var redirectingToLogin = false

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
        viewModel = ViewModelProvider(this)[PhoneInspectionViewModel::class.java]
        setContent {
            DundueniFETheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                var loadingPopupDismissed by rememberSaveable { mutableStateOf(false) }
                Box(modifier = Modifier.safeDrawingPadding()) {
                    PhoneInputScreen(
                        onInspect = { phone ->
                            if (showLimitIfExhausted(this@PhoneInputActivity)) return@PhoneInputScreen
                            when (BuildConfig.ANALYSIS_API_MODE.trim().uppercase()) {
                                "REAL" -> {
                                    loadingPopupDismissed = false
                                    viewModel.inspect(phone)
                                }
                                "FAKE" -> showMockResult(phone)
                                else -> viewModel.showInvalidMode()
                            }
                        },
                        onClose = { finish() },
                        isInspecting = state == PhoneInspectionUiState.Loading,
                    )
                }
                when (state) {
                    PhoneInspectionUiState.Loading -> if (!loadingPopupDismissed) {
                        InspectionLoadingDialog(onDismissPopup = { loadingPopupDismissed = true })
                    }
                    is PhoneInspectionUiState.Error -> InspectionErrorDialog(
                        message = (state as PhoneInspectionUiState.Error).message,
                        onRetry = {
                            if (showLimitIfExhausted(this@PhoneInputActivity)) return@InspectionErrorDialog
                            loadingPopupDismissed = false
                            viewModel.retry()
                        },
                        onCancelInspection = viewModel::dismiss,
                        showRetry = BuildConfig.ANALYSIS_API_MODE.trim().equals("REAL", ignoreCase = true),
                    )
                    else -> Unit
                }
                LaunchedEffect(state) {
                    when (val current = state) {
                        PhoneInspectionUiState.LoginRequired -> redirectToLogin()
                        is PhoneInspectionUiState.Success -> {
                            val id = current.response.analysisId ?: return@LaunchedEffect
                            if (BasicInspectionQuota(this@PhoneInputActivity).recordCompleted(id)) {
                                val result = current.response.toPb05Result()
                                startActivity(AnalysisResultActivity.createIntent(
                                    this@PhoneInputActivity, Gson().toJson(result),
                                    isDemo = false, suspendFloatingButton = true,
                                ))
                                viewModel.dismiss()
                            } else {
                                viewModel.dismiss()
                                showLimitIfExhausted(this@PhoneInputActivity)
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun redirectToLogin() {
        if (redirectingToLogin || isFinishing) return
        redirectingToLogin = true
        FloatingButtonManager.stop(this)
        startActivity(Intent(this, LoginActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
        ))
        finishAffinity()
    }

    private fun showMockResult(phone: String) {
        val result = AnalysisResult(
            historyType = HistoryType.PHONE,
            id = "phone-scan-${UUID.randomUUID()}",
            riskLevel = RiskLevel.CAUTION,
            summary = "입력하신 전화번호에 대한 분석 결과입니다 (Mock 데이터).",
            keyFinding = "발신자 번호(발신처)의 신고 이력 및 스팸 여부를 가상 조회했습니다.",
            fraudScore = 50.0,
            aiLikelihood = AiLikelihood.UNKNOWN,
            evidence = listOf(
                RiskEvidence("발신 번호 조회 (Mock)", "입력된 전화번호($phone)가 사기 신고 및 스미싱 의심 번호 데이터베이스와 가상 대조되었습니다."),
                RiskEvidence("사칭 주의", "공공기관이나 금융사 번호로 위장한 발신 번호인지 확인이 필요합니다.")
            ),
            targetLabel = "입력한 전화번호"
        )
        if (!BasicInspectionQuota(this).recordCompleted(result.id)) {
            showLimitIfExhausted(this)
            return
        }
        val json = Gson().toJson(result)
        val intent = AnalysisResultActivity.createIntent(
            this, json, isDemo = true, suspendFloatingButton = true,
        )
        startActivity(intent)
    }
}
