package com.dundueni.app.data.remote.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.dundueni.app.data.image.ImagePreprocessor
import com.dundueni.app.data.model.ImageInput
import com.dundueni.app.data.model.PreprocessedImage
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import okhttp3.MultipartBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class ImageMultipartFactoryTest {
    private val resolver = RuntimeEnvironment.getApplication().contentResolver
    private val factory = ImageMultipartFactory(resolver)

    @Test
    fun photoPreservesBytesMetadataAndClosesStream() {
        val bytes = byteArrayOf(0, 1, 2, 127, -1)
        val uri = Uri.parse("content://test/photo/1")
        var closed = false
        val input = object : ByteArrayInputStream(bytes) {
            override fun close() {
                closed = true
                super.close()
            }
        }
        shadowOf(resolver).registerInputStream(uri, input)
        val image = PreprocessedImage.PhotoPicker(uri, "image/heic", "original.heic", 5L)

        val part = factory.createPart(image, "testImage")
        assertEquals("image/heic", part.body.contentType().toString())
        assertEquals(5L, part.body.contentLength())
        assertEquals(
            "form-data; name=\"testImage\"; filename=\"original.heic\"",
            part.headers?.get("Content-Disposition")
        )
        assertFalse(closed)
        val buffer = Buffer()
        part.body.writeTo(buffer)
        assertArrayEquals(bytes, buffer.readByteArray())
        assertTrue(closed)
    }

    @Test
    fun missingPhotoMetadataUsesFallbackAndUnknownLength() {
        val image = PreprocessedImage.PhotoPicker(Uri.parse("content://test/photo/2"), null, null, null)
        val part = factory.createPart(image, "anotherField")
        assertEquals("application/octet-stream", part.body.contentType().toString())
        assertEquals(-1L, part.body.contentLength())
        assertEquals(
            "form-data; name=\"anotherField\"; filename=\"image\"",
            part.headers?.get("Content-Disposition")
        )
        assertEquals(-1L, factory.createRequestBody(image.copy(sizeBytes = -10)).contentLength())
    }

    @Test
    fun photoInputPassesThroughPreprocessorIntoSerializedMultipart() {
        val file = java.io.File.createTempFile("multipart", ".bin", RuntimeEnvironment.getApplication().cacheDir)
        try {
            file.writeBytes(byteArrayOf(10, 20, 30))
            val image = ImagePreprocessor(resolver).preprocess(ImageInput.PhotoPicker(Uri.fromFile(file)))
            val part = factory.createPart(image, "testImage")
            val multipart = MultipartBody.Builder("test-boundary")
                .setType(MultipartBody.FORM)
                .addPart(part)
                .build()
            val buffer = Buffer()
            multipart.writeTo(buffer)
            val serialized = buffer.readByteArray()
            val text = serialized.toString(Charsets.ISO_8859_1)
            assertTrue(text.startsWith("--test-boundary\r\n"))
            assertTrue(text.contains("name=\"testImage\""))
            assertTrue(text.contains("\r\n\r\n" + byteArrayOf(10, 20, 30).toString(Charsets.ISO_8859_1)))
            assertTrue(text.endsWith("--test-boundary--\r\n"))
        } finally {
            file.delete()
        }
    }

    @Test
    fun screenCaptureIsPngWithoutResizeAndBodySurvivesOriginalRecycle() {
        val bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        val image = ImagePreprocessor(resolver).preprocess(ImageInput.ScreenCapture(bitmap))
        val part = factory.createPart(image, "testImage")
        assertEquals("image/png", part.body.contentType().toString())
        assertTrue(part.headers!!["Content-Disposition"]!!.contains("filename=\"capture.png\""))
        assertFalse(bitmap.isRecycled)
        bitmap.recycle()

        val first = Buffer().also { part.body.writeTo(it) }.readByteArray()
        val second = Buffer().also { part.body.writeTo(it) }.readByteArray()
        assertArrayEquals(first, second)
        assertEquals(first.size.toLong(), part.body.contentLength())
        assertArrayEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10), first.copyOfRange(0, 8))
        val decoded = BitmapFactory.decodeByteArray(first, 0, first.size)
        assertNotNull(decoded)
        assertEquals(3, decoded.width)
        assertEquals(2, decoded.height)
        assertEquals(Color.RED, decoded.getPixel(0, 0))
        decoded.recycle()
    }

    @Test
    fun missingUriPropagatesReadFailure() {
        val image = PreprocessedImage.PhotoPicker(Uri.parse("file:///nonexistent-pb20-image"), null, null, null)
        val body = factory.createRequestBody(image)
        assertThrows(FileNotFoundException::class.java) { body.writeTo(Buffer()) }
    }

    @Test
    fun rejectsBlankFieldAndRecycledBitmap() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val image = PreprocessedImage.ScreenCapture(bitmap)
        assertThrows(IllegalArgumentException::class.java) { factory.createPart(image, " ") }
        bitmap.recycle()
        assertThrows(IllegalArgumentException::class.java) { factory.createRequestBody(image) }
    }
}
