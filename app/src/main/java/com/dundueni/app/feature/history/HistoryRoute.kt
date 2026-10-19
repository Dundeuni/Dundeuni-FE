package com.dundueni.app.feature.history

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HistoryRoute(onHome: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { HistoryStore(context) }
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<HistoryRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    LaunchedEffect(revision) {
        loading = true
        val loaded = withContext(Dispatchers.IO) { runCatching { store.read() } }
        loaded.onSuccess { records = it; error = null }
            .onFailure { error = "기록을 불러오지 못했어요. 다시 시도해주세요." }
        loading = false
    }
    HistoryScreen(
        records = records, loading = loading, error = error, onHome = onHome,
        onRetry = { revision++ }, onDelete = { pendingDelete = it }, onClear = { pendingDelete = "*" },
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
