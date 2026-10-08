package com.dundueni.app.data.mapper

import com.dundueni.app.data.model.RiskLevel
import com.dundueni.app.data.remote.dto.AnalysisResponseDto
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AnalysisResultMapperTest {
    private val gson = Gson()

    private fun parse(json: String): AnalysisResponseDto =
        gson.fromJson(json, AnalysisResponseDto::class.java)

    @Test
    fun mockJsonParsesAndMapsThroughConfiguredConverterType() {
        val json = checkNotNull(javaClass.getResourceAsStream("/analysis_response_mock.json"))
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        // Converter만 사용하며 네트워크 호출은 하지 않습니다.
        val retrofit = Retrofit.Builder()
            .baseUrl("https://example.invalid/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val converter: Converter<okhttp3.ResponseBody, AnalysisResponseDto> =
            retrofit.responseBodyConverter(AnalysisResponseDto::class.java, emptyArray())
        val dto = json.toResponseBody("application/json".toMediaType()).use {
            checkNotNull(converter.convert(it))
        }
        assertEquals("HIGH", dto.riskLevel)
        val result = dto.toDomain()
        assertEquals(RiskLevel.HIGH, result.riskLevel)
        assertEquals(listOf("출처를 확인할 수 없는 링크가 포함되어 있습니다.", "개인정보 입력을 요구합니다."), result.reasons)
        assertEquals(listOf("링크를 열기 전에 발신자를 확인하세요.", "공식 채널에서 안내 내용을 확인하세요."), result.recommendedActions)
    }

    @Test
    fun mapsAllProvisionalRiskLevelsAndDoesNotTreatUnknownAsLow() {
        for ((raw, expected) in mapOf(
            "LOW" to RiskLevel.LOW,
            "MEDIUM" to RiskLevel.MEDIUM,
            "HIGH" to RiskLevel.HIGH,
            "NEW_SERVER_VALUE" to RiskLevel.UNKNOWN,
            "" to RiskLevel.UNKNOWN
        )) {
            assertEquals(expected, parse("""{"risk_level":"$raw"}""").toDomain().riskLevel)
        }
    }

    @Test
    fun missingAndNullFieldsProduceUnknownAndEmptyLists() {
        for (json in listOf("{}", """{"risk_level":null,"reasons":null,"recommended_actions":null}""")) {
            val result = parse(json).toDomain()
            assertEquals(RiskLevel.UNKNOWN, result.riskLevel)
            assertTrue(result.reasons.isEmpty())
            assertTrue(result.recommendedActions.isEmpty())
        }
    }

    @Test
    fun ignoresExtraFieldsAndNullItemsWhilePreservingTextAndOrder() {
        val result = parse("""{
            "risk_level":"MEDIUM",
            "reasons":[" second ",null,"first"],
            "recommended_actions":[null,"확인하세요"],
            "future_field":{"value":true}
        }""").toDomain()
        assertEquals(RiskLevel.MEDIUM, result.riskLevel)
        assertEquals(listOf(" second ", "first"), result.reasons)
        assertEquals(listOf("확인하세요"), result.recommendedActions)
        val empty = parse("""{"reasons":[],"recommended_actions":[]}""").toDomain()
        assertTrue(empty.reasons.isEmpty())
        assertTrue(empty.recommendedActions.isEmpty())
    }

    @Test
    fun incompatibleJsonShapeFailsInsteadOfInventingResult() {
        assertThrows(JsonSyntaxException::class.java) {
            parse("""{"reasons":{"unexpected":"object"}}""")
        }
    }
}
