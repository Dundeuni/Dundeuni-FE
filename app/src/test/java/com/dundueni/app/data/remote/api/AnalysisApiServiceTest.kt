package com.dundueni.app.data.remote.api

import android.graphics.Bitmap
import com.dundueni.app.data.model.PreprocessedImage
import com.dundueni.app.data.remote.network.ImageMultipartFactory
import com.dundueni.app.data.remote.network.RetrofitClient
import com.dundueni.app.data.repository.AnalysisRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AnalysisApiServiceTest {
    @Test
    fun sendsFactoryPartThroughRepositoryAndExistingRetrofitClient() = runBlocking {
        val server = MockWebServer()
        server.start()
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        try {
            val part = ImageMultipartFactory(RuntimeEnvironment.getApplication().contentResolver)
                .createPart(PreprocessedImage.ScreenCapture(bitmap), AnalysisApiContract.IMAGE_FIELD)
            val expectedBytes = Buffer().also { part.body.writeTo(it) }.readByteArray()
            val repository = AnalysisRepository(RetrofitClient(server.url("/v1/").toString()).analysisApiService)

            // 응답 스키마를 가정하지 않도록 JSON이 아닌 원문을 사용합니다.
            server.enqueue(MockResponse().setResponseCode(200).setBody("raw result"))
            val response = repository.analyzeImage(part)
            assertEquals(200, response.code())
            response.body()!!.use { assertEquals("raw result", it.string()) }

            val request = server.takeRequest(5, TimeUnit.SECONDS)!!
            assertEquals("POST", request.method)
            assertEquals("/v1/analysis", request.path)
            val contentType = request.getHeader("Content-Type")!!
            assertTrue(contentType.startsWith("multipart/form-data; boundary="))
            val boundary = contentType.substringAfter("boundary=")
            val body = request.body.readByteArray().toString(Charsets.ISO_8859_1)
            assertTrue(body.startsWith("--$boundary\r\n"))
            assertTrue(body.contains("name=\"image\"; filename=\"capture.png\""))
            assertTrue(body.contains("Content-Type: image/png\r\n"))
            assertTrue(body.contains("\r\n\r\n${expectedBytes.toString(Charsets.ISO_8859_1)}\r\n"))
            assertTrue(body.endsWith("--$boundary--\r\n"))

            server.enqueue(MockResponse().setResponseCode(400).setBody("raw error"))
            val error = repository.analyzeImage(part)
            assertEquals(400, error.code())
            error.errorBody()!!.use { assertEquals("raw error", it.string()) }
            assertEquals(2, server.requestCount)
        } finally {
            bitmap.recycle()
            server.shutdown()
        }
    }
}
