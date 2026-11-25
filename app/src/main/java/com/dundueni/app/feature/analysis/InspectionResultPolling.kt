package com.dundueni.app.feature.analysis

import com.dundueni.app.data.repository.AnalysisNotCompletedException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private const val POLL_INTERVAL_MS = 2_000L
private const val POLL_TIMEOUT_MS = 60_000L

/** Polls only the accepted analysis ID and preserves it for manual retry on timeout. */
internal suspend fun <T : Any> pollPendingInspection(
    initial: AnalysisNotCompletedException,
    lookup: suspend (String) -> T,
): T {
    val id = initial.analysisId?.takeIf { it.isNotBlank() } ?: throw initial
    var lastPending = initial
    val completed = withTimeoutOrNull(POLL_TIMEOUT_MS) {
        var result: T? = null
        while (result == null) {
            delay(POLL_INTERVAL_MS)
            try {
                result = lookup(id)
            } catch (pending: AnalysisNotCompletedException) {
                lastPending = pending
            }
        }
        result
    }
    return completed ?: throw lastPending
}
