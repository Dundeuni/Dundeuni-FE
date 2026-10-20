package com.dundueni.app.feature.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dundueni.app.feature.analysisresult.ResultGlyph
import com.dundueni.app.feature.analysisresult.ResultIcon
import com.dundueni.app.feature.analysisresult.RiskLevel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val Background = Color(0xFFF8F9FF)
private val Ink = Color(0xFF192232)
private val Muted = Color(0xFF697386)
private val Blue = Color(0xFF0866FF)
private val PaleBlue = Color(0xFFEEF3FF)
private val Red = Color(0xFFD52D36)

/** Shared by the production route and debug previews. Samples never enter the real store. */
@Composable
fun HistoryScreen(
    records: List<HistoryRecord>,
    onHome: () -> Unit,
    onDelete: (String) -> Unit,
    onClear: () -> Unit,
    loading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    isDemo: Boolean = false,
) {
    var easy by rememberSaveable { mutableStateOf(false) }
    var filterName by rememberSaveable { mutableStateOf("ALL") }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = records.firstOrNull { it.id == selectedId }
    val filter = HistoryType.entries.firstOrNull { it.name == filterName }
    val visible = remember(records, filter, query.text) { filterHistory(records, filter, query.text) }
    LaunchedEffect(records, loading) {
        if (!loading && selectedId != null && selected == null) selectedId = null
    }
    BackHandler { if (selectedId != null) selectedId = null else onHome() }

    Scaffold(
        containerColor = Background,
        topBar = {
            Surface(color = Background) {
                Row(Modifier.statusBarsPadding().fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (selected != null) {
                        IconButton(onClick = { selectedId = null }, modifier = Modifier.semantics { contentDescription = "검사 기록 목록으로 돌아가기" }) {
                            ResultGlyph(ResultIcon.BACK, Ink)
                        }
                    } else {
                        Box(Modifier.size(32.dp).background(if (easy) Blue else Ink, CircleShape), contentAlignment = Alignment.Center) {
                            ResultGlyph(ResultIcon.SHIELD, Color.White, 18.dp)
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(if (selected != null) "검사 기록 상세" else if (easy) "쉬운 검사 기록" else "든든이", Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold, fontSize = if (easy) 20.sp else 18.sp)
                    if (selected == null) TextButton(onClick = { easy = !easy }) { Text(if (easy) "기본 모드" else "쉬운 모드", fontSize = 12.sp) }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(selected = false, onClick = onHome, icon = { ResultGlyph(ResultIcon.SHIELD, Muted) }, label = { Text("홈") })
                NavigationBarItem(selected = true, onClick = { selectedId = null }, icon = { ResultGlyph(ResultIcon.CLOCK, Blue) }, label = { Text("검사 기록", color = Blue) })
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).imePadding().fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (selected != null) {
                HistoryDetail(selected, easy, onDelete = { onDelete(selected.id) })
            } else {
                LazyColumn(
                    modifier = Modifier.testTag("history-list").widthIn(max = 600.dp).fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (isDemo) item { Notice("디자인 미리보기 · 샘플 기록", ResultIcon.TIP) }
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("검사 기록", color = Ink, fontSize = if (easy) 26.sp else 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Surface(color = PaleBlue, shape = CircleShape) { Text("${records.size}건", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Blue, fontSize = 12.sp) }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { searching = !searching; if (!searching) query = TextFieldValue() }, modifier = Modifier.semantics { contentDescription = "검사 기록 검색" }) { ResultGlyph(ResultIcon.SEARCH, Ink) }
                        }
                    }
                    item { Notice("검사 기록은 이 기기에 보관돼요. 원문과 링크는 저장하지 않으며, 언제든 삭제할 수 있어요.", ResultIcon.SAVE, easy) }
                    if (searching) item {
                        HistorySearchField(query, onValueChange = { query = it })
                    }
                    item {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HistoryFilter("전체 ${records.size}", filterName == "ALL") { filterName = "ALL" }
                            HistoryType.entries.forEach { type ->
                                val count = records.count { it.type == type }
                                if (type != HistoryType.UNKNOWN || count > 0) HistoryFilter("${type.label} $count", filter == type) { filterName = type.name }
                            }
                        }
                    }
                    when {
                        loading -> item {
                            Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                CircularProgressIndicator(color = Blue)
                                Text("검사 기록을 불러오고 있어요", color = Muted)
                            }
                        }
                        error != null -> item { EmptyPanel("기록을 확인하지 못했어요", error, "다시 시도", onRetry, easy) }
                        records.isEmpty() -> item { EmptyPanel("아직 검사 기록이 없어요", "검사를 진행하고 결과를 보관하면\n이곳에서 다시 확인할 수 있어요.", "홈으로 돌아가기", onHome, easy) }
                        visible.isEmpty() -> item { EmptyPanel("조건에 맞는 기록이 없어요", "다른 검사 유형을 선택하거나\n검색어를 바꿔보세요.", "전체 기록 보기", { query = TextFieldValue(); filterName = "ALL" }, easy) }
                        else -> {
                            val today = LocalDate.now()
                            visible.groupBy { it.date() }.forEach { (date, group) ->
                                item(key = "date-$date") {
                                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(when (date) { today -> "오늘"; today.minusDays(1) -> "어제"; else -> date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) }, Modifier.weight(1f), fontSize = if (easy) 16.sp else 14.sp, color = Ink, fontWeight = FontWeight.Medium)
                                        Text("${group.size}건", color = Muted, fontSize = 12.sp)
                                    }
                                }
                                items(group, key = { "record-${it.id}" }) { record -> HistoryCard(record, easy) { selectedId = record.id } }
                            }
                        }
                    }
                    if (!loading && error == null && records.isNotEmpty()) item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            TextButton(onClick = onClear) { Text("전체 기록 모두 지우기", color = Red, fontSize = if (easy) 16.sp else 14.sp) }
                            Text("삭제한 기록은 복구할 수 없어요 · 최근 50건 보관", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryFilter(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, shape = CircleShape,
        border = null, colors = FilterChipDefaults.filterChipColors(containerColor = PaleBlue, labelColor = Ink, selectedContainerColor = Ink, selectedLabelColor = Color.White))
}

@Composable
private fun HistoryCard(record: HistoryRecord, easy: Boolean, onClick: () -> Unit) {
    if (easy) {
        EasyHistoryCard(record, onClick)
        return
    }
    val tint = riskColor(record.riskLevel)
    Surface(onClick = onClick, color = Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(if (easy) 18.dp else 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(if (easy) 46.dp else 40.dp).background(tint.copy(alpha = .09f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                ResultGlyph(when(record.type) { HistoryType.IMAGE -> ResultIcon.AI; HistoryType.LINK -> ResultIcon.LINK; HistoryType.TEXT -> ResultIcon.MESSAGE; HistoryType.SCREEN -> ResultIcon.SEARCH; HistoryType.PHONE -> ResultIcon.PHONE; HistoryType.UNKNOWN -> ResultIcon.SHIELD }, tint)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (easy) RiskBadge(record.riskLevel)
                Text("${record.title} · ${timeLabel(record.savedAt)}", color = Ink, fontSize = if (easy) 16.sp else 14.sp, fontWeight = FontWeight.Medium)
                Text(record.preview ?: "${record.riskLevel.label} 판정 · 저장한 분석 지표 확인", color = Muted, fontSize = if (easy) 14.sp else 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (!easy) RiskBadge(record.riskLevel)
            Text("›", color = Muted, fontSize = 24.sp)
        }
    }
}

@Composable
private fun HistorySearchField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit) {
    val requester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    LaunchedEffect(Unit) { requester.requestFocus(); keyboard?.show() }
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text("검사 유형 또는 위험 단계 검색") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); focus.clearFocus() }),
        modifier = Modifier.fillMaxWidth().testTag("history-search").focusRequester(requester),
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
private fun EasyHistoryCard(record: HistoryRecord, onClick: () -> Unit) {
    val tint = riskColor(record.riskLevel)
    Surface(onClick = onClick, color = Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().testTag("easy-record-${record.id}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = tint.copy(alpha = .1f), shape = CircleShape) {
                    Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ResultGlyph(when (record.riskLevel) {
                            RiskLevel.DANGER, RiskLevel.CAUTION -> ResultIcon.WARNING
                            RiskLevel.SAFE -> ResultIcon.CHECK
                            RiskLevel.UNKNOWN -> ResultIcon.SEARCH
                        }, tint, 12.dp)
                        Text(when(record.riskLevel) { RiskLevel.DANGER -> "위험 발견"; RiskLevel.CAUTION -> "주의 경고"; RiskLevel.SAFE -> "안전 확인"; RiskLevel.UNKNOWN -> "판정 미확정" }, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(record.title, Modifier.weight(1f), color = Ink, fontSize = 13.sp, maxLines = 2, fontWeight = FontWeight.Medium)
                Text(timeLabel(record.savedAt), color = Muted, fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(record.preview ?: "${record.riskLevel.label} · 저장한 분석 지표 확인", color = Ink, fontSize = 15.sp, lineHeight = 23.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(when(record.riskLevel) {
                        RiskLevel.DANGER -> "위험 판정 기록이에요. 송금 전 다시 확인하세요."
                        RiskLevel.CAUTION -> "의심스러운 요청은 공식 창구로 확인하세요."
                        RiskLevel.SAFE -> "안전 판정 기록이에요. 상세 내용을 확인하세요."
                        RiskLevel.UNKNOWN -> "최종 위험 판정이 없는 기록이에요. 상세 내용을 확인하세요."
                    }, color = if (record.riskLevel == RiskLevel.DANGER) Red else Blue, fontSize = 12.sp, lineHeight = 19.sp)
                }
                Box(Modifier.size(36.dp).background(PaleBlue, CircleShape), contentAlignment = Alignment.Center) { Text("›", color = Ink, fontSize = 24.sp) }
            }
        }
    }
}

@Composable
private fun HistoryDetail(record: HistoryRecord, easy: Boolean, onDelete: () -> Unit) {
    LazyColumn(Modifier.testTag("history-detail").widthIn(max = 600.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Panel {
                RiskBadge(record.riskLevel)
                Text(record.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(Instant.ofEpochMilli(record.savedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")), color = Muted)
                Text(record.preview ?: "이 기록에는 위험 단계와 제공된 분석 점수가 보관되어 있어요.", color = Ink, fontSize = if (easy) 17.sp else 15.sp)
            }
        }
        item {
            Panel {
                Text("분석 지표", fontWeight = FontWeight.Bold, color = Ink, fontSize = 18.sp)
                ScoreRow("사기 위험도", record.fraudScore)
                ScoreRow("AI 생성·변조", record.aiScore)
                Text("두 지표는 서로 독립적이며, AI 점수가 사기 위험 판정을 의미하지는 않아요.", color = Muted, fontSize = 13.sp)
            }
        }
        item { Panel {
            Text("분석 근거", fontWeight = FontWeight.Bold, color = Ink, fontSize = 18.sp)
            Text("이 기록에는 세부 근거가 저장되어 있지 않아요. 원문·링크·근거 내용은 기기에 보관하지 않습니다.", color = Muted, fontSize = if (easy) 16.sp else 14.sp)
        } }
        item { Notice(when(record.riskLevel) {
            RiskLevel.SAFE -> "안전 판정이어도 개인정보나 인증번호를 요구하는 요청은 공식 연락처로 다시 확인하세요."
            RiskLevel.CAUTION -> "의심스러운 링크를 열기 전에 공식 창구나 믿을 수 있는 사람에게 확인하세요."
            RiskLevel.DANGER -> "송금이나 개인정보 입력을 멈추고 공식 연락처로 사실 여부를 확인하세요."
            RiskLevel.UNKNOWN -> "최종 위험 판정이 없어요. 의심스러운 요청은 공식 연락처로 확인하세요."
        }, ResultIcon.TIP, easy) }
        item { OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("이 기록 삭제하기", color = Red) } }
    }
}

@Composable
private fun ScoreRow(label: String, score: Double?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted)
        Text(score?.let { "${com.dundueni.app.feature.analysisresult.formatScore(it)}%" } ?: "정보 없음", color = Ink, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RiskBadge(level: RiskLevel) {
    val tint = riskColor(level)
    Surface(color = tint.copy(alpha = .10f), shape = CircleShape) {
        Text(level.label, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun riskColor(level: RiskLevel): Color = when(level) {
    RiskLevel.SAFE -> Color(0xFF23865B)
    RiskLevel.CAUTION -> Color(0xFF315EC3)
    RiskLevel.DANGER -> Red
    RiskLevel.UNKNOWN -> Muted
}

private fun timeLabel(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))

@Composable
private fun Notice(message: String, icon: ResultIcon, easy: Boolean = false) {
    Surface(color = PaleBlue, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ResultGlyph(icon, Blue)
            Text(message, color = Ink, fontSize = if (easy) 16.sp else 14.sp, lineHeight = if (easy) 25.sp else 22.sp)
        }
    }
}

@Composable
private fun EmptyPanel(title: String, message: String, action: String, onAction: () -> Unit, easy: Boolean) {
    Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(64.dp).background(PaleBlue, CircleShape), contentAlignment = Alignment.Center) { ResultGlyph(ResultIcon.CLOCK, Blue, 30.dp) }
            Text(title, color = Ink, fontSize = if (easy) 22.sp else 20.sp, fontWeight = FontWeight.Bold)
            Text(message, color = Muted, fontSize = if (easy) 16.sp else 14.sp, lineHeight = 24.sp)
            Button(onClick = onAction, shape = CircleShape) { Text(action) }
        }
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}
