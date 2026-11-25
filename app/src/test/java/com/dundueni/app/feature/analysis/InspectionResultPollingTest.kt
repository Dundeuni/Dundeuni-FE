package com.dundueni.app.feature.analysis

import com.dundueni.app.data.repository.AnalysisExecutionStatus
import com.dundueni.app.data.repository.AnalysisNotCompletedException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InspectionResultPollingTest {
    @Test
    fun pollsSameIdUntilComplete() = runTest {
        val queries = mutableListOf<Pair<String, Long>>()
        val result = pollPendingInspection(pending(AnalysisExecutionStatus.QUEUED)) { id ->
            queries.add(id to testScheduler.currentTime)
            if (queries.size == 1) throw pending(AnalysisExecutionStatus.ANALYZING)
            "complete"
        }

        assertEquals("complete", result)
        assertEquals(listOf("analysis-id" to 2_000L, "analysis-id" to 4_000L), queries)
    }

    @Test
    fun timeoutPreservesIdAndMostRecentStatus() = runTest {
        val failure = runCatching {
            pollPendingInspection<String>(pending(AnalysisExecutionStatus.QUEUED)) {
                throw pending(AnalysisExecutionStatus.ANALYZING)
            }
        }.exceptionOrNull() as AnalysisNotCompletedException

        assertEquals("analysis-id", failure.analysisId)
        assertEquals(AnalysisExecutionStatus.ANALYZING, failure.status)
        assertEquals(60_000L, testScheduler.currentTime)
    }

    private fun pending(status: AnalysisExecutionStatus) =
        AnalysisNotCompletedException(status, "analysis-id", null)
}
