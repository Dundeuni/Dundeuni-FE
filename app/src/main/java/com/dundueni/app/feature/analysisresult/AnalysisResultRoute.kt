package com.dundueni.app.feature.analysisresult

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun AnalysisResultRoute(result: AnalysisResult, onReturn: () -> Unit, isDemo: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember(context) { context.getSharedPreferences("analysis_result_history", Context.MODE_PRIVATE) }
    var saved by rememberSaveable(result.id) { mutableStateOf(hasSavedResult(preferences.getString("results", "[]"), result.id)) }
    var saving by remember { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }

    fun open(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "이 동작을 실행할 앱이 없어요.", Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(context, "이 동작을 실행할 수 없어요.", Toast.LENGTH_SHORT).show()
        }
    }

    fun share() {
        val content = buildString {
            if (isDemo) append("[디자인 미리보기 · 샘플 데이터]\n")
            append("든든이 분석 결과: ${result.riskLevel.label}\n${result.summary}\n")
            result.evidence.forEach { append("• ${it.title}\n") }
            append("\n분석 결과는 참고 정보입니다. 의심스러운 요청은 공식 연락처로 다시 확인해주세요.")
        }
        open(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }, "분석 결과 공유"))
    }

    AnalysisResultScreen(result, historySaved = saved, isDemo = isDemo, onAction = { action ->
        when (action) {
            ResultAction.RETURN -> onReturn()
            ResultAction.PROFILE -> Toast.makeText(context, "계정 메뉴를 사용할 수 없어요.", Toast.LENGTH_SHORT).show()
            ResultAction.CALL_FAMILY -> open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:")))
            ResultAction.SHARE, ResultAction.ASK_FAMILY -> share()
            ResultAction.REPORT -> dialog = "report"
            ResultAction.PREVIEW_DOMAIN -> dialog = "domain"
            ResultAction.SAVE_HISTORY -> {
                if (isDemo) {
                    Toast.makeText(context, "샘플 결과는 실제 검사 기록에 보관하지 않아요.", Toast.LENGTH_SHORT).show()
                } else if (!saved && !saving) {
                    saving = true
                    scope.launch {
                        val success = withContext(Dispatchers.IO) {
                            runCatching {
                                val previous = JSONArray(preferences.getString("results", "[]"))
                                val updated = JSONArray()
                                updated.put(JSONObject().apply {
                                    put("id", result.id)
                                    put("riskLevel", result.riskLevel.name)
                                    put("savedAt", System.currentTimeMillis())
                                    result.fraudScore?.let { put("fraudScore", it) }
                                    result.aiScore?.let { put("aiScore", it) }
                                })
                                for (index in 0 until previous.length()) {
                                    val item = previous.getJSONObject(index)
                                    if (item.optString("id") != result.id && updated.length() < 50) updated.put(item)
                                }
                                // No original message, URL, evidence, or personally identifying text is persisted.
                                preferences.edit().putString("results", updated.toString()).commit()
                            }.getOrDefault(false)
                        }
                        saved = success
                        saving = false
                        Toast.makeText(context, if (success) "검사 결과를 보관했어요." else "보관하지 못했어요. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    })

    if (dialog == "report") {
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("신고 연락처 선택") },
            text = { Text("전화 앱에서 번호를 확인한 뒤 직접 통화할 수 있어요.") },
            confirmButton = { TextButton(onClick = { dialog = null; open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) }) { Text("112 전화 앱 열기") } },
            dismissButton = { TextButton(onClick = { dialog = null; open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:118"))) }) { Text("118 전화 앱 열기") } },
        )
    }
    if (dialog == "domain") {
        val host = remember(result.sourceUrl) { domainForPreview(result.sourceUrl) }
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("도메인만 미리보기") },
            text = { Text(if (host == null) "올바른 웹 주소를 확인할 수 없어요." else "$host\n\n링크에 접속하지 않고 주소의 도메인만 표시했어요. 단축 링크의 최종 목적지나 사이트의 안전성을 확인한 결과는 아니에요.") },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("확인") } },
        )
    }
}

private fun hasSavedResult(json: String?, id: String): Boolean = runCatching {
    val results = JSONArray(json ?: "[]")
    (0 until results.length()).any { results.getJSONObject(it).optString("id") == id }
}.getOrDefault(false)

