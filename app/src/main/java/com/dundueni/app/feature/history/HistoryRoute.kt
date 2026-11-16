package com.dundueni.app.feature.history

import com.dundueni.app.BuildConfig
import com.dundueni.app.feature.analysis.AnalysisFlowDependencies
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.dundueni.app.feature.home.HomeDestination
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
    val remote = remember(context, real) { if (real) runCatching { AnalysisFlowDependencies.createHistoryRepository(context) }.getOrNull() else null }
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<HistoryRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) revision++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(revision) {
        loading = true
        val loaded = withContext(Dispatchers.IO) { runCatching {
            if (real) checkNotNull(remote).list().map { it.toHistoryRecord() }
            else store.read()
        } }
        loaded.onSuccess { records = it; error = null }
            .onFailure { error = "기록을 불러오지 못했어요. 다시 시도해주세요." }
        loading = false
    }
    LaunchedEffect(initialRecordId, revision, loading) {
        if (real && !loading && initialRecordId != null) {
            withContext(Dispatchers.IO) { runCatching { checkNotNull(remote).detail(initialRecordId).toHistoryRecord().let { record ->
                if (record.type == HistoryType.IMAGE && record.status == "COMPLETED")
                    runCatching { record.withImageDetail(AnalysisFlowDependencies.createRepository(context).findResult(initialRecordId)) }.getOrDefault(record)
                else record
            } } }
                .onSuccess { detail -> records = listOf(detail) + records.filterNot { it.id == detail.id } }
                .onFailure { error = "기록 상세를 불러오지 못했어요. 다시 시도해주세요." }
        }
    }
    HistoryScreen(
        initialRecordId = initialRecordId, onCloseDetail = if (initialRecordId != null) onHome else null,
        onNavigationSelected = onNavigationSelected,
        records = records, loading = loading, error = error, onHome = onHome,
        onRetry = { revision++ }, onDelete = { pendingDelete = it }, onClear = { pendingDelete = "*" },
        allowDelete = !real, onSelect = { id ->
            if (real) scope.launch {
                val detail = withContext(Dispatchers.IO) { runCatching { checkNotNull(remote).detail(id).toHistoryRecord().let { record ->
                    if (record.type == HistoryType.IMAGE && record.status == "COMPLETED")
                        runCatching { record.withImageDetail(AnalysisFlowDependencies.createRepository(context).findResult(id)) }.getOrDefault(record)
                    else record
                } } }
                detail.onSuccess { record -> records = records.map { if (it.id == id) record else it }; error = null }
                    .onFailure { error = "기록 상세를 불러오지 못했어요. 다시 시도해주세요." }
            }
        },
    )
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!deleting) pendingDelete = null },
            title = { Text(if (target == "*") "전체 기록을 지울까요?" else "이 검사 기록을 지울까요?") },
            text = { Text("삭제한 기록은 복구할 수 없어요.") },
            confirmButton = {
                TextButton(enabled = !deleting, onClick = {
                    deleting = true
                    scope.launch {
                        val success = withContext(Dispatchers.IO) {
                            runCatching { if (target == "*") store.clear() else store.delete(target) }.getOrDefault(false)
                        }
                        deleting = false
                        pendingDelete = null
                        if (success) revision++ else error = "기록을 삭제하지 못했어요. 다시 시도해주세요."
                    }
                }) { Text(if (deleting) "삭제 중…" else "삭제") }
            },
            dismissButton = { TextButton(enabled = !deleting, onClick = { pendingDelete = null }) { Text("취소") } },
        )
    }
}
