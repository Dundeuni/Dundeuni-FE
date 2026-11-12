package com.dundueni.app.feature.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dundueni.app.R
import com.dundueni.app.feature.analysisresult.AiLikelihood
import com.dundueni.app.feature.analysisresult.ResultGlyph
import com.dundueni.app.feature.analysisresult.ResultIcon
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.feature.analysisresult.formatScore
import com.dundueni.app.feature.home.HomeBottomNavigation
import com.dundueni.app.feature.home.HomeDestination
import com.dundueni.app.ui.theme.BrandInk
import com.dundueni.app.ui.theme.CautionInk
import com.dundueni.app.ui.theme.Caution
import com.dundueni.app.ui.theme.Danger
import com.dundueni.app.ui.theme.PageBackground
import com.dundueni.app.ui.theme.ProtectiveBlue
import com.dundueni.app.ui.theme.Safe
import com.dundueni.app.ui.theme.SafeInk
import com.dundueni.app.ui.theme.SecondaryInk
import com.dundueni.app.ui.theme.SoftCaution
import com.dundueni.app.ui.theme.SoftDanger
import com.dundueni.app.ui.theme.SoftSafe
import com.dundueni.app.ui.theme.SoftBlue
import com.dundueni.app.ui.component.dundueniOutlinedTextFieldColors
import com.dundueni.app.ui.theme.LocalEasyMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val Background = PageBackground
private val Ink = BrandInk
private val Muted = SecondaryInk
private val Blue = ProtectiveBlue
private val PaleBlue = SoftBlue
private val Red = Danger
private val Purple = Color(0xFF7661F4)
private val Green = Safe
private val Outline = Color(0xFFEBEEF6)

