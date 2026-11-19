package com.dundueni.app.feature.phone

import android.os.Bundle
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
import com.dundueni.app.feature.analysisresult.AnalysisResultActivity
import com.dundueni.app.feature.floatingbutton.FloatingButtonManager
import com.dundueni.app.ui.theme.DundueniFETheme
import com.dundueni.app.ui.component.InspectionErrorDialog
import com.dundueni.app.ui.component.InspectionLoadingDialog
import com.google.gson.Gson
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhoneInputActivity : ComponentActivity() {
    private val overlayOwner = "phone-input:${UUID.randomUUID()}"
    private lateinit var viewModel: PhoneInspectionViewModel

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
                            loadingPopupDismissed = false
                            viewModel.inspect(phone)
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
                            loadingPopupDismissed = false
                            viewModel.retry()
                        },
                        onCancelInspection = viewModel::dismiss,
                        showRetry = true,
                    )
                    else -> Unit
                }
                LaunchedEffect(state) {
                    when (val current = state) {
                        is PhoneInspectionUiState.Success -> {
                            current.response.analysisId ?: return@LaunchedEffect
                            val result = current.result
                            val destination = try {
                                withContext(Dispatchers.IO) {
                                    AnalysisResultActivity.createIntent(
                                        applicationContext, Gson().toJson(result),
                                        isDemo = false, suspendFloatingButton = true,
                                    )
                                }
                            } catch (_: IOException) {
                                viewModel.onResultOpenFailure()
                                return@LaunchedEffect
                            }
                            startActivity(destination)
                            viewModel.dismiss()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
