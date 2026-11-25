package com.dundueni.app.feature.textinput

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.dundueni.app.data.model.AnalysisResult
import com.dundueni.app.data.model.ExtractedPhoneNumber
import com.dundueni.app.data.remote.api.ExtractedPhoneNumberDto
import com.dundueni.app.data.remote.api.TextAnalysisApiService
import com.dundueni.app.data.remote.api.TextAnalysisRequest
import com.dundueni.app.data.remote.api.TextAnalysisResponse
import com.dundueni.app.data.remote.api.TextAnalysisResponseResult
import com.dundueni.app.data.repository.AnalysisExecutionStatus
import com.dundueni.app.data.repository.AnalysisNotCompletedException
import com.dundueni.app.data.repository.AnalysisResultRepository
import com.dundueni.app.feature.analysisresult.AnalysisResultJsonAdapter
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import retrofit2.Response

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

    @Test fun queuedTextPostPreservesPhonesThroughDetailRetriesAndClearsThemForTheNextInspection() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val posts = mutableListOf<String>()
            val gets = mutableListOf<String>()
            val phone = ExtractedPhoneNumber("01012345678", "010-1234-5678", "TEXT")
            val api = object : TextAnalysisApiService {
                override suspend fun analyzeText(request: TextAnalysisRequest): Response<TextAnalysisResponse> {
                    posts += request.text
                    val result = if (posts.size == 1) {
                        TextAnalysisResponseResult(
                            "text-id", "TEXT", "QUEUED",
                            extractedPhoneNumbers = listOf(
                                ExtractedPhoneNumberDto(phone.phoneNumber, phone.displayNumber, phone.source),
                            ),
                        )
                    } else TextAnalysisResponseResult("new-id", "TEXT", "COMPLETED")
                    return Response.success(202, TextAnalysisResponse(true, "ANALYSIS202", result))
                }

                override suspend fun analyzeScreenCrop(
                    file: MultipartBody.Part, type: RequestBody, sourceType: RequestBody,
                ): Response<TextAnalysisResponse> = error("Unexpected capture")
            }
            val repository = object : AnalysisResultRepository {
                override suspend fun analyzeResult(image: MultipartBody.Part): AnalysisResult = error("Unexpected image")

                override suspend fun findResult(analysisId: String): AnalysisResult {
                    gets += analysisId
                    if (gets.size == 1) throw IOException("offline")
                    if (gets.size == 2) throw AnalysisNotCompletedException(
                        AnalysisExecutionStatus.ANALYZING, analysisId, null,
                    )
                    return AnalysisResult(
                        analysisId, "TEXT", "COMPLETED", null, null, emptyList(),
                        null, null, null, null, null, textDecision = "phishing",
                        textEvidence = listOf("GET reason"), textLimitations = listOf("GET limitation"),
                    )
                }
            }
            val vm = TextInspectionViewModel(
                RuntimeEnvironment.getApplication(), SavedStateHandle(), textApi = { api }, repository = { repository },
            )
            vm.inspect("original text")
            assertEquals(TextInspectionUiState.Loading, vm.uiState.value)
            assertTrue(vm.hasPendingLookup)
            advanceTimeBy(2_000)
            runCurrent()
            assertEquals(TextInspectionUiState.Error, vm.uiState.value)
            vm.retry()
            assertEquals(TextInspectionUiState.Pending("text-id"), vm.uiState.value)
            vm.retry()
            val success = vm.uiState.value as TextInspectionUiState.Success
            val result = AnalysisResultJsonAdapter.decode(success.resultJson)
            assertEquals(listOf(phone), result.extractedPhoneNumbers)
            assertEquals("original text", result.sourceText)
            assertEquals(listOf("GET reason"), result.evidence.map { it.description })
            assertEquals(listOf("GET limitation"), result.textLimitations)
            assertEquals(listOf("original text"), posts)
            assertEquals(List(3) { "text-id" }, gets)

            vm.onResultHandled(success.resultJson)
            vm.inspect("new request")
            val next = AnalysisResultJsonAdapter.decode((vm.uiState.value as TextInspectionUiState.Success).resultJson)
            assertEquals("new-id", next.id)
            assertTrue(next.extractedPhoneNumbers.isEmpty())
            assertEquals(listOf("original text", "new request"), posts)
            assertEquals(List(3) { "text-id" } + "new-id", gets)
        } finally { Dispatchers.resetMain() }
    }

}