/** Production records and debug samples use the same UI, but never share their storage. */
@Composable
fun HistoryScreen(
    records: List<HistoryRecord>,
    onHome: () -> Unit,
    onDelete: (String) -> Unit,
    onClear: () -> Unit,
    onSelect: (String) -> Unit = {},
    allowDelete: Boolean = true,
    loading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    isDemo: Boolean = false,
    initialRecordId: String? = null,
    onCloseDetail: (() -> Unit)? = null,
    onNavigationSelected: ((HomeDestination) -> Unit)? = null,
    initialEasyMode: Boolean = LocalEasyMode.current,
) {
    val easy = initialEasyMode
    var filterName by rememberSaveable { mutableStateOf("ALL") }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf(initialRecordId) }
    val closeDetail: () -> Unit = { if (onCloseDetail != null) onCloseDetail() else selectedId = null }
    var panel by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    fun navigate(destination: HomeDestination) {
        when {
            destination == HomeDestination.REPORT -> {
                selectedId = null
                onNavigationSelected?.invoke(destination)
            }
            onNavigationSelected != null -> onNavigationSelected(destination)
            destination == HomeDestination.HOME -> onHome()
            destination == HomeDestination.SETTINGS -> panel = "settings"
            destination == HomeDestination.MY_SHELTER -> panel = "profile"
            else -> panel = "notifications"
        }
    }
    val selected = records.firstOrNull { it.id == selectedId }
    val displayRecords = remember(records) { records.filterNot { it.type == HistoryType.LINK } }
    val searchableRecords = remember(displayRecords, query.text) { filterHistory(displayRecords, null, query.text) }
    val activeFilter = HistoryType.entries.firstOrNull { it.name == filterName && it != HistoryType.LINK }
    val visible = remember(searchableRecords, activeFilter) {
        searchableRecords.filter { activeFilter == null || it.type == activeFilter }
    }
    LaunchedEffect(records, loading) {
        if (!loading && selectedId != null && selected == null && initialRecordId == null) selectedId = null
    }
    BackHandler {
        when {
            selectedId != null -> closeDetail()
            searching -> { searching = false; query = TextFieldValue() }
            else -> onHome()
        }
    }
    Scaffold(
        containerColor = Background,
        topBar = {
            Surface(color = if (easy) Color.White else Background, border = BorderStroke(1.dp, Outline)) {
                Row(Modifier.statusBarsPadding().fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (selected != null) {
                        IconButton(onClick = closeDetail, modifier = Modifier.semantics { contentDescription = if (onCloseDetail != null) "홈으로 돌아가기" else "검사 기록 목록으로 돌아가기" }) {
                            ResultGlyph(ResultIcon.BACK, Ink, 21.dp)
                        }
                    } else {
                        Image(
                            painter = painterResource(R.drawable.dundeuni_mascot_search),
                            contentDescription = "검사 기록 든든이",
                            modifier = Modifier.padding(start = 8.dp).size(40.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (selected != null) "검사 기록 상세" else if (easy) "검사 기록" else "든든이",
                        modifier = if (easy) Modifier.weight(1f) else Modifier, color = Ink, fontSize = 18.sp,
                        fontWeight = if (easy) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!easy) {
                        if (selected == null) Text("History", Modifier.padding(start = 12.dp), color = Muted, fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                    }
                    IconButton(onClick = { navigate(HomeDestination.MY_SHELTER) }, modifier = Modifier.semantics { contentDescription = "내 기록 정보" }) {
                        Box(Modifier.size(if (easy) 28.dp else 32.dp).background(Color.Black, CircleShape), contentAlignment = Alignment.Center) {
                            ResultGlyph(ResultIcon.PROFILE, Color.White, 16.dp)
                        }
                    }
                }
            }
        },
        bottomBar = { HomeBottomNavigation(HomeDestination.REPORT, ::navigate) },
    ) { padding ->
        Box(Modifier.padding(padding).imePadding().fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (selected != null) {
                HistoryDetail(selected, easy, error, onRetry, onBack = closeDetail,
                    onDelete = { onDelete(selected.id) }, allowDelete = allowDelete)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.testTag("history-list").widthIn(max = 600.dp).fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!easy) item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("검사 기록", color = Ink, fontSize = if (easy) 26.sp else 24.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(8.dp))
                            Surface(color = Color(0xFFE9EFFF), shape = CircleShape) {
                                Text("${displayRecords.size}건", Modifier.padding(horizontal = 7.dp, vertical = 2.dp), color = Blue, fontSize = 11.sp)
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { searching = !searching; if (!searching) query = TextFieldValue() },
                                modifier = Modifier.semantics { contentDescription = "검사 기록 검색" }) { ResultGlyph(ResultIcon.SEARCH, Ink, 19.dp) }
                            if (allowDelete) IconButton(onClick = onClear, enabled = records.isNotEmpty() && !loading,
                                modifier = Modifier.semantics { contentDescription = "전체 검사 기록 삭제" }) {
                                HistoryGlyph(HistoryIcon.TRASH, if (records.isNotEmpty() && !loading) Ink else Muted, 19.dp)
                            }
                        }
                    }
                    if (isDemo) item { Notice("디자인 미리보기 · 샘플 기록", easy = easy) }
                    item { EmptyNotice(easy) }
                    if (searching) item { HistorySearchField(query, onValueChange = { query = it }) }
                    item { HistoryFilters(filterName, searchableRecords, easy) { filterName = it } }
                    when {
                        loading -> item {
                            Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                CircularProgressIndicator(color = Blue)
                                Text("검사 기록을 불러오고 있어요", color = Muted)
                            }
                        }
                        error != null -> item { EmptyPanel("기록을 확인하지 못했어요", error, "다시 시도", onRetry, easy) }
                        displayRecords.isEmpty() -> item { EmptyPanel("아직 검사 기록이 없어요", "검사를 진행하고 결과를 보관하면\n이곳에서 다시 확인할 수 있어요.", "홈으로 돌아가기", onHome, easy) }
                        visible.isEmpty() -> item { EmptyPanel("조건에 맞는 기록이 없어요", "다른 검사 유형을 선택하거나\n검색어를 바꿔보세요.", "전체 기록 보기", { query = TextFieldValue(); filterName = "ALL" }, easy) }
                        else -> {
                            val today = LocalDate.now()
                            visible.groupBy { it.date() }.forEach { (date, group) ->
                                item(key = "date-$date") {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (easy) {
                                                Box(Modifier.size(7.dp).background(Blue, CircleShape))
                                                Spacer(Modifier.width(7.dp))
                                            }
                                            Text(when (date) {
                                                today -> if (easy) "오늘 검사한 내용" else "오늘"
                                                today.minusDays(1) -> if (easy) "어제 검사한 내용" else "어제"
                                                else -> date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) + if (easy) " 검사한 내용" else ""
                                            }, Modifier.weight(1f), fontSize = if (easy) 16.sp else 13.sp, color = Ink, fontWeight = FontWeight.Medium)
                                            Surface(color = if (easy) Color(0xFFE8EDFA) else Color.Transparent, shape = CircleShape) {
                                                Text("${group.size}건", Modifier.padding(horizontal = if (easy) 8.dp else 0.dp, vertical = if (easy) 3.dp else 0.dp),
                                                    color = if (easy) Ink else Muted, fontSize = 11.sp)
                                            }
                                        }
                                        if (easy) {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                group.forEach { record -> key(record.id) { HistoryCard(record, true) { selectedId = record.id; onSelect(record.id) } } }
                                            }
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                group.forEach { record ->
                                                    key(record.id) { HistoryCard(record, false) { selectedId = record.id; onSelect(record.id) } }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (allowDelete && !loading && error == null && records.isNotEmpty()) item {
                        Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (easy) {
                                Button(onClick = onClear, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDDE4F3), contentColor = Ink)) {
                                    HistoryGlyph(HistoryIcon.TRASH, Color(0xFFD52D36), 18.dp)
                                    Text("전체 기록 모두 지우기", Modifier.padding(start = 8.dp), fontSize = 14.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("삭제한 기록은 복구할 수 없어요.", color = Muted, fontSize = 12.sp)
                            } else {
                                TextButton(onClick = onClear) {
                                    HistoryGlyph(HistoryIcon.TRASH, Red, 15.dp)
                                    Text("전체 기록 지우기", Modifier.padding(start = 6.dp), color = Red, fontSize = 12.sp)
                                }
                                Text("최근 50건까지 이 기기에 보관돼요", color = Muted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
    panel?.let { current ->
        AlertDialog(
            onDismissRequest = { panel = null },
            containerColor = Color.White,
            title = { Text(when (current) { "settings" -> "검사 기록 설정"; "profile" -> "내 기록 정보"; else -> "알림 설정" }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(when (current) {
                        "settings" -> if (allowDelete) "이 기기에 저장된 기록을 관리할 수 있어요. 화면 모드는 홈에서 바꿀 수 있어요." else "서버에 저장된 검사 기록을 확인할 수 있어요."
                        "profile" -> if (allowDelete) "현재 기록은 이 기기에 보관돼요. 계정 간 동기화와 회원 로그인은 연결되어 있지 않아요." else "현재 계정의 검사 기록을 서버에서 조회해요."
                        else -> "검사 기록 화면에는 별도의 알림 내역이 없어요. 실제 앱에서는 기기의 앱 알림 설정을 열 수 있어요."
                    }, fontSize = 14.sp, lineHeight = 22.sp)
                    if (allowDelete && current == "settings") {

                        TextButton(onClick = { panel = null; onClear() }, enabled = records.isNotEmpty() && !loading) { Text("전체 기록 지우기", color = Red) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { panel = null }) { Text("닫기") } },
        )
    }
}

@Composable
private fun HistoryFilters(selected: String, records: List<HistoryRecord>, easy: Boolean, onSelect: (String) -> Unit) {
    val types = listOf(HistoryType.SCREEN, HistoryType.TEXT, HistoryType.IMAGE, HistoryType.PHONE, HistoryType.UNKNOWN)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = selected == "ALL" || selected == "LINK" || selected == "TEXT_LINK",
            onClick = { onSelect("ALL") }, label = { Text("전체 ${records.size}", fontSize = if (easy) 16.sp else 12.sp) },
            modifier = Modifier.heightIn(min = if (easy) 52.dp else 48.dp), shape = CircleShape,
            colors = FilterChipDefaults.filterChipColors(containerColor = Color.White, labelColor = Ink,
                selectedContainerColor = Blue, selectedLabelColor = Color.White))
        types.forEach { type ->
            val count = records.count { it.type == type }
            if (type != HistoryType.UNKNOWN || count > 0) {
                val label = when (type) {
                    HistoryType.SCREEN -> "화면 검사"
                    HistoryType.TEXT -> "문자 검사"
                    HistoryType.IMAGE -> "사진 검사"
                    HistoryType.PHONE -> "전화번호 검사"
                    HistoryType.UNKNOWN -> "기타 검사"
                    HistoryType.LINK -> ""
                }
                FilterChip(selected = selected == type.name, onClick = { onSelect(type.name) },
                    label = { Text("$label $count", fontSize = if (easy) 16.sp else 12.sp) },
                    modifier = Modifier.heightIn(min = if (easy) 52.dp else 48.dp), shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(containerColor = Color.White, labelColor = Ink,
                        selectedContainerColor = Blue, selectedLabelColor = Color.White))
            }
        }
    }
}

@Composable
private fun EmptyNotice(easy: Boolean) {
    Surface(color = if (easy) Color.White else PaleBlue, shape = RoundedCornerShape(if (easy) 16.dp else 14.dp),
        modifier = Modifier.fillMaxWidth().height(if (easy) 112.dp else 72.dp)) {}
}

@Composable
private fun CardRiskBadge(level: RiskLevel, easy: Boolean) {
    val tint = riskColor(level)
    val background = when (level) {
        RiskLevel.DANGER -> SoftDanger
        RiskLevel.CAUTION -> SoftCaution
        RiskLevel.SAFE -> SoftSafe
        RiskLevel.UNKNOWN -> Color(0xFFEDF0FA)
    }
    Surface(color = background, shape = CircleShape) {
        Text(level.label, Modifier.padding(horizontal = if (easy) 12.dp else 8.dp, vertical = if (easy) 7.dp else 4.dp),
            color = tint, fontSize = if (easy) 16.sp else 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun cardBorder(level: RiskLevel): Color = when (level) {
    RiskLevel.DANGER -> Red
    RiskLevel.CAUTION -> Caution
    RiskLevel.SAFE -> Green
    RiskLevel.UNKNOWN -> Outline
}

private fun recordColor(record: HistoryRecord): Color = when {
    record.type == HistoryType.IMAGE -> Purple
    record.riskLevel == RiskLevel.DANGER -> Red
    record.riskLevel == RiskLevel.CAUTION -> CautionInk
    record.riskLevel == RiskLevel.SAFE -> SafeInk
    else -> Blue
}

@Composable
private fun EasyHistoryCard(record: HistoryRecord, onClick: () -> Unit) {
    val danger = record.riskLevel == RiskLevel.DANGER
    val tint = riskColor(record.riskLevel)
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = Color.White,
        border = BorderStroke(1.5.dp, cardBorder(record.riskLevel)),
        modifier = Modifier.fillMaxWidth().testTag("easy-record-${record.id}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(timeLabel(record.savedAt), Modifier.weight(1f), color = Muted, fontSize = 16.sp)
                CardRiskBadge(record.riskLevel, easy = true)
            }
            Text(record.title, Modifier.fillMaxWidth(), color = Ink, fontSize = 20.sp, lineHeight = 28.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(record.preview ?: "${record.riskLevel.label} 판정 · 저장한 분석 지표 확인", color = Ink,
                        fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(when (record.riskLevel) {
                        RiskLevel.DANGER -> "위험 판정 기록이에요. 송금 전 다시 확인하세요."
                        RiskLevel.CAUTION -> "출처가 불분명하면 누르지 않는 것이 안전해요."
                        RiskLevel.SAFE -> "안전 판정 기록이에요. 낯선 요청은 한 번 더 확인하세요."
                        RiskLevel.UNKNOWN -> "사기 위험 판정 정보가 없어요. 상세 내용을 확인하세요."
                    }, color = if (danger) tint else if (record.riskLevel == RiskLevel.SAFE) Blue else Muted,
                        fontSize = 16.sp, lineHeight = 24.sp)
                }
                Box(Modifier.size(36.dp).background(Color(0xFFE8EDFA), CircleShape), contentAlignment = Alignment.Center) {
                    Text("›", color = Ink, fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(record: HistoryRecord, easy: Boolean, onClick: () -> Unit) {
    if (easy) {
        EasyHistoryCard(record, onClick)
        return
    }
    Surface(onClick = onClick, color = Color.White, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, cardBorder(record.riskLevel)),
        modifier = Modifier.fillMaxWidth().testTag("record-${record.id}")) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RecordTypeIcon(record, 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(buildAnnotatedString {
                    append(record.title)
                    withStyle(SpanStyle(color = Muted, fontSize = 10.sp)) { append(" · ${timeLabel(record.savedAt)}") }
                }, color = Ink, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(record.preview ?: "${record.riskLevel.label} 판정 · 저장한 분석 지표 확인", color = Ink, fontSize = 11.sp,
                    lineHeight = 17.sp, maxLines = if (record.type == HistoryType.IMAGE) 2 else 1, overflow = TextOverflow.Ellipsis)
            }
            CardRiskBadge(record.riskLevel, easy = false)
            Text("›", color = Muted, fontSize = 22.sp)
        }
    }
}
@Composable
private fun RecordTypeIcon(record: HistoryRecord, size: Dp) {
    val tint = recordColor(record)
    Box(Modifier.size(size).background(tint.copy(alpha = .10f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        when (record.type) {
            HistoryType.SCREEN -> HistoryGlyph(HistoryIcon.SCAN, tint, 20.dp)
            HistoryType.IMAGE -> HistoryGlyph(HistoryIcon.IMAGE, tint, 20.dp)
            HistoryType.LINK -> ResultGlyph(ResultIcon.LINK, tint, 20.dp)
            HistoryType.TEXT -> ResultGlyph(ResultIcon.MESSAGE, tint, 20.dp)
            HistoryType.PHONE -> ResultGlyph(ResultIcon.PHONE, tint, 20.dp)
            HistoryType.UNKNOWN -> ResultGlyph(ResultIcon.SHIELD, tint, 20.dp)
        }
    }
}

@Composable
private fun HistorySearchField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit) {
    val requester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    LaunchedEffect(Unit) { requester.requestFocus(); keyboard?.show() }
    OutlinedTextField(value = value, onValueChange = onValueChange,
        label = { Text("검사 유형 또는 위험 단계 검색", fontSize = 13.sp) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); focus.clearFocus() }),
        trailingIcon = { if (value.text.isNotEmpty()) IconButton(onClick = { onValueChange(TextFieldValue()) },
            modifier = Modifier.semantics { contentDescription = "검색어 지우기" }) { ResultGlyph(ResultIcon.CLOSE, Muted, 16.dp) } },
        modifier = Modifier.fillMaxWidth().testTag("history-search").focusRequester(requester),
        shape = RoundedCornerShape(12.dp), colors = dundueniOutlinedTextFieldColors())
}

@Composable
private fun HistoryDetail(record: HistoryRecord, easy: Boolean, error: String?, onRetry: () -> Unit, onBack: () -> Unit, onDelete: () -> Unit, allowDelete: Boolean) {
    LazyColumn(Modifier.testTag("history-detail").widthIn(max = 600.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (error != null) item { EmptyPanel("기록을 확인하지 못했어요", error, "다시 시도", onRetry, easy) }
        item {
            Panel {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RecordTypeIcon(record, 48.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(record.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text(Instant.ofEpochMilli(record.savedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")), fontSize = 12.sp, color = Muted)
                    }
                    RiskBadge(record)
                }
                HorizontalDivider(color = Outline)
                Text("최종 위험 단계: ${record.riskLevel.label}", color = riskColor(record.riskLevel), fontSize = 13.sp)
                Text("저장된 검사 내용", color = Muted, fontSize = 12.sp)
                Text(record.preview ?: "이 기록에는 위험 단계와 제공된 분석 점수가 보관되어 있어요.", color = Ink, fontSize = if (easy) 17.sp else 14.sp, lineHeight = 23.sp)
            }
        }
        item {
            Panel {
                Text("분석 지표", fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 16.sp)
                ScoreRow("사기 위험도", record.fraudScore, riskColor(record.riskLevel))
                ScoreRow("AI 생성·변조", record.aiScore, Purple)
                if (record.aiLikelihood != AiLikelihood.UNKNOWN) Text("AI 분석 단계: ${record.aiLikelihood.label}", color = Muted, fontSize = 12.sp)
                Text("두 지표는 서로 독립적이며, AI 점수가 사기 위험 판정을 의미하지는 않아요.", color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
            }
        }
        item {
            Panel {
                Text("분석 근거", fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 16.sp)
                if (record.evidence.isEmpty()) {
                    Text(if (allowDelete) "이 기록에는 세부 근거가 저장되어 있지 않아요. 원문·링크·근거 내용은 기기에 보관하지 않습니다." else "서버에서 제공한 세부 근거가 없어요.", color = Muted, fontSize = if (easy) 16.sp else 13.sp, lineHeight = 22.sp)
                } else record.evidence.forEach { reason ->
                    Text(reason, color = Muted, fontSize = if (easy) 16.sp else 13.sp, lineHeight = 22.sp)
                }
            }
        }
        item { Notice(when (record.riskLevel) {
            RiskLevel.SAFE -> "안전 판정이어도 개인정보나 인증번호를 요구하는 요청은 공식 연락처로 다시 확인하세요."
            RiskLevel.CAUTION -> "의심스러운 링크를 열기 전에 공식 창구나 믿을 수 있는 사람에게 확인하세요."
            RiskLevel.DANGER -> "송금이나 개인정보 입력을 멈추고 공식 연락처로 사실 여부를 확인하세요."
            RiskLevel.UNKNOWN -> "사기 위험 판정이 없는 기록이에요. 분석 정보와 공식 연락처를 함께 확인하세요."
        }, easy) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Ink)) { Text("목록으로 돌아가기") }
                if (allowDelete) OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = CircleShape,
                    border = BorderStroke(1.dp, Red.copy(alpha = .25f))) {
                    HistoryGlyph(HistoryIcon.TRASH, Red, 16.dp)
                    Text("이 기록 삭제하기", Modifier.padding(start = 6.dp), color = Red)
                }
            }
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: Double?, tint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Muted, fontSize = 13.sp)
            Text(score?.let { "${formatScore(it)}%" } ?: "정보 없음", color = if (score == null) Muted else tint, fontWeight = FontWeight.SemiBold)
        }
        if (score != null) Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(Outline)) {
            Box(Modifier.fillMaxWidth((score / 100).toFloat().coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(tint))
        }
    }
}

@Composable
private fun RiskBadge(record: HistoryRecord) {
    val tint = if (record.statusLabel == "AI 생성 의심") Purple else riskColor(record.riskLevel)
    Surface(color = tint.copy(alpha = .09f), shape = CircleShape) {
        Text(record.statusLabel, Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = tint, fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
    }
}

private fun riskColor(level: RiskLevel): Color = when (level) { RiskLevel.SAFE -> SafeInk; RiskLevel.CAUTION -> CautionInk; RiskLevel.DANGER -> Red; RiskLevel.UNKNOWN -> Muted }
private fun timeLabel(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))

@Composable
private fun Notice(message: String, easy: Boolean = false) {
    Surface(color = PaleBlue, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HistoryGlyph(HistoryIcon.LOCK, Blue, 17.dp)
            Text(message, Modifier.weight(1f), color = Ink, fontSize = if (easy) 16.sp else 12.sp, lineHeight = if (easy) 25.sp else 20.sp)
        }
    }
}

@Composable
private fun EmptyPanel(title: String, message: String, action: String, onAction: () -> Unit, easy: Boolean) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = if (easy) 72.dp else 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(if (easy) 20.dp else 16.dp)) {
        Box(Modifier.size(if (easy) 88.dp else 72.dp).background(PaleBlue, CircleShape), contentAlignment = Alignment.Center) {
            HistoryGlyph(HistoryIcon.HISTORY, Blue, if (easy) 38.dp else 32.dp)
        }
        Text(title, color = Ink, fontSize = if (easy) 22.sp else 19.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(message, color = Muted, fontSize = if (easy) 16.sp else 13.sp, lineHeight = if (easy) 25.sp else 22.sp, textAlign = TextAlign.Center)
        Button(onClick = onAction, modifier = (if (easy) Modifier.fillMaxWidth() else Modifier.widthIn(min = 192.dp)).heightIn(min = if (easy) 56.dp else 48.dp),
            shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
            Text(action, fontSize = if (easy) 16.sp else 14.sp)
        }
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Outline)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

private enum class HistoryIcon { HOME, SCAN, HISTORY, SETTINGS, BELL, TRASH, LOCK, IMAGE, PROTECT }

/** History-specific vectors stay in this feature rather than changing PB-05's result icons. */
@Composable
private fun HistoryGlyph(icon: HistoryIcon, tint: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        scale(this.size.width / 24f, this.size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(1.65f, cap = StrokeCap.Round)
            fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(tint, Offset(x1, y1), Offset(x2, y2), 1.65f, StrokeCap.Round)
            when (icon) {
                HistoryIcon.PROTECT -> {
                    drawPath(Path().apply {
                        moveTo(12f, 2f); lineTo(21f, 6f); lineTo(20f, 14f)
                        quadraticTo(18f, 20f, 12f, 23f); quadraticTo(6f, 20f, 4f, 14f); lineTo(3f, 6f); close()
                    }, tint, style = stroke)
                    line(7f, 12f, 10.5f, 15.5f); line(10.5f, 15.5f, 17f, 8.5f)
                }
                HistoryIcon.HOME -> {
                    drawPath(Path().apply { moveTo(3f, 10f); lineTo(12f, 3f); lineTo(21f, 10f); lineTo(21f, 21f); lineTo(14f, 21f); lineTo(14f, 14f); lineTo(10f, 14f); lineTo(10f, 21f); lineTo(3f, 21f); close() }, tint, style = stroke)
                }
                HistoryIcon.SCAN -> {
                    drawRoundRect(tint, Offset(3f, 4f), Size(18f, 15f), androidx.compose.ui.geometry.CornerRadius(3f), style = stroke)
                    line(8f, 22f, 16f, 22f); line(12f, 19f, 12f, 22f)
                    line(8f, 12f, 11f, 15f); line(11f, 15f, 17f, 9f)
                }
                HistoryIcon.HISTORY -> {
                    drawRoundRect(tint, Offset(4f, 3f), Size(16f, 19f), androidx.compose.ui.geometry.CornerRadius(2f), style = stroke)
                    line(8f, 2f, 8f, 6f); line(16f, 2f, 16f, 6f); line(8f, 11f, 16f, 11f); line(8f, 15f, 16f, 15f); line(8f, 19f, 13f, 19f)
                }
                HistoryIcon.SETTINGS -> {
                    drawCircle(tint, 7f, Offset(12f, 12f), style = stroke)
                    drawCircle(tint, 2.5f, Offset(12f, 12f), style = stroke)
                    for (step in 0..7) {
                        val angle = step * Math.PI / 4
                        line((12 + 7 * kotlin.math.cos(angle)).toFloat(), (12 + 7 * kotlin.math.sin(angle)).toFloat(),
                            (12 + 10 * kotlin.math.cos(angle)).toFloat(), (12 + 10 * kotlin.math.sin(angle)).toFloat())
                    }
                }
                HistoryIcon.BELL -> {
                    drawPath(Path().apply { moveTo(4f, 18f); lineTo(6f, 15f); lineTo(6f, 10f); cubicTo(6f, 2f, 18f, 2f, 18f, 10f); lineTo(18f, 15f); lineTo(20f, 18f); close() }, tint, style = stroke)
                    line(10f, 22f, 14f, 22f); line(12f, 2f, 12f, 4f)
                }
                HistoryIcon.TRASH -> {
                    line(3f, 6f, 21f, 6f); line(9f, 3f, 15f, 3f)
                    drawPath(Path().apply { moveTo(6f, 6f); lineTo(7f, 22f); lineTo(17f, 22f); lineTo(18f, 6f) }, tint, style = stroke)
                    line(10f, 10f, 10f, 18f); line(14f, 10f, 14f, 18f)
                }
                HistoryIcon.LOCK -> {
                    drawRoundRect(tint, Offset(5f, 10f), Size(14f, 12f), androidx.compose.ui.geometry.CornerRadius(2f), style = stroke)
                    drawPath(Path().apply { moveTo(8f, 10f); lineTo(8f, 6f); cubicTo(8f, 1f, 16f, 1f, 16f, 6f); lineTo(16f, 10f) }, tint, style = stroke)
                    line(12f, 15f, 12f, 18f)
                }
                HistoryIcon.IMAGE -> {
                    drawRoundRect(tint, Offset(3f, 4f), Size(18f, 17f), androidx.compose.ui.geometry.CornerRadius(3f), style = stroke)
                    drawCircle(tint, 1.5f, Offset(8f, 9f))
                    drawPath(Path().apply { moveTo(4f, 18f); lineTo(10f, 12f); lineTo(14f, 16f); lineTo(17f, 13f); lineTo(21f, 17f) }, tint, style = stroke)
                }
            }
        }
    }
}
