package com.dundueni.app.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.dundueni.app.ui.theme.LocalEasyMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dundueni.app.BuildConfig
import com.dundueni.app.feature.floatingbutton.FloatingButtonPreferences
import com.dundueni.app.feature.floatingbutton.FloatingButtonManager
import com.dundueni.app.feature.history.HistoryStore
import com.dundueni.app.feature.home.HomeDestination
import com.dundueni.app.feature.home.HomeBottomNavigation
import com.dundueni.app.ui.theme.BrandInk
import com.dundueni.app.ui.theme.PageBackground
import com.dundueni.app.ui.theme.ProtectiveBlue
import com.dundueni.app.ui.theme.SecondaryInk
import com.dundueni.app.ui.theme.SoftBlue
import com.dundueni.app.ui.component.DundueniButton
import com.dundueni.app.ui.component.DundueniButtonType
import com.dundueni.app.ui.component.dundueniOutlinedTextFieldColors
import org.json.JSONArray
import org.json.JSONObject

private val Bg = PageBackground
private val Blue = ProtectiveBlue
private val Ink = BrandInk
private val Muted = SecondaryInk
private val Pale = SoftBlue

@Composable
fun SettingsScreen(onNavigationSelected: (HomeDestination) -> Unit) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        FloatingButtonManager.suspendForActivity("settings")
        onDispose { FloatingButtonManager.resumeForActivity("settings") }
    }
    val prefs = remember { context.getSharedPreferences(FloatingButtonPreferences.PREFERENCES_NAME, Context.MODE_PRIVATE) }
    val initialButtonSize = remember { prefs.getString(FloatingButtonPreferences.KEY_SIZE, "보통") ?: "보통" }
    val initialEdgeSnap = remember { prefs.getBoolean("edge_snap", true) }
    val initialOpacity = remember { prefs.getFloat(FloatingButtonPreferences.KEY_OPACITY, .85f) }
    val initialFontSize = remember {
        prefs.getString(FloatingButtonPreferences.KEY_FONT_SIZE, null) ?: when {
            prefs.getFloat(FloatingButtonPreferences.KEY_FONT_SCALE, 1f) < .93f -> "작게"
            prefs.getFloat(FloatingButtonPreferences.KEY_FONT_SCALE, 1f) > 1.08f -> "크게"
            else -> "보통"
        }
    }
    val initialHaptics = remember { prefs.getBoolean(FloatingButtonPreferences.KEY_HAPTICS, true) }
    val initialLocalBackup = remember { prefs.getBoolean("local_backup", true) }
    val initialRetention = remember { prefs.getString("retention", "90일 보관") ?: "90일 보관" }
    var buttonSize by remember { mutableStateOf(initialButtonSize) }
    var appliedButtonSize by remember { mutableStateOf(initialButtonSize) }
    var edgeSnap by remember { mutableStateOf(initialEdgeSnap) }
    var appliedEdgeSnap by remember { mutableStateOf(initialEdgeSnap) }
    var opacity by remember { mutableFloatStateOf(initialOpacity) }
    var appliedOpacity by remember { mutableFloatStateOf(initialOpacity) }
    var fontSize by remember { mutableStateOf(initialFontSize) }
    var appliedFontSize by remember { mutableStateOf(initialFontSize) }
    var haptics by remember { mutableStateOf(initialHaptics) }
    var appliedHaptics by remember { mutableStateOf(initialHaptics) }
    var localBackup by remember { mutableStateOf(initialLocalBackup) }
    var appliedLocalBackup by remember { mutableStateOf(initialLocalBackup) }
    var retention by remember { mutableStateOf(initialRetention) }
    var appliedRetention by remember { mutableStateOf(initialRetention) }
    var showClear by remember { mutableStateOf(false) }
    var showRetention by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var family by remember {
        mutableStateOf(runCatching {
            val array = JSONArray(prefs.getString("family_contacts", "[]"))
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let { it.optString("name") to it.optString("phone") }
            }.filter { it.first.isNotBlank() && it.second.isNotBlank() }
        }.getOrDefault(listOf("딸내미 (안심 보호자)" to "010-8291-****")))
    }
    var appliedFamily by remember { mutableStateOf(family) }
    var familyDialog by remember { mutableStateOf(false) }
    var editingFamily by remember { mutableIntStateOf(-1) }
    var familyName by remember { mutableStateOf("") }
    var familyPhone by remember { mutableStateOf("") }
    BackHandler { onNavigationSelected(HomeDestination.HOME) }

    val hasPendingChanges = buttonSize != appliedButtonSize ||
        edgeSnap != appliedEdgeSnap ||
        opacity != appliedOpacity ||
        fontSize != appliedFontSize ||
        haptics != appliedHaptics ||
        localBackup != appliedLocalBackup ||
        retention != appliedRetention ||
        family != appliedFamily

    fun applySettings() {
        val editor = prefs.edit()
            .putString(FloatingButtonPreferences.KEY_SIZE, buttonSize)
            .putBoolean("edge_snap", edgeSnap)
            .putFloat(FloatingButtonPreferences.KEY_OPACITY, opacity)
            .putString(FloatingButtonPreferences.KEY_FONT_SIZE, fontSize)
            .putBoolean(FloatingButtonPreferences.KEY_HAPTICS, haptics)
            .putBoolean("local_backup", localBackup)
            .putString("retention", retention)
        if (family != appliedFamily) {
            val familyJson = JSONArray().apply {
                family.forEach { (name, phone) -> put(JSONObject().put("name", name).put("phone", phone)) }
            }.toString()
            editor.putString("family_contacts", familyJson)
        }
        editor.apply()
        appliedButtonSize = buttonSize
        appliedEdgeSnap = edgeSnap
        appliedOpacity = opacity
        appliedFontSize = fontSize
        appliedHaptics = haptics
        appliedLocalBackup = localBackup
        appliedRetention = retention
        appliedFamily = family
        onNavigationSelected(HomeDestination.HOME)
    }
    val easy = LocalEasyMode.current
    val density = LocalDensity.current
    Scaffold(
        containerColor = Bg,
        bottomBar = { HomeBottomNavigation(HomeDestination.SETTINGS, onNavigationSelected) },
    ) { padding ->
        CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * if (easy) 1.2f else 1f)) {
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().height(58.dp).background(Color.White).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (easy) "쉬운설정" else "설정", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onNavigationSelected(HomeDestination.HOME) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("홈 화면으로 돌아가기", color = Blue, fontSize = 14.sp)
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                SettingsSection("플로팅 버튼 설정") {
                    Column(Modifier.background(Color.White, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) { Text("크기 조절", Modifier.weight(1f), color = Ink, fontWeight = FontWeight.SemiBold); Text("$buttonSize (기본 권장)", color = Blue, fontSize = 12.sp) }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Pale).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("작게", "보통", "크게").forEach { size ->
                                    Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(7.dp)).background(if (buttonSize == size) Color.White else Color.Transparent).clickable { buttonSize = size }, contentAlignment = Alignment.Center) { Text(size, color = Ink, fontSize = 13.sp) }
                                }
                            }
                        }
                        if (easy) {
                            Row(
                                Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("화면 가장자리 자석 고정", Modifier.weight(1f), color = Ink, fontWeight = FontWeight.SemiBold)
                                Switch(edgeSnap, { edgeSnap = it }, Modifier.widthIn(min = 52.dp).heightIn(min = 48.dp),
                                    colors = SwitchDefaults.colors(checkedTrackColor = Blue))
                            }
                        } else Box(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text("화면 가장자리 자석 고정", Modifier.fillMaxWidth(), color = Ink, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                            Switch(edgeSnap, { edgeSnap = it }, Modifier.align(Alignment.CenterEnd), colors = SwitchDefaults.colors(checkedTrackColor = Blue))
                        }
                        Column {
                            Row { Text("버튼 대기 투명도", Modifier.weight(1f), color = Ink, fontWeight = FontWeight.SemiBold); Text("${(opacity * 100).toInt()}%", color = Blue, fontWeight = FontWeight.Bold) }
                            Slider(opacity, { opacity = it }, valueRange = .25f..1f, colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue))
                        }
                    }
                }
                SettingsSection("접근성 및 편의") {
                    Column(Modifier.background(Color.White, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Column {
                            Text("글자 크기", color = Ink, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Pale).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("작게", "보통", "크게").forEach { size ->
                                    Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(7.dp)).background(if (fontSize == size) Color.White else Color.Transparent).clickable { fontSize = size }, contentAlignment = Alignment.Center) { Text(size, color = Ink, fontSize = 13.sp) }
                                }
                            }
                        }
                        SettingToggle("촉각 햅틱 경고 진동", "버튼 조작과 주의·위험 검사 결과에 진동을 적용합니다", haptics) { haptics = it }
                    }
                }
                SettingsSection("개인정보 및 데이터 보안") {
                    Column(Modifier.background(Color.White, RoundedCornerShape(16.dp))) {
                        if (easy) {
                            Column(
                                Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("검사 기록 기기 내 안전 보관", Modifier.weight(1f), color = Ink,
                                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Switch(localBackup, { localBackup = it }, Modifier.widthIn(min = 52.dp).heightIn(min = 48.dp),
                                        colors = SwitchDefaults.colors(checkedTrackColor = Blue))
                                }
                                Text("온디바이스", Modifier.clip(RoundedCornerShape(5.dp)).background(Pale)
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                                    color = Blue, fontSize = 12.sp, maxLines = 1, softWrap = false)
                                Text("기록은 기기에 저장됩니다 · 보관 정책 토글 연동 준비 중",
                                    color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        } else SettingToggle("검사 기록 기기 내 안전 보관", "기록은 기기에 저장됩니다 · 보관 정책 토글 연동 준비 중", localBackup, badge = "온디바이스") { localBackup = it }
                        SettingAction("분석 캡처 이미지 즉시 영구 파기 원칙", "분석 종료 시 임시 캡처 파일 정리", "✓") { dialog = "캡처 이미지 파기 정책" to "화면 캡처는 분석을 위해 앱 캐시에 임시 파일로 전달되며, 분석 화면이 종료될 때 삭제됩니다. 검사 기록에는 캡처 이미지가 저장되지 않습니다." }
                        SettingAction("검사 기록 보관 주기", "보관주기 선택 UI · 자동 삭제 연동 준비 중", retention) { showRetention = true }
                        Row(Modifier.fillMaxWidth().background(Color(0xFFFFF8F7)).clickable { showClear = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text("전체 데이터 및 검사 이력 비우기", color = Color(0xFFCA2424), fontWeight = FontWeight.Bold); Text("저장된 모든 기록을 영구 삭제합니다", color = Color(0xFFB66A6A), fontSize = 11.sp) }
                            Button(onClick = { showClear = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBE1E24)), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp)) { Text("초기화") }
                        }
                    }
                }
                SettingsSection("보호자 및 비상 신고망", trailing = { Text("+ 가족 추가", Modifier.clickable { editingFamily = -1; familyName = ""; familyPhone = ""; familyDialog = true }, color = Blue, fontSize = 12.sp) }) {
                    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        family.forEachIndexed { index, person ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Pale).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("●", color = Blue); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(person.first, color = Ink, fontWeight = FontWeight.SemiBold); Text(person.second, color = Muted, fontSize = 12.sp) }
                    Text("☎", Modifier.clickable {
                        val number = person.second.replace("-", "").replace("*", "")
                        if (number.length >= 10) dial(context, number)
                        else dialog = "보호자 전화번호" to "보호자 상세 정보에서 실제 전화번호를 입력해 주세요."
                    }.padding(8.dp), color = Blue, fontSize = 20.sp)
                                Text("⋮", Modifier.clickable { editingFamily = index; familyName = person.first; familyPhone = person.second; familyDialog = true }.padding(8.dp), color = Muted, fontSize = 22.sp)
                            }
                        }
                        Text("원터치 비상 신고 채널", color = Muted, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EmergencyCard("경찰청 112", "보이스피싱 긴급신고", Modifier.weight(1f)) { dial(context, "112") }
                            EmergencyCard("KISA 118", "스미싱·해킹 상담", Modifier.weight(1f)) { dial(context, "118") }
                        }
                    }
                }
                SettingsSection("ⓘ  앱 정보 및 고객지원") {
                    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp)) {
                        SettingAction("현재 버전", "든든이 안전 엔진 v${BuildConfig.VERSION_NAME}", "v${BuildConfig.VERSION_NAME}") { dialog = "현재 버전" to "든든이 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})" }
                        SettingAction("오픈소스 라이선스", "", "›") { dialog = "오픈소스 라이선스" to "이 앱은 AndroidX 및 Jetpack Compose 등 오픈소스 소프트웨어를 사용합니다. 라이선스 상세 목록 안내 화면은 준비 중입니다." }
                        SettingAction("서비스 이용약관 및 개인정보 처리방침", "", "›") { dialog = "이용약관 및 개인정보 처리방침" to "서비스 이용약관과 개인정보 처리방침 전문은 준비 중입니다." }
                        SettingAction("고객센터 및 오탐지 제보", "", "›") { dialog = "고객센터 및 오탐지 제보" to "고객센터 연결 기능은 준비 중입니다." }
                    }
                }
                Text("© 든든이 · 개인정보보호 규정을 준수합니다", Modifier.fillMaxWidth(), color = Muted, fontSize = 11.sp)
                if (hasPendingChanges) {
                    Text("변경한 설정은 적용 버튼을 눌러야 저장됩니다.", color = Muted, fontSize = 12.sp)
                }
                DundueniButton("적용", ::applySettings, Modifier.fillMaxWidth(), DundueniButtonType.SECONDARY, hasPendingChanges)
            }
        }
    }
    }
    if (showClear) AlertDialog(
        onDismissRequest = { showClear = false },
        title = { Text("전체 데이터 삭제") },
        text = { Text("저장된 검사 이력을 모두 삭제할까요? 삭제한 기록은 복구할 수 없습니다.") },
        confirmButton = {
            TextButton(onClick = {
                HistoryStore(context).clear()
                showClear = false
                dialog = "삭제 완료" to "기기 내 검사 기록을 삭제했습니다."
            }) { Text("삭제", color = Color.Red) }
        },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("취소") } },
    )
    if (showRetention) AlertDialog(
        onDismissRequest = { showRetention = false },
        title = { Text("검사 기록 보관 주기") },
        text = {
            Column {
                listOf("30일 보관", "90일 보관", "1년 보관", "직접 삭제").forEach { value ->
                    Row(Modifier.fillMaxWidth().clickable {
                        retention = value
                        showRetention = false
                    }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(retention == value, { retention = value; showRetention = false })
                        Text(value)
                    }
                }
            }
        },
        confirmButton = {},
    )
    dialog?.let { (title, message) -> AlertDialog(
        onDismissRequest = { dialog = null }, title = { Text(title) }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { dialog = null }) { Text("확인") } },
    ) }
    if (familyDialog) AlertDialog(
        onDismissRequest = { familyDialog = false },
        title = { Text(if (editingFamily < 0) "보호자 등록" else "보호자 정보") },
        text = {
            Column {
                OutlinedTextField(familyName, { familyName = it }, label = { Text("이름 및 관계") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = dundueniOutlinedTextFieldColors())
                OutlinedTextField(familyPhone, { familyPhone = it }, label = { Text("전화번호") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = dundueniOutlinedTextFieldColors())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (familyName.isNotBlank() && familyPhone.isNotBlank()) {
                    val updated = family.toMutableList()
                    val row = familyName to familyPhone
                    if (editingFamily >= 0) updated[editingFamily] = row else updated.add(row)
                    family = updated
                }
                familyDialog = false
            }) { Text("완료") }
        },
        dismissButton = {
            if (editingFamily >= 0) TextButton(onClick = {
                val updated = family.filterIndexed { i, _ -> i != editingFamily }
                family = updated
                familyDialog = false
            }) { Text("삭제", color = Color.Red) }
            else TextButton(onClick = { familyDialog = false }) { Text("취소") }
        },
    )
}

@Composable private fun SettingsSection(title: String, trailing: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold); trailing?.invoke() }; content() }
}

@Composable private fun SettingToggle(title: String, subtitle: String, checked: Boolean, badge: String? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); if (badge != null) Text(badge, Modifier.padding(start = 8.dp).clip(RoundedCornerShape(5.dp)).background(Pale).padding(horizontal = 7.dp, vertical = 3.dp), color = Blue, fontSize = 10.sp) }; Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 16.sp) }
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Blue))
    }
}

@Composable private fun SettingAction(title: String, subtitle: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); if (subtitle.isNotBlank()) Text(subtitle, color = Muted, fontSize = 11.sp) }; Text(value, color = Blue, fontSize = 12.sp) }
}

@Composable private fun EmergencyCard(title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).background(Pale).clickable(onClick = onClick).padding(10.dp)) { Text(title, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 11.sp) }
}

private fun dial(context: Context, number: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))) }
}
