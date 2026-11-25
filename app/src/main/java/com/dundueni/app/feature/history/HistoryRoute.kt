package com.dundueni.app.feature.history

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dundueni.app.BuildConfig
import com.dundueni.app.data.repository.PreparedApiHttpException
import com.dundueni.app.feature.analysis.AnalysisFlowDependencies
import com.dundueni.app.feature.home.HomeDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HistoryRoute(
    onHome: () -> Unit,
    initialRecordId: String? = null,
    onNavigationSelected: ((HomeDestination) -> Unit)? = null,
) {
    val context = LocalContext.current
    val store = remember(context) { HistoryStore(context) }
    val real = BuildConfig.ANALYSIS_API_MODE.trim().equals("REAL", ignoreCase = true)
    val remote = remember(context, real) {
        if (real) runCatching { AnalysisFlowDependencies.createHistoryRepository(context) }.getOrNull() else null
    }
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<HistoryRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var requestedPage by remember { mutableIntStateOf(1) }
    var pageAttempt by remember { mutableIntStateOf(0) }
    var nextPage by remember { mutableStateOf<Int?>(null) }
    var loadingMore by remember { mutableStateOf(false) }
    var loadMoreError by remember { mutableStateOf<String?>(null) }
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var deletedRecordId by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        requestedPage = 1
        revision++
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(revision, requestedPage, pageAttempt) {
        val page = requestedPage
        loading = page == 1
        loadingMore = page > 1
        loadMoreError = null
        if (page == 1) nextPage = null
        try {
            val (items, hasNext) = withContext(Dispatchers.IO) {
                if (real) {
                    val result = checkNotNull(remote).list(page = page)
                    result.items.map { it.toHistoryRecord() } to result.hasNext
                } else {
                    store.read() to false
                }
            }
            records = if (page == 1) items else (records + items).distinctBy(HistoryRecord::id)
            nextPage = if (hasNext) page + 1 else null
            error = null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            val message = failure.historyMessage("기록을 불러오지 못했어요. 다시 시도해주세요.")
            if (page == 1 || (failure as? PreparedApiHttpException)?.httpStatus == 401) {
                records = emptyList()
                nextPage = null
                error = message
            } else {
                loadMoreError = message
            }
        } finally {
            loading = false
            loadingMore = false
        }
    }

    HistoryScreen(
        initialRecordId = initialRecordId, onCloseDetail = if (initialRecordId != null) onHome else null,
        onNavigationSelected = onNavigationSelected,
        records = records, loading = loading, error = error, onHome = onHome,
        onRetry = ::refresh,
        onDelete = { pendingDelete = it },
        onClear = { pendingDelete = "*" },
        serverBacked = real,
        deletedRecordId = deletedRecordId,
        loadingMore = loadingMore,
        loadMoreError = loadMoreError,
        onLoadMore = nextPage?.let { next ->
            { if (!loadingMore && !deleting) { requestedPage = next; pageAttempt++ } }
        },
        detailContent = if (real) {
            { id ->
                ServerHistoryDetail(revision, onDelete = { pendingDelete = id }) {
                    checkNotNull(remote).detail(id).toRecordDetail()
                }
            }
        } else null,
    )

    pendingDelete?.let { target ->
        fun dismiss() {
            pendingDelete = null
            deleteError = null
        }
        AlertDialog(
            onDismissRequest = { if (!deleting) dismiss() },
            title = { Text(if (target == "*") "전체 기록을 지울까요?" else "이 검사 기록을 지울까요?") },
            text = { Text(deleteError ?: "삭제한 기록은 복구할 수 없어요.") },
            confirmButton = {
                TextButton(enabled = !deleting, onClick = {
                    deleting = true
                    deleteError = null
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                if (real) {
                                    val repository = checkNotNull(remote)
                                    if (target == "*") repository.deleteAll() else repository.deleteOne(target)
                                } else {
                                    check(if (target == "*") store.clear() else store.delete(target))
                                }
                            }
                            records = if (target == "*") emptyList() else records.filterNot { it.id == target }
                            if (target != "*") deletedRecordId = target
                            dismiss()
                            refresh()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            deleteError = failure.historyMessage("기록을 삭제하지 못했어요. 다시 시도해주세요.")
                            if ((failure as? PreparedApiHttpException)?.httpStatus == 401) refresh()
                        } finally {
                            deleting = false
                        }
                    }
                }) { Text(if (deleting) "삭제 중…" else "삭제") }
            },
            dismissButton = { TextButton(enabled = !deleting, onClick = ::dismiss) { Text("취소") } },
        )
    }
}

@Composable
private fun ServerHistoryDetail(
    revision: Int,
    onDelete: () -> Unit,
    load: suspend () -> HistoryRecordDetail,
) {
    var attempt by remember { mutableIntStateOf(0) }
    var response by remember(revision, attempt) { mutableStateOf<Result<HistoryRecordDetail>?>(null) }
    LaunchedEffect(revision, attempt) {
        response = try {
            Result.success(withContext(Dispatchers.IO) { load() })
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
    HistoryResultDetail(
        detail = response?.getOrNull(),
        error = response?.exceptionOrNull()?.historyMessage("기록 상세를 불러오지 못했어요."),
        onRetry = { attempt++ },
        onDelete = onDelete,
    )
}

private fun Throwable.historyMessage(fallback: String): String {
    val error = this as? PreparedApiHttpException ?: return fallback
    return when {
        error.httpStatus == 401 || error.apiCode == "AUTH401" -> "검사 기록을 확인하려면 다시 로그인해주세요."
        error.httpStatus == 400 && error.apiCode == "COMMON400" -> "검사 기록 요청이 올바르지 않아요."
        error.httpStatus == 404 && error.apiCode == "COMMON404" -> "검사 기록이 없거나 더 이상 볼 수 없어요."
        error.httpStatus == 500 && error.apiCode == "COMMON500" -> "검사 기록 서버에 오류가 발생했어요. 다시 시도해주세요."
        else -> fallback
    }
}
