package com.dundueni.app.data.remote.api

import android.content.ContentProvider
import android.content.ContentValues
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import com.dundueni.app.data.model.ImageInput
import com.dundueni.app.data.remote.network.RetrofitClient
import com.dundueni.app.data.repository.AnalysisRepository
import com.dundueni.app.feature.analysis.AnalysisInputProcessor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AnalysisApiServiceTest {
    private val resolver = RuntimeEnvironment.getApplication().contentResolver
    private val processor = AnalysisInputProcessor(resolver, RuntimeEnvironment.getApplication().cacheDir)

    @Test
    fun photoPickerPreservesJpegAndPngMetadataAndBytes() = runBlocking {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        try {
            for ((format, mime, filename) in listOf(
                Triple(Bitmap.CompressFormat.JPEG, "image/jpeg", "original.jpeg"),
                Triple(Bitmap.CompressFormat.PNG, "image/png", "original.png")
            )) {
                val bytes = encode(bitmap, format)
                val uri = Uri.parse("content://analysis.test/$filename")
                ShadowContentResolver.registerProviderInternal(
                    "analysis.test", PhotoProvider(mime, filename, bytes.size.toLong())
                )
                shadowOf(resolver).registerInputStream(uri, ByteArrayInputStream(bytes))
                val part = processor.prepare(ImageInput.PhotoPicker(uri))
                assertUpload(part, mime, filename, bytes, "/")
            }
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun screenCaptureUsesImageTypeAndPngFile() = runBlocking {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        try {
            val bytes = encode(bitmap, Bitmap.CompressFormat.PNG)
            val part = processor.prepare(ImageInput.ScreenCapture(bitmap))
            assertUpload(part, "image/png", "capture.png", bytes, "/api/")
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun savedCaptureUsesSameContractAndPreservesHttpError() = runBlocking {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val file = File.createTempFile("capture_", ".png", RuntimeEnvironment.getApplication().cacheDir)
        try {
            val bytes = encode(bitmap, Bitmap.CompressFormat.PNG)
            file.writeBytes(bytes)
            val part = processor.prepareCaptureFile(file)
            assertUpload(part, "image/png", "capture.png", bytes, "/v1/", checkError = true)
        } finally {
            bitmap.recycle()
            file.delete()
        }
    }

    private suspend fun assertUpload(
        part: MultipartBody.Part,
        mime: String,
        filename: String,
        bytes: ByteArray,
        basePath: String,
        checkError: Boolean = false
    ) {
        val server = MockWebServer()
        server.start()
        try {
            val repository = AnalysisRepository(RetrofitClient(server.url(basePath).toString()).analysisApiService)
            // 요청 계약만 검증하며 기존 응답 파싱 계약은 변경하지 않는다.
            server.enqueue(MockResponse().setResponseCode(202).setBody("raw result"))
            val response = repository.analyzeImage(part)
            assertEquals(202, response.code())
            response.body()!!.use { assertEquals("raw result", it.string()) }
            assertMultipart(server.takeRequest(5, TimeUnit.SECONDS)!!, mime, filename, bytes)

            if (checkError) {
                server.enqueue(MockResponse().setResponseCode(400).setBody("raw error"))
                val error = repository.analyzeImage(part)
                assertEquals(400, error.code())
                error.errorBody()!!.use { assertEquals("raw error", it.string()) }
                assertMultipart(server.takeRequest(5, TimeUnit.SECONDS)!!, mime, filename, bytes)
            }
            assertEquals(if (checkError) 2 else 1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    private fun assertMultipart(request: RecordedRequest, mime: String, filename: String, bytes: ByteArray) {
        assertEquals("POST", request.method)
        assertEquals("/api/analysis", request.path)
        val contentType = request.getHeader("Content-Type")!!
        assertTrue(contentType.startsWith("multipart/form-data; boundary="))
        val boundary = contentType.substringAfter("boundary=")
        // ISO-8859-1은 모든 바이트를 보존하므로 바이너리 본문도 정확히 비교할 수 있다.
        val body = request.body.readByteArray().toString(Charsets.ISO_8859_1)
        val sections = body.split("--$boundary")
        assertEquals("Exactly two multipart parts", 4, sections.size)
        assertEquals("", sections.first())
        assertEquals("--\r\n", sections.last())
        val parts = sections.subList(1, 3).map { section ->
            assertTrue(section.startsWith("\r\n"))
            assertTrue(section.endsWith("\r\n"))
            val content = section.removePrefix("\r\n").removeSuffix("\r\n")
            content.substringBefore("\r\n\r\n") to content.substringAfter("\r\n\r\n")
        }
        val type = parts.single { (headers, _) -> headers.contains("name=\"type\"") }
        assertFalse(type.first.contains("filename="))
        assertEquals("IMAGE", type.second)
        val file = parts.single { (headers, _) -> headers.contains("name=\"file\"") }
        assertTrue(file.first.contains("name=\"file\"; filename=\"$filename\""))
        assertTrue(file.first.split("\r\n").contains("Content-Type: $mime"))
        assertArrayEquals(bytes, file.second.toByteArray(Charsets.ISO_8859_1))
    }

    private fun encode(bitmap: Bitmap, format: Bitmap.CompressFormat): ByteArray =
        ByteArrayOutputStream().use {
            check(bitmap.compress(format, 100, it))
            it.toByteArray()
        }

    private class PhotoProvider(
        private val mime: String,
        private val filename: String,
        private val size: Long
    ) : ContentProvider() {
        override fun onCreate() = true
        override fun getType(uri: Uri) = mime
        override fun query(
            uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?
        ) = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)).apply {
            addRow(arrayOf(filename, size))
        }
        override fun insert(uri: Uri, values: ContentValues?): Uri? = error("Read only")
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = error("Read only")
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = error("Read only")
    }
}
