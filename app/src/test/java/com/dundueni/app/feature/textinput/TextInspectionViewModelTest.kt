package com.dundueni.app.feature.textinput

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

@OptIn(ExperimentalCoroutinesApi::class)
class TextInspectionViewModelTest {
    @Test fun savedInputAndRetryKeepTheTextResultType() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val savedState = SavedStateHandle()
            val viewModel = TextInspectionViewModel(savedState)
            viewModel.updateInputText("PB-03 and PB-18")
            val restored = TextInspectionViewModel(
                SavedStateHandle(mapOf("inputText" to savedState.get<String>("inputText"))),
            )
            assertEquals("PB-03 and PB-18", restored.inputText.value)

            val states = mutableListOf<TextInspectionUiState>()
            val observer = launch(Dispatchers.Unconfined) { viewModel.uiState.collect { states += it } }
            try {
                viewModel.inspect(viewModel.inputText.value)
                val first = withTimeout(5_000) {
                    viewModel.uiState.filterIsInstance<TextInspectionUiState.Success>().first()
                }
                assertTrue(states.contains(TextInspectionUiState.Loading))
                val firstJson = JsonParser.parseString(first.resultJson).asJsonObject
                assertEquals(HistoryType.TEXT.name, firstJson.get("historyType").asString)
                assertEquals("PB-03 and PB-18", firstJson.get("sourceText").asString)

                viewModel.onResultOpenFailure()
                assertEquals(TextInspectionUiState.Error, viewModel.uiState.value)
                viewModel.retry()
                val retried = withTimeout(5_000) {
                    viewModel.uiState.filterIsInstance<TextInspectionUiState.Success>().first()
                }
                val retriedJson = JsonParser.parseString(retried.resultJson).asJsonObject
                assertEquals("PB-03 and PB-18", retriedJson.get("sourceText").asString)
                assertEquals(HistoryType.TEXT.name, retriedJson.get("historyType").asString)
            } finally {
                observer.cancel()
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
}
