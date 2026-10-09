package com.dundueni.app.data.remote.api

import java.io.IOException
import kotlinx.coroutines.delay
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.ForwardingSink
import okio.blackholeSink
import okio.buffer
import retrofit2.Response

/** Local-only API implementation used to exercise the image-to-result flow without a server. */
class FakeAnalysisApiService : AnalysisApiService {
    override suspend fun analyzeImage(image: MultipartBody.Part, type: RequestBody): Response<ResponseBody> {
        val requestType = Buffer().also { type.writeTo(it) }.readUtf8()
        require(requestType == AnalysisApiContract.IMAGE_TYPE) { "Only IMAGE analysis is supported" }
        var size = 0L
        val sink = object : ForwardingSink(blackholeSink()) {
            override fun write(source: Buffer, byteCount: Long) {
                size += byteCount
                super.write(source, byteCount)
            }
        }.buffer()
        sink.use { image.body.writeTo(it) }
        if (size == 0L) throw IOException("Image body is empty")
        delay(600)
        return Response.success(202, MOCK_RESPONSE.toResponseBody())
    }

    private companion object {
        // Deliberately synthetic test data; this is not an AI result.
        const val MOCK_RESPONSE = """{
            "isSuccess":true,
            "code":"ANALYSIS202",
            "message":"Mock response for local flow verification",
            "result":{
                "analysisId":"mock-analysis-id",
                "type":"IMAGE",
                "status":"COMPLETED",
                "aiGenerationScore":82.4,
                "aiRiskLevel":"MOCK_CAUTION",
                "reasons":[{"description":"Mock test data: verify the source before sharing."}],
                "modelVersion":"mock-test",
                "errorCode":null,
                "createdAt":"2026-01-01T00:00:00.000Z",
                "completedAt":"2026-01-01T00:00:01.000Z",
                "expiresAt":"2026-01-08T00:00:01.000Z"
            }
        }"""
    }
}
