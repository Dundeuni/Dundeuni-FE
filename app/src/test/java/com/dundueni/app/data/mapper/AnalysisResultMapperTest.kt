package com.dundueni.app.data.mapper

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

    private fun parse(json: String): AnalysisResponseDto = gson.fromJson(json, AnalysisResponseDto::class.java)

    @Test
    fun backendEnvelopeMapsRawValuesAndObjectReasons() {
        val json = checkNotNull(javaClass.getResourceAsStream("/analysis_response_mock.json"))
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val retrofit = Retrofit.Builder()
            .baseUrl("https://example.invalid/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val converter: Converter<okhttp3.ResponseBody, AnalysisResponseDto> =
            retrofit.responseBodyConverter(AnalysisResponseDto::class.java, emptyArray())
        val dto = json.toResponseBody("application/json".toMediaType()).use {
            checkNotNull(converter.convert(it))
        }
        val result = dto.toDomain()
        assertEquals("analysis-example", result.analysisId)
        assertEquals("COMPLETED", result.status)
        assertEquals("NEW_SERVER_RISK", result.aiRiskLevel)
        assertEquals(82.4, result.aiGenerationScore!!, 0.0)
        assertEquals(listOf("reason one", "reason two"), result.reasons)
        assertEquals("model-example", result.modelVersion)
        assertNull(result.errorCode)
        assertEquals(emptyList<String>(), result.recommendedActions)
    }

    @Test
    fun nullableAndMissingResultFieldsStayNullAndCollectionsStayEmpty() {
        val result = parse("""{"result":{"status":"COMPLETED","reasons":null}}""").toDomain()
        assertNull(result.analysisId)
        assertNull(result.aiGenerationScore)
        assertNull(result.aiRiskLevel)
        assertTrue(result.reasons.isEmpty())
        assertTrue(result.recommendedActions.isEmpty())
    }

    @Test
    fun rawRiskTextAndReasonTextAreNotNormalized() {
        val result = parse("""{
            "result": {
                "aiRiskLevel":"  FUTURE_VALUE  ",
                "reasons":[{"description":" reason "},null,{"description":null}]
            }
        }""").toDomain()
        assertEquals("  FUTURE_VALUE  ", result.aiRiskLevel)
        assertEquals("  FUTURE_VALUE  ", result.riskLevel)
        assertEquals(listOf(" reason "), result.reasons)
    }

    @Test
    fun absentEnvelopeResultIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { parse("{}").toDomain() }
    }

    @Test
    fun incompatibleReasonsShapeFailsInsteadOfInventingResult() {
        assertThrows(JsonSyntaxException::class.java) {
            parse("""{"result":{"reasons":{"unexpected":"object"}}}""")
        }
    }
}
