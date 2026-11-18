package com.dundueni.app.feature.analysisresult

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import com.dundueni.app.data.repository.AnalysisResultRepository
import com.dundueni.app.data.repository.ImageSummaryResult
import com.dundueni.app.feature.history.HistoryType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ImageSummaryUiState(
    val result: ImageSummaryResult? = null,
    val checking: Boolean = true,
    val showRetry: Boolean = false,
    val lookupFailed: Boolean = false,
)

internal fun AnalysisResult.imageSummaryLookupId(): String? =
    id.takeIf {
        imageModelResult && summary.isBlank() &&
            (historyType == HistoryType.IMAGE || historyType == HistoryType.SCREEN)
    }

internal class ImageSummaryViewModel private constructor(
    private val analysisId: String,
    private val repository: AnalysisResultRepository,
    private val lookup: ImageSummaryLookup,
) : ViewModel() {
    val uiState = lookup.uiState

    /** Cancellation of the result screen's resumed lifecycle stops the delay or GET. */
    @MainThread
    suspend fun refreshWhileVisible() = lookup.refreshWhileVisible { repository.findImageSummary(analysisId) }

    @MainThread
    fun checkAgain() = lookup.checkAgain()

    companion object {
        private val lookupsById = mutableMapOf<String, ImageSummaryLookup>()

        /** Reopening the same result in this app process reuses its status and budget. */
        @MainThread
        fun forResult(analysisId: String, repository: AnalysisResultRepository): ImageSummaryViewModel {
            val lookup = lookupsById.getOrPut(analysisId) { ImageSummaryLookup() }
            return ImageSummaryViewModel(analysisId, repository, lookup)
        }
    }
}

/** With the current Repository, Unavailable includes the BE's initial null status. */
private fun ImageSummaryResult?.canRecheck() =
    this == ImageSummaryResult.Pending || this == ImageSummaryResult.Unavailable

private class ImageSummaryLookup {
    private val state = MutableStateFlow(ImageSummaryUiState())
    val uiState = state.asStateFlow()
    private val observation = Mutex()
    private val manualRequests = Channel<Unit>(Channel.CONFLATED)
    private var visible = false
    private var automaticAttempts = 0
    private var automaticFinished = false
    private var result: ImageSummaryResult? = null
    private var lookupFailed = false

    @MainThread
    suspend fun refreshWhileVisible(fetch: suspend () -> ImageSummaryResult) = observation.withLock {
        visible = true
        try {
            if (!automaticFinished) {
                while (automaticAttempts < MAX_AUTOMATIC_LOOKUPS && !lookupFailed &&
                    (result == null || result.canRecheck())
                ) {
                    publish(checking = true)
                    if (automaticAttempts > 0) delay(AUTOMATIC_INTERVAL_MS)
                    automaticAttempts++
                    lookup(fetch)
                }
                automaticFinished = true
            }

            while (result.canRecheck()) {
                publish()
                manualRequests.receive()
                lookup(fetch)
            }
        } finally {
            visible = false
            manualRequests.tryReceive()
            publish()
        }
    }

    @MainThread
    fun checkAgain() {
        val current = state.value
        if (!visible || !current.showRetry || current.checking) return
        publish(checking = true)
        manualRequests.trySend(Unit)
    }

    private suspend fun lookup(fetch: suspend () -> ImageSummaryResult) {
        val response = fetch()
        lookupFailed = response == ImageSummaryResult.LookupError
        if (!lookupFailed || !result.canRecheck()) result = response
    }

    private fun publish(checking: Boolean = false) {
        state.value = ImageSummaryUiState(
            result = result,
            checking = checking,
            showRetry = automaticFinished && result.canRecheck(),
            lookupFailed = lookupFailed,
        )
    }

    private companion object {
        const val MAX_AUTOMATIC_LOOKUPS = 12
        const val AUTOMATIC_INTERVAL_MS = 5_000L
    }
}
