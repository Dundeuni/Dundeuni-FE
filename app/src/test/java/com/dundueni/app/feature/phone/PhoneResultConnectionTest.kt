package com.dundueni.app.feature.phone

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.lifecycle.ViewModelProvider
import com.dundueni.app.data.remote.api.PhoneAnalysisResponseDto
import com.dundueni.app.feature.analysisresult.AnalysisResultActivity
import com.dundueni.app.feature.analysisresult.AnalysisResultJsonAdapter
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.feature.history.HistoryType
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PhoneResultConnectionTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val response = Gson().fromJson(
        """{"analysisId":"d13ea8aa-9efa-4cc0-86e4-cb46e20e2bbc","status":"COMPLETED"}""",
        PhoneAnalysisResponseDto::class.java,
    )

    @Test fun phonePostRoundTripStaysNeutralWithoutInventedContent() {
        val decoded = AnalysisResultJsonAdapter.decode(Gson().toJson(response.toPb05Result()))
        assertEquals(response.analysisId, decoded.id)
        assertEquals(HistoryType.PHONE, decoded.historyType)
        assertEquals(RiskLevel.UNKNOWN, decoded.riskLevel)
        assertTrue(decoded.evidence.isEmpty())
        assertTrue(decoded.guidance.isEmpty())
        assertNull(decoded.fraudScore)
        assertNull(decoded.aiScore)
    }

    @Test fun realSuccessOpensExistingPb05ActivityOnceAndConsumesSuccess() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("basic_inspection_quota", Context.MODE_PRIVATE).edit().clear().commit()
        ActivityScenario.launch<PhoneInputActivity>(Intent(context, PhoneInputActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                val vm = ViewModelProvider(activity)[PhoneInspectionViewModel::class.java]
                val field = PhoneInspectionViewModel::class.java.getDeclaredField("mutableUiState").apply { isAccessible = true }
                @Suppress("UNCHECKED_CAST")
                val state = field.get(vm) as MutableStateFlow<PhoneInspectionUiState>
                state.value = PhoneInspectionUiState.Success(response)
            }
            compose.waitForIdle()
            scenario.onActivity { activity ->
                val intent = shadowOf(activity).nextStartedActivity
                assertEquals(AnalysisResultActivity::class.java.name, intent.component!!.className)
                assertFalse(intent.getBooleanExtra(AnalysisResultActivity.EXTRA_IS_DEMO, true))
                val result = AnalysisResultJsonAdapter.decode(intent.getStringExtra(AnalysisResultActivity.EXTRA_RESULT_JSON)!!)
                assertEquals(RiskLevel.UNKNOWN, result.riskLevel)
                assertEquals(HistoryType.PHONE, result.historyType)
                assertEquals(PhoneInspectionUiState.Idle, ViewModelProvider(activity)[PhoneInspectionViewModel::class.java].uiState.value)
                assertNull(shadowOf(activity).nextStartedActivity)
            }
        }
    }

    @Test fun pendingOrMalformedPhoneResponsesDoNotProduceCompletedResults() {
        assertThrows(IllegalArgumentException::class.java) { response.copy(status = "QUEUED").toPb05Result() }
        assertThrows(IllegalArgumentException::class.java) { response.copy(analysisId = null).toPb05Result() }
    }
}
