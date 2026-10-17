package com.dundueni.app.feature.photopicker

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.dundueni.app.feature.analysis.AnalysisActivity

/** Hosts the system photo picker without showing the old temporary diagnostics UI behind it. */
@Composable
fun PhotoPickerTestScreen(
    openPickerOnLaunch: Boolean = false,
    closeOnCancel: Boolean = false,
    onCancel: () -> Unit = {},
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        if (uri == null) {
            if (closeOnCancel) onCancel()
        } else {
            try {
                context.startActivity(AnalysisActivity.forPhoto(context, uri, suspendFloatingButton = true))
                (context as? Activity)?.finish()
            } catch (_: Exception) {
                Toast.makeText(context, "분석 화면을 열지 못했습니다. 다시 시도해 주세요.", Toast.LENGTH_LONG).show()
            }
        }
    }

    var pendingPickerLaunch by rememberSaveable { mutableStateOf(openPickerOnLaunch) }
    LaunchedEffect(pendingPickerLaunch) {
        if (pendingPickerLaunch) {
            pendingPickerLaunch = false
            try {
                launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            } catch (_: Exception) {
                Toast.makeText(context, "사진 선택을 열지 못했습니다. 다시 시도해 주세요.", Toast.LENGTH_LONG).show()
                if (closeOnCancel) onCancel()
            }
        }
    }

    Box(Modifier.fillMaxSize())
}
