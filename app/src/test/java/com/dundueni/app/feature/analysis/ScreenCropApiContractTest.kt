package com.dundueni.app.feature.analysis

import com.dundueni.app.data.remote.network.RetrofitClient
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenCropApiContractTest {
    @Test
    fun screenCropUsesMultipartAnalysisEndpointAndMapsCompletedPhishingDecision() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(202).setBody(
                """{"isSuccess":true,"code":"ANALYSIS202","result":{"analysisId":"ocr-id","type":"TEXT","status":"COMPLETED","textResult":{"decision":"phishing","riskScore":0.87,"scoreMeaning":"UNCALIBRATED_CLASSIFIER_SCORE","sourceType":"SCREEN_CROP"}}}"""
            ))
            val file = MultipartBody.Part.createFormData(
                "file", "capture.png", "png-bytes".toRequestBody("image/png".toMediaType())
            )
            val response = RetrofitClient(server.url("/").toString())
                .textAnalysisApiService.analyzeScreenCrop(file)
            assertEquals(202, response.code())
            val mapped = response.toCompletedScreenCrop()
            assertEquals("ocr-id", mapped.analysisId)
            assertEquals("phishing", mapped.decision)
            assertEquals("피싱 의심", mapped.label)

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/analysis", request.path)
            assertTrue(request.getHeader("Content-Type")!!.startsWith("multipart/form-data; boundary="))
            val multipart = request.body.readUtf8()
            assertEquals(3, Regex("Content-Disposition:").findAll(multipart).count())
            assertTrue(multipart.contains("name=\"type\""))
            assertTrue(multipart.contains("\r\nTEXT\r\n"))
            assertTrue(multipart.contains("name=\"sourceType\""))
            assertTrue(multipart.contains("\r\nSCREEN_CROP\r\n"))
            assertTrue(multipart.contains("name=\"file\"; filename=\"capture.png\""))
            assertTrue(multipart.contains("png-bytes"))
            assertFalse(multipart.contains("name=\"text\""))
        } finally {
            server.shutdown()
        }
    }
}
