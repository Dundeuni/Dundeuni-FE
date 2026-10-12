package com.dundueni.app.data.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import com.dundueni.app.data.model.PreprocessedImage
import java.io.File
import java.io.IOException
import java.io.OutputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImageUploadPreprocessorTest {
    private val context = RuntimeEnvironment.getApplication()
    private val calls = mutableListOf<Pair<Bitmap.CompressFormat, Int>>()
    private val encodedBitmaps = mutableListOf<Bitmap>()

    private fun processor(reject: Set<Pair<Bitmap.CompressFormat, Int>> = emptySet()) =
        ImageUploadPreprocessor(context.contentResolver, context.cacheDir) { bitmap, format, quality, output ->
            calls += format to quality
            encodedBitmaps += bitmap
            if ((format to quality) in reject) {
                writeOversize(output)
                true
            } else bitmap.compress(format, quality, output)
        }

    @Test fun capturePngUnderLimitIsPreserved() = withBitmap { bitmap ->
        val result = processor().prepare(PreprocessedImage.ScreenCapture(bitmap))
        assertEquals("image/png", result.mimeType)
        assertEquals(listOf(Bitmap.CompressFormat.PNG to 100), calls)
        assertFalse(bitmap.isRecycled)
        assertDimensions(result, bitmap.width, bitmap.height)
    }

    @Test fun oversizedCapturePngUsesJpeg90() = withBitmap { bitmap ->
        val result = processor(setOf(Bitmap.CompressFormat.PNG to 100))
            .prepare(PreprocessedImage.ScreenCapture(bitmap))
        assertEquals("image/jpeg", result.mimeType)
        assertEquals("capture.jpg", result.filename)
        assertEquals(listOf(Bitmap.CompressFormat.PNG to 100, Bitmap.CompressFormat.JPEG to 90), calls)
        assertFalse(bitmap.isRecycled)
    }

    @Test fun realLargeCapturePngFallsBackToJpegWithoutResize() {
        val width = 2048
        val bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
        try {
            val random = java.util.Random(42)
            val row = IntArray(width)
            repeat(width) { y ->
                for (x in row.indices) row[x] = random.nextInt() or (0xff shl 24)
                bitmap.setPixels(row, 0, width, 0, y, width, 1)
            }
            val result = processor().prepare(PreprocessedImage.ScreenCapture(bitmap))
            assertEquals(listOf(Bitmap.CompressFormat.PNG to 100, Bitmap.CompressFormat.JPEG to 90), calls)
            assertEquals("image/jpeg", result.mimeType)
            assertDimensions(result, width, width)
        } finally { bitmap.recycle() }
    }

    @Test fun oversizedJpeg90Retries85FromSamePixels() = withBitmap { bitmap ->
        val result = processor(setOf(Bitmap.CompressFormat.PNG to 100, Bitmap.CompressFormat.JPEG to 90))
            .prepare(PreprocessedImage.ScreenCapture(bitmap))
        assertEquals(listOf(Bitmap.CompressFormat.PNG to 100, Bitmap.CompressFormat.JPEG to 90, Bitmap.CompressFormat.JPEG to 85), calls)
        assertSame(encodedBitmaps[1], encodedBitmaps[2])
        assertTrue(encodedBitmaps[1].isRecycled)
        assertFalse(bitmap.isRecycled)
        assertDimensions(result, bitmap.width, bitmap.height)
    }

    @Test fun stillOversizedAfter85FailsAndReleasesOwnedBitmap() = withBitmap { bitmap ->
        val policy = processor(setOf(Bitmap.CompressFormat.PNG to 100, Bitmap.CompressFormat.JPEG to 90, Bitmap.CompressFormat.JPEG to 85))
        assertThrows(IOException::class.java) { policy.prepare(PreprocessedImage.ScreenCapture(bitmap)) }
        assertEquals(3, calls.size)
        assertTrue(encodedBitmaps.last().isRecycled)
        assertFalse(bitmap.isRecycled)
    }

    @Test fun jpegAndPngAtExact10MiBLimitKeepAllOriginalBytes() = withBitmap { bitmap ->
        for (format in listOf(Bitmap.CompressFormat.PNG, Bitmap.CompressFormat.JPEG)) {
            withPhoto(bitmap, format) { file ->
                file.appendBytes(ByteArray(ImageUploadPreprocessor.MAX_BYTES - file.length().toInt()))
                val result = processor().prepare(photo(file))
                assertArrayEquals(file.readBytes(), result.bytes)
                assertEquals(ImageUploadPreprocessor.MAX_BYTES, result.bytes.size)
                assertTrue(calls.isEmpty())
            }
        }
    }

    @Test fun oversizedPngAndJpegUse90Then85() = withBitmap { bitmap ->
        for (format in listOf(Bitmap.CompressFormat.PNG, Bitmap.CompressFormat.JPEG)) {
            withPhoto(bitmap, format) { file ->
                file.appendBytes(ByteArray(ImageUploadPreprocessor.MAX_BYTES + 1 - file.length().toInt()))
                for (reject90 in listOf(false, true)) {
                    calls.clear()
                    encodedBitmaps.clear()
                    val result = processor(if (reject90) setOf(Bitmap.CompressFormat.JPEG to 90) else emptySet())
                        .prepare(photo(file))
                    assertEquals("image/jpeg", result.mimeType)
                    assertTrue(result.filename.endsWith(".jpg"))
                    assertEquals(if (reject90) listOf(Bitmap.CompressFormat.JPEG to 90, Bitmap.CompressFormat.JPEG to 85)
                        else listOf(Bitmap.CompressFormat.JPEG to 90), calls)
                    if (reject90) assertSame(encodedBitmaps[0], encodedBitmaps[1])
                }
            }
        }
        assertNoTemporaryInput()
    }

    @Test fun webpConvertsToJpegWithWhiteTransparencyAndNoResize() = withBitmap { bitmap ->
        bitmap.eraseColor(Color.TRANSPARENT)
        @Suppress("DEPRECATION")
        withPhoto(bitmap, Bitmap.CompressFormat.WEBP) { file ->
            val result = processor().prepare(photo(file).copy(mimeType = "image/png", displayName = "wrong.png"))
            assertEquals("image/jpeg", result.mimeType)
            assertEquals("wrong.jpg", result.filename)
            assertEquals(listOf(Bitmap.CompressFormat.JPEG to 90), calls)
            val decoded = BitmapFactory.decodeByteArray(result.bytes, 0, result.bytes.size)!!
            try {
                assertEquals(bitmap.width, decoded.width)
                assertEquals(bitmap.height, decoded.height)
                assertTrue(Color.red(decoded.getPixel(0, 0)) >= 250)
                assertTrue(Color.green(decoded.getPixel(0, 0)) >= 250)
                assertTrue(Color.blue(decoded.getPixel(0, 0)) >= 250)
            } finally { decoded.recycle() }
        }
    }

    @Test fun jpegConversionAppliesExifRotation() = withBitmap { bitmap ->
        withPhoto(bitmap, Bitmap.CompressFormat.JPEG) { file ->
            ExifInterface(file.path).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }
            // 원본 유지 경로는 EXIF까지 그대로 보존한다.
            assertArrayEquals(file.readBytes(), processor().prepare(photo(file)).bytes)
            file.appendBytes(ByteArray(ImageUploadPreprocessor.MAX_BYTES + 1 - file.length().toInt()))
            val result = processor().prepare(photo(file))
            assertDimensions(result, bitmap.height, bitmap.width)
            assertEquals(listOf(Bitmap.CompressFormat.JPEG to 90), calls)
        }
    }

    @Test fun allExifOrientationsMapCornersCorrectly() {
        val expected = listOf(
            floatArrayOf(0f, 0f, 3f, 2f), floatArrayOf(0f, 0f, -3f, 2f),
            floatArrayOf(0f, 0f, -3f, -2f), floatArrayOf(0f, 0f, 3f, -2f),
            floatArrayOf(0f, 0f, 2f, 3f), floatArrayOf(0f, 0f, -2f, 3f),
            floatArrayOf(0f, 0f, -2f, -3f), floatArrayOf(0f, 0f, 2f, -3f)
        )
        for (orientation in 1..8) {
            val points = floatArrayOf(0f, 0f, 3f, 2f)
            ImageUploadPreprocessor.orientationMatrix(orientation).mapPoints(points)
            assertArrayEquals(expected[orientation - 1], points, 0.001f)
        }
    }

    @Test fun emptyAndDamagedInputFailWithoutEncodingOrLeakingCache() {
        for (bytes in listOf(byteArrayOf(), byteArrayOf(1, 2, 3), byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10))) {
            val file = File.createTempFile("invalid_", ".png", context.cacheDir)
            try {
                file.writeBytes(bytes)
                assertThrows(IOException::class.java) { processor().prepare(photo(file)) }
                assertTrue(calls.isEmpty())
                assertNoTemporaryInput()
            } finally { file.delete() }
        }
    }

    @Test fun failedPhotoConversionCleansSnapshotAndOwnedBitmaps() = withBitmap { bitmap ->
        @Suppress("DEPRECATION")
        withPhoto(bitmap, Bitmap.CompressFormat.WEBP) { file ->
            val policy = processor(setOf(Bitmap.CompressFormat.JPEG to 90, Bitmap.CompressFormat.JPEG to 85))
            assertThrows(IOException::class.java) { policy.prepare(photo(file)) }
            assertEquals(listOf(Bitmap.CompressFormat.JPEG to 90, Bitmap.CompressFormat.JPEG to 85), calls)
            assertTrue(encodedBitmaps.all { it.isRecycled })
            assertNoTemporaryInput()
        }
    }

    @Test @Config(sdk = [26], manifest = Config.NONE)
    fun api26PngAndWebpProcessing() = withBitmap { bitmap ->
        withPhoto(bitmap, Bitmap.CompressFormat.PNG) { file ->
            assertArrayEquals(file.readBytes(), processor().prepare(photo(file)).bytes)
        }
        @Suppress("DEPRECATION")
        withPhoto(bitmap, Bitmap.CompressFormat.WEBP) { file ->
            assertEquals("image/jpeg", processor().prepare(photo(file)).mimeType)
        }
    }

    private fun photo(file: File) = PreprocessedImage.PhotoPicker(Uri.fromFile(file), null, "photo.dat", 1L)
    private fun withBitmap(block: (Bitmap) -> Unit) {
        val bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        try { block(bitmap) } finally { bitmap.recycle() }
    }
    private fun withPhoto(bitmap: Bitmap, format: Bitmap.CompressFormat, block: (File) -> Unit) {
        val file = File.createTempFile("photo_", ".dat", context.cacheDir)
        try {
            file.outputStream().use { check(bitmap.compress(format, 100, it)) }
            block(file)
        } finally { file.delete() }
    }
    private fun assertDimensions(upload: ImageUploadPreprocessor.Upload, width: Int, height: Int) {
        assertTrue(upload.bytes.size <= ImageUploadPreprocessor.MAX_BYTES)
        val bitmap = BitmapFactory.decodeByteArray(upload.bytes, 0, upload.bytes.size)!!
        try { assertEquals(width, bitmap.width); assertEquals(height, bitmap.height) }
        finally { bitmap.recycle() }
    }
    private fun assertNoTemporaryInput() {
        assertFalse(context.cacheDir.listFiles()!!.any { it.name.startsWith("analysis_input_") })
    }
    private fun writeOversize(output: OutputStream) {
        val chunk = ByteArray(8192)
        repeat(ImageUploadPreprocessor.MAX_BYTES / chunk.size) { output.write(chunk) }
        output.write(0)
    }
}
