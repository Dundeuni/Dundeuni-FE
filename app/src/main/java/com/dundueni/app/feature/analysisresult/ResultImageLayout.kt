package com.dundueni.app.feature.analysisresult

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dundueni.app.ui.theme.BrandInk
import com.dundueni.app.ui.theme.LocalEasyMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal const val SaveResultImageLabel = "검사 결과 이미지로 저장"
internal val SaveResultImageColor = Color(0xFF3B3D42)

@Composable
internal fun ResultImageLayout(
    title: String,
    maxWidth: Dp,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.(onSave: () -> Unit, canSave: Boolean) -> Unit,
    saveEnabled: Boolean = true,
    imageContentAtClick: (() -> (@Composable () -> Unit))? = null,
    content: @Composable ColumnScope.(forImage: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val parentComposition = rememberCompositionContext()
    val density = LocalDensity.current
    val easy = LocalEasyMode.current
    val preview = LocalInspectionMode.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var widthPx by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var pendingCapture by remember { mutableStateOf<(() -> Bitmap)?>(null) }

    fun finishSaving(success: Boolean) {
        saving = false
        snackbar.currentSnackbarData?.dismiss()
        scope.launch {
            snackbar.showSnackbar(
                message = if (success) "검사 결과 이미지가 저장되었어요."
                    else "이미지 저장에 실패했어요. 다시 시도해 주세요.",
                duration = SnackbarDuration.Short,
            )
        }
    }

    fun save(capture: () -> Bitmap) {
        scope.launch {
            val success = try {
                val bitmap = capture()
                try {
                    withContext(Dispatchers.IO) { saveResultPng(context.applicationContext, bitmap) }
                } finally {
                    bitmap.recycle()
                }
                true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            } catch (_: OutOfMemoryError) {
                false
            }
            finishSaving(success)
        }
    }

    val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
    val permissionLauncher = if (!preview && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val capture = pendingCapture
            pendingCapture = null
            if (capture != null) {
                if (granted) save(capture) else finishSaving(false)
            }
        }
    } else null

    val spacing = Arrangement.spacedBy(16.dp)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = maxWidth).fillMaxSize()
                .onSizeChanged { widthPx = it.width }
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            verticalArrangement = spacing,
        ) {
            content(false)
            actions(
                {
                    if (!preview && saveEnabled && !saving && widthPx > 0) {
                        val widthAtClick = widthPx
                        val imageContent = imageContentAtClick?.invoke()
                        val capture = {
                            renderResultBitmap(view, parentComposition, widthAtClick) {
                                CompositionLocalProvider(LocalDensity provides density, LocalEasyMode provides easy) {
                                    Column(
                                        Modifier.fillMaxWidth().padding(contentPadding),
                                        verticalArrangement = spacing,
                                    ) {
                                        Text(title, color = BrandInk, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                        if (imageContent == null) content(true) else imageContent()
                                    }
                                }
                            }
                        }
                        saving = true
                        if (permissionLauncher != null &&
                            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
                        ) {
                            pendingCapture = capture
                            try {
                                permissionLauncher.launch(permission)
                            } catch (_: RuntimeException) {
                                pendingCapture = null
                                finishSaving(false)
                            }
                        } else {
                            save(capture)
                        }
                    }
                },
                saveEnabled && !saving && (preview || widthPx > 0),
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
