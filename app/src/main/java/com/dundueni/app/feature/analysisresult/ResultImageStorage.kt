package com.dundueni.app.feature.analysisresult

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import com.dundueni.app.ui.theme.PageBackground
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.ceil

private const val ResultImageWidthPx = 720

internal fun renderResultBitmap(
    anchor: View,
    parentComposition: CompositionContext,
    contentWidthPx: Int,
    content: @Composable () -> Unit,
): Bitmap {
    // Compose needs the host's view-tree owners. Attach and remove within the same main-thread call.
    val host = checkNotNull(anchor.rootView.findViewById<ViewGroup>(android.R.id.content))
    val composeView = ComposeView(anchor.context).apply {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        setParentCompositionContext(parentComposition)
        setContent(content)
    }
    try {
        host.addView(composeView, ViewGroup.LayoutParams(0, 0))
        composeView.measure(
            MeasureSpec.makeMeasureSpec(contentWidthPx, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )
        val height = composeView.measuredHeight
        check(height > 0)
        composeView.layout(0, 0, contentWidthPx, height)
        val scale = ResultImageWidthPx.toDouble() / contentWidthPx
        val bitmap = Bitmap.createBitmap(ResultImageWidthPx, ceil(height * scale).toInt(), Bitmap.Config.ARGB_8888)
        var drawn = false
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(PageBackground.toArgb())
            canvas.scale(scale.toFloat(), scale.toFloat())
            composeView.draw(canvas)
            drawn = true
            return bitmap
        } finally {
            if (!drawn) bitmap.recycle()
        }
    } finally {
        host.removeView(composeView)
        composeView.disposeComposition()
    }
}

@Suppress("DEPRECATION")
internal fun saveResultPng(context: Context, bitmap: Bitmap) {
    val resolver = context.contentResolver
    val album = "${Environment.DIRECTORY_PICTURES}/Dundueni"
    val scopedStorage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    var legacyFile: File? = null
    var uri: Uri? = null
    var saved = false
    try {
        val values = ContentValues().apply { put(MediaStore.Images.Media.MIME_TYPE, "image/png") }
        val collection = if (scopedStorage) {
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "Dundueni_${UUID.randomUUID()}.png")
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "$album/")
            values.put(MediaStore.Images.Media.IS_PENDING, 1)
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            val directory = File(Environment.getExternalStorageDirectory(), album)
            if (!directory.mkdirs() && !directory.isDirectory) throw IOException("Could not create image directory")
            val file = File.createTempFile("Dundueni_", ".png", directory)
            legacyFile = file
            values.put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            values.put(MediaStore.Images.Media.DATA, file.absolutePath)
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val image = resolver.insert(collection, values) ?: throw IOException("Could not create MediaStore image")
        uri = image
        val output = resolver.openOutputStream(image) ?: throw IOException("Could not open MediaStore image")
        output.use {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("Could not encode PNG")
        }
        if (scopedStorage) {
            val published = resolver.update(
                image,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null,
                null,
            )
            if (published != 1) throw IOException("Could not publish MediaStore image")
        }
        saved = true
    } finally {
        if (!saved) {
            uri?.let { runCatching { resolver.delete(it, null, null) } }
            legacyFile?.let { runCatching { it.delete() } }
        }
    }
}
