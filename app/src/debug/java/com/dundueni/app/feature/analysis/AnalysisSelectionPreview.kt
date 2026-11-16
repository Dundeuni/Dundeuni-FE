package com.dundueni.app.feature.analysis

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.dundueni.app.data.remote.api.PhotoBox
import com.dundueni.app.data.remote.api.PhotoCandidate
import com.dundueni.app.data.remote.api.PhotoImage
import com.dundueni.app.data.remote.api.PhotoUpload
import com.dundueni.app.ui.theme.DundueniFETheme

@Preview(name = "검사 대상 선택 · 사진 후보 2개", widthDp = 390, heightDp = 900, showBackground = true)
@Composable
private fun PhotoCandidatesPreview() = SelectionPreview(withCandidates = true)

@Preview(name = "검사 대상 선택 · 사진 후보 없음", widthDp = 390, heightDp = 900, showBackground = true)
@Composable
private fun WholeImagePreview() = SelectionPreview(withCandidates = false)

@Composable
private fun SelectionPreview(withCandidates: Boolean) {
    val preview = remember { samplePreviewBitmap() }
    val upload = remember(withCandidates) {
        PhotoUpload(
            uploadId = "preview-upload",
            image = PhotoImage(width = preview.width, height = preview.height),
            original = PhotoCandidate(elementId = "whole-image"),
            elements = if (withCandidates) listOf(
                PhotoCandidate(elementId = "photo-1", bbox = PhotoBox(24, 76, 144, 132)),
                PhotoCandidate(elementId = "photo-2", bbox = PhotoBox(192, 76, 144, 132)),
            ) else emptyList(),
        )
    }
    DundueniFETheme {
        AnalysisFlowScreen(
            state = AnalysisUiState.Selection(upload, preview),
            onRetry = {},
            onClose = {},
            onHome = {},
            isDemo = false,
            onChoosePhoto = {},
        )
    }
}

private fun samplePreviewBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(360, 248, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    canvas.drawColor(Color.WHITE)
    paint.color = Color.rgb(17, 27, 43)
    canvas.drawRect(0f, 0f, 360f, 56f, paint)
    paint.color = Color.rgb(233, 238, 251)
    canvas.drawRect(24f, 76f, 168f, 208f, paint)
    canvas.drawRect(192f, 76f, 336f, 208f, paint)
    paint.color = Color.rgb(2, 102, 255)
    canvas.drawCircle(96f, 125f, 24f, paint)
    canvas.drawCircle(264f, 125f, 24f, paint)
    paint.color = Color.rgb(100, 105, 121)
    canvas.drawRect(51f, 175f, 141f, 183f, paint)
    canvas.drawRect(219f, 175f, 309f, 183f, paint)
    return bitmap
}
