package com.dundueni.app.data.remote.api

import java.io.IOException
import kotlinx.coroutines.delay
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.ForwardingSink
import okio.blackholeSink
import okio.buffer
import retrofit2.Response

/** 로컬 Flow 검증 전용. 이미지는 읽기만 하며 서버에 전송하거나 저장하지 않습니다. */
/**
 * 실제 서버 없이 입력 준비부터 결과 표시까지 확인하기 위한 API 구현이다.
 * AnalysisFlowDependencies가 현재 실행 흐름에 이 구현을 연결한다.
 * 이미지 본문을 읽어 비어 있는지 확인하지만 이미지 내용을 판별하지는 않는다.
 * 잠시 기다린 뒤 고정된 HIGH 응답을 반환해 결과 화면까지 진행하게 한다.
 * 실제 API와 같은 반환형을 사용하므로 Repository의 파싱 경로도 함께 확인한다.
 */
class FakeAnalysisApiService : AnalysisApiService {
    override suspend fun analyzeImage(image: MultipartBody.Part): Response<ResponseBody> {
        var size = 0L
        val sink = object : ForwardingSink(blackholeSink()) {
            override fun write(source: Buffer, byteCount: Long) {
                size += byteCount
                super.write(source, byteCount)
            }
        }.buffer()
        // 준비된 본문을 실제로 읽어 URI 읽기나 본문 생성 단계의 실패도 드러나게 한다.
        sink.use { image.body.writeTo(it) }
        if (size == 0L) throw IOException("Image body is empty")
        delay(600)
        return Response.success(MOCK_RESPONSE.toResponseBody())
    }

    private companion object {
        const val MOCK_RESPONSE = """{
            "risk_level":"HIGH",
            "reasons":["Mock 결과: 개인정보 입력을 요구하는 상황을 가정합니다."],
            "recommended_actions":["Mock 안내: 공식 채널에서 내용을 확인하세요."]
        }"""
    }
}
