package com.dundueni.app.ui.component

import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.dundueni.app.R
import com.dundueni.app.ui.theme.DundueniDefaultShape
import com.dundueni.app.ui.theme.DundueniDimens

/** Dismissing this dialog only hides the popup. The caller keeps the analysis job running. */
@Composable
fun InspectionLoadingDialog(onDismissPopup: () -> Unit) {
    InspectionStatusDialog(
        mascotResId = R.drawable.dundeuni_mascot_phone,
        mascotDescription = "검사 중인 든든이",
        title = "잠시만 기다려 주세요",
        description = "든든이가 열심히 검사하고 있어요!",
        onDismissRequest = onDismissPopup,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(24.dp))
        DundueniButton(
            text = "닫고 검사 계속하기",
            onClick = onDismissPopup,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "팝업을 닫아도 검사는 계속 진행돼요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Retry and cancel are actions supplied by the caller; hiding is never treated as cancel. */
@Composable
fun InspectionErrorDialog(
    message: String,
    onRetry: () -> Unit,
    onCancelInspection: () -> Unit,
    showRetry: Boolean = true,
) {
    InspectionStatusDialog(
        mascotResId = R.drawable.dundeuni_mascot_nervous,
        mascotDescription = "걱정하는 든든이",
        title = "검사를 완료하지 못했어요",
        description = message,
        onDismissRequest = {},
        dismissible = false,
    ) {
        if (showRetry) {
            DundueniButton(
                text = "다시 시도",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }
        OutlinedButton(
            onClick = onCancelInspection,
            modifier = Modifier.fillMaxWidth().height(DundueniDimens.ButtonHeight),
            shape = DundueniDefaultShape,
        ) {
            Text("검사 취소", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun InspectionLimitDialog(onUpgrade: () -> Unit, onLater: () -> Unit) {
    InspectionStatusDialog(
        mascotResId = R.drawable.dundeuni_mascot_sleep,
        mascotDescription = "잠든 든든이",
        title = "검사 횟수를 모두 소진했어요",
        description = "베이직 검사 5회를 모두 사용했어요.",
        onDismissRequest = onLater,
    ) {
        DundueniButton(
            text = "업그레이드",
            onClick = onUpgrade,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onLater,
            modifier = Modifier.fillMaxWidth().height(DundueniDimens.ButtonHeight),
            shape = DundueniDefaultShape,
        ) {
            Text("나중에", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun InspectionStatusDialog(
    @DrawableRes mascotResId: Int,
    mascotDescription: String,
    title: String,
    description: String,
    onDismissRequest: () -> Unit,
    dismissible: Boolean = true,
    actions: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
        ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(window) {
            if (window == null) {
                onDispose { }
            } else {
                val originalDimAmount = window.attributes.dimAmount
                val hadDimBehind = window.attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setDimAmount(0.45f)
                onDispose {
                    window.setDimAmount(originalDimAmount)
                    if (!hadDimBehind) window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(mascotResId),
                    contentDescription = mascotDescription,
                    modifier = Modifier.size(112.dp),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                actions()
            }
        }
    }
}
