package com.dundueni.app.data.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import com.dundueni.app.data.model.PreprocessedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream

/** IO 스레드에서 호출한다. 호출자가 소유한 Bitmap/캡처 파일은 수정하거나 삭제하지 않는다. */
internal class ImageUploadPreprocessor(
    private val resolver: ContentResolver,
    private val cacheDirectory: File,
    private val encode: (Bitmap, Bitmap.CompressFormat, Int, OutputStream) -> Boolean =
        { bitmap, format, quality, output -> bitmap.compress(format, quality, output) }
) {
    data class Upload(val bytes: ByteArray, val mimeType: String, val filename: String)

    fun prepare(image: PreprocessedImage): Upload = try {
        when (image) {
            is PreprocessedImage.PhotoPicker -> preparePhoto(image)
            is PreprocessedImage.ScreenCapture -> {
                check(!image.bitmap.isRecycled) { "Screen capture bitmap is recycled" }
                compress(image.bitmap, Bitmap.CompressFormat.PNG, 100)?.let {
                    upload(it, "capture.png", converted = false)
                } ?: jpeg(image.bitmap, "capture.png", ExifInterface.ORIENTATION_NORMAL)
            }
        }
    } catch (error: OutOfMemoryError) {
        // ViewModel의 기존 Error 경로로 전달한다. 메모리 부족 시 해상도를 임의 축소하지 않는다.
        throw IOException("Insufficient memory to process image", error)
    }

    private fun preparePhoto(image: PreprocessedImage.PhotoPicker): Upload {
        // Provider 메타데이터와 무관하게 동일한 원본 스냅샷을 검사·디코딩·전송한다.
        val source = File.createTempFile("analysis_input_", ".tmp", cacheDirectory)
        try {
            (resolver.openInputStream(image.uri) ?: throw IOException("Unable to open image")).use { input ->
                source.outputStream().use { input.copyTo(it) }
            }
            if (source.length() == 0L) throw IOException("Image is empty")
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.path, options)
            checkMemory(options.outWidth, options.outHeight)
            val bitmap = BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
            }) ?: throw IOException("Unsupported or damaged image")
            try {
                val header = source.inputStream().use { input ->
                    ByteArray(8).also { input.read(it) }
                }
                if (format(header) != null && source.length() <= MAX_BYTES) {
                    // EXIF도 원본 바이트에 남는다. 변환이 필요 없는 JPEG/PNG는 재인코딩하지 않는다.
                    return upload(source.readBytes(), image.displayName, converted = false)
                }
                val orientation = try {
                    ExifInterface(source.path).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                    )
                } catch (_: IOException) {
                    // OS가 EXIF를 지원하지 않는 형식도 픽셀 디코딩에 성공했다면 변환할 수 있다.
                    ExifInterface.ORIENTATION_NORMAL
                }
                return jpeg(bitmap, image.displayName, orientation)
            } finally {
                bitmap.recycle()
            }
        } finally {
            source.delete()
        }
    }

    private fun jpeg(original: Bitmap, name: String?, orientation: Int): Upload {
        checkMemory(original.width, original.height)
        val matrix = orientationMatrix(orientation)
        val rotated = if (matrix.isIdentity) original else
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        try {
            val opaque = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
            try {
                Canvas(opaque).apply {
                    drawColor(Color.WHITE)
                    drawBitmap(rotated, 0f, 0f, null)
                }
                // 두 품질 모두 같은 원본 디코딩/방향 보정 픽셀에서 인코딩한다.
                for (quality in intArrayOf(90, 85)) {
                    compress(opaque, Bitmap.CompressFormat.JPEG, quality)?.let {
                        return upload(it, name, converted = true)
                    }
                }
                throw IOException("Image exceeds 10 MiB after JPEG quality 85")
            } finally {
                opaque.recycle()
            }
        } finally {
            if (rotated !== original) rotated.recycle()
        }
    }

    private fun compress(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int): ByteArray? {
        val output = LimitedOutput()
        if (!encode(bitmap, format, quality, output)) throw IOException("Unable to encode image")
        return output.result()
    }

    private fun upload(bytes: ByteArray, name: String?, converted: Boolean): Upload {
        if (bytes.isEmpty() || bytes.size > MAX_BYTES) throw IOException("Invalid image size")
        val mime = format(bytes) ?: throw IOException("Invalid encoded image signature")
        if (converted && mime != "image/jpeg") throw IOException("Expected JPEG output")
        val extension = if (mime == "image/png") "png" else "jpg"
        val safeName = name?.substringAfterLast('/')?.substringAfterLast('\\')
            ?.replace(Regex("[\\r\\n\\\"]"), "_")?.takeIf { it.isNotBlank() }
        val currentExtension = safeName?.substringAfterLast('.', "")?.lowercase()
        val matches = if (mime == "image/png") currentExtension == "png"
            else currentExtension == "jpg" || (!converted && currentExtension == "jpeg")
        val filename = if (matches && !converted) safeName!! else
            "${safeName?.substringBeforeLast('.', safeName)?.takeIf { it.isNotBlank() } ?: "image"}.$extension"
        return Upload(bytes, mime, filename)
    }

    private fun checkMemory(width: Int, height: Int) {
        if (width <= 0 || height <= 0) throw IOException("Unsupported or damaged image")
        // ARGB 원본·회전·흰 배경과 인코딩 버퍼 여유를 확보한다. 자동 축소는 하지 않는다.
        val bitmapBytes = width.toLong() * height * 4L
        val runtime = Runtime.getRuntime()
        val available = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
        if (bitmapBytes > MAX_BITMAP_BYTES || bitmapBytes * 3L + MAX_BYTES * 4L > available) {
            throw IOException("Image dimensions exceed available processing memory")
        }
    }

    private class LimitedOutput : OutputStream() {
        private val bytes = ByteArrayOutputStream()
        private var exceeded = false
        override fun write(value: Int) {
            if (exceeded) return
            if (bytes.size() == MAX_BYTES) exceeded = true else bytes.write(value)
        }
        override fun write(buffer: ByteArray, offset: Int, length: Int) {
            if (exceeded) return
            if (length > MAX_BYTES - bytes.size()) exceeded = true else bytes.write(buffer, offset, length)
        }
        fun result(): ByteArray? = if (exceeded) null else bytes.toByteArray()
    }

    companion object {
        const val MAX_BYTES = 10_485_760
        private const val MAX_BITMAP_BYTES = 128L * 1024 * 1024
        private val PNG = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
        private fun format(bytes: ByteArray): String? = when {
            bytes.size >= 8 && PNG.indices.all { bytes[it] == PNG[it] } -> "image/png"
            bytes.size >= 4 && bytes[0] == (-1).toByte() && bytes[1] == (-40).toByte() && bytes[2] == (-1).toByte() -> "image/jpeg"
            else -> null
        }

        internal fun orientationMatrix(orientation: Int): Matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
    }
}
