package com.dundueni.app.feature.textinput

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.dundueni.app.feature.history.HistoryType
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class TextInspectionViewModelTest {
    @Test fun directTextRequiresTrimmedLength() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val viewModel = TextInspectionViewModel(RuntimeEnvironment.getApplication(), SavedStateHandle())
            viewModel.inspect(" 1234 ")
            assertEquals(TextInspectionUiState.InvalidInput, viewModel.uiState.value)
            viewModel.inspect("x".repeat(3_001))
            assertEquals(TextInspectionUiState.InvalidInput, viewModel.uiState.value)
            viewModel.updateInputText(" 12345 ")
            assertEquals(TextInspectionUiState.Idle, viewModel.uiState.value)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun savedInputSurvivesRecreation() {
        val application: Application = RuntimeEnvironment.getApplication()
        val savedState = SavedStateHandle()
        val viewModel = TextInspectionViewModel(application, savedState)
        viewModel.updateInputText("PB-03 and PB-18")
        val restored = TextInspectionViewModel(
            application,
            SavedStateHandle(mapOf("inputText" to savedState.get<String>("inputText"))),
        )
        assertEquals("PB-03 and PB-18", restored.inputText.value)
    }

    @Test fun realCompletedPostUsesGetAndRetriesSameIdWithoutMockFallback() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            var posts = 0
            var gets = 0
            val api = object : com.dundueni.app.data.remote.api.TextAnalysisApiService {
                override suspend fun analyzeText(request: com.dundueni.app.data.remote.api.TextAnalysisRequest): retrofit2.Response<com.dundueni.app.data.remote.api.TextAnalysisResponse> {
                    posts++
                    assertEquals("original text", request.text)
                    return retrofit2.Response.success(202, com.dundueni.app.data.remote.api.TextAnalysisResponse(
                        true, "ANALYSIS202", com.dundueni.app.data.remote.api.TextAnalysisResponseResult("text-id", "TEXT", "COMPLETED")))
                }
                override suspend fun analyzeScreenCrop(file: okhttp3.MultipartBody.Part, type: okhttp3.RequestBody, sourceType: okhttp3.RequestBody): retrofit2.Response<com.dundueni.app.data.remote.api.TextAnalysisResponse> = error("Unexpected capture")
            }
            val repository = object : com.dundueni.app.data.repository.AnalysisResultRepository {
                override suspend fun analyzeResult(image: okhttp3.MultipartBody.Part): com.dundueni.app.data.model.AnalysisResult = error("Unexpected image")
                override suspend fun findResult(analysisId: String): com.dundueni.app.data.model.AnalysisResult {
                    assertEquals("text-id", analysisId)
                    gets++
                    if (gets == 1) throw java.io.IOException("offline")
                    if (gets == 2) throw com.dundueni.app.data.repository.AnalysisNotCompletedException(
                        com.dundueni.app.data.repository.AnalysisExecutionStatus.ANALYZING, analysisId, null)
                    return com.dundueni.app.data.model.AnalysisResult(
                        analysisId, "TEXT", "COMPLETED", null, null, listOf("GET reason"),
                        null, null, null, null, null, recommendedActions = listOf("GET guidance"),
                        summary = "GET summary", normalizedRiskLevel = "UNKNOWN", textDecision = null)
                }
            }
            val vm = TextInspectionViewModel(RuntimeEnvironment.getApplication(), SavedStateHandle(),
                textApi = { api }, repository = { repository })
            vm.inspect("original text")
            assertEquals(TextInspectionUiState.Error, vm.uiState.value)
            assertTrue(vm.hasPendingLookup)
            vm.retry()
            assertEquals(TextInspectionUiState.Pending("text-id"), vm.uiState.value)
            vm.retry()
            val json = JsonParser.parseString((vm.uiState.value as TextInspectionUiState.Success).resultJson).asJsonObject
            assertEquals("GET summary", json.get("summary").asString)
            assertEquals("original text", json.get("sourceText").asString)
            assertEquals("UNKNOWN", json.get("riskLevel").asString)
            assertEquals("GET reason", json.getAsJsonArray("evidence")[0].asJsonObject.get("description").asString)
            assertEquals("GET guidance", json.getAsJsonArray("guidance")[0].asString)
            assertTrue(!json.has("fraudScore"))
            assertEquals(1, posts); assertEquals(3, gets)
        } finally { Dispatchers.resetMain() }
    }

}
