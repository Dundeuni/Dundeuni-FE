package com.dundueni.app.data.remote.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.dundueni.app.data.image.ImagePreprocessor
import com.dundueni.app.data.model.ImageInput
import com.dundueni.app.data.model.PreprocessedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImageMultipartFactoryTest {
    private val context = RuntimeEnvironment.getApplication()
    private val resolver = context.contentResolver
    private val factory = ImageMultipartFactory(resolver, context.cacheDir)

    @Test
    fun photoUsesActualBytesDespiteWrongMetadataAndClosesStream() {
        val bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it); it.toByteArray() }
        bitmap.recycle()
        val uri = Uri.parse("content://test/photo/1")
        var closed = false
        val input = object : ByteArrayInputStream(bytes) {
            override fun close() { closed = true; super.close() }
        }
        shadowOf(resolver).registerInputStream(uri, input)
        val part = factory.createPart(PreprocessedImage.PhotoPicker(uri, "image/heic", "original.heic", 1L), "file")
        assertTrue(closed)
        assertEquals("image/png", part.body.contentType().toString())
        assertEquals(bytes.size.toLong(), part.body.contentLength())
        assertTrue(part.headers!!["Content-Disposition"]!!.contains("filename=\"original.png\""))
        assertArrayEquals(bytes, Buffer().also { part.body.writeTo(it) }.readByteArray())
        assertArrayEquals(bytes, Buffer().also { part.body.writeTo(it) }.readByteArray())
        assertFalse(context.cacheDir.listFiles()!!.any { it.name.startsWith("analysis_input_") })
    }

    @Test
    fun photoInputWithMissingMetadataPreservesJpegBytes() {
        val file = File.createTempFile("multipart", ".bin", context.cacheDir)
        val bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            val bytes = file.readBytes()
            val metadata = ImagePreprocessor(resolver).preprocess(ImageInput.PhotoPicker(Uri.fromFile(file)))
            val part = factory.createPart(metadata.copy(mimeType = null, displayName = null, sizeBytes = null), "file")
            assertEquals("image/jpeg", part.body.contentType().toString())
            assertTrue(part.headers!!["Content-Disposition"]!!.contains("filename=\"image.jpg\""))
            assertArrayEquals(bytes, Buffer().also { part.body.writeTo(it) }.readByteArray())
        } finally {
            bitmap.recycle()
            file.delete()
        }
    }

    @Test
    fun screenCaptureIsPngWithoutResizeAndBodySurvivesOriginalRecycle() {
        val bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        val image = ImagePreprocessor(resolver).preprocess(ImageInput.ScreenCapture(bitmap))
        val part = factory.createPart(image, "file")
        assertEquals("image/png", part.body.contentType().toString())
        assertTrue(part.headers!!["Content-Disposition"]!!.contains("filename=\"capture.png\""))
        assertFalse(bitmap.isRecycled)
        bitmap.recycle()
        val first = Buffer().also { part.body.writeTo(it) }.readByteArray()
        assertArrayEquals(first, Buffer().also { part.body.writeTo(it) }.readByteArray())
        assertEquals(first.size.toLong(), part.body.contentLength())
        val decoded = BitmapFactory.decodeByteArray(first, 0, first.size)
        assertNotNull(decoded)
        assertEquals(3, decoded.width)
        assertEquals(2, decoded.height)
        assertEquals(Color.RED, decoded.getPixel(0, 0))
        decoded.recycle()
    }

    @Test
    fun missingUriFailsBeforeRequestBodyIsCreated() {
        val image = PreprocessedImage.PhotoPicker(Uri.parse("file:///nonexistent-pb02-image"), null, null, null)
        assertThrows(FileNotFoundException::class.java) { factory.createRequestBody(image) }
        assertFalse(context.cacheDir.listFiles()!!.any { it.name.startsWith("analysis_input_") })
    }

    @Test
    fun rejectsBlankFieldAndRecycledBitmap() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val image = PreprocessedImage.ScreenCapture(bitmap)
        assertThrows(IllegalArgumentException::class.java) { factory.createPart(image, " ") }
        bitmap.recycle()
        assertThrows(IllegalStateException::class.java) { factory.createRequestBody(image) }
    }
}
