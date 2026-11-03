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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dundueni.app.BuildConfig
import com.dundueni.app.feature.history.HistoryStore
import com.dundueni.app.feature.home.HomeDestination
import com.dundueni.app.feature.home.HomeBottomNavigation
import org.json.JSONArray
import org.json.JSONObject

private val Bg = Color(0xFFF7F8FC)
private val Blue = Color(0xFF0758D5)
private val Ink = Color(0xFF171B24)
private val Muted = Color(0xFF777E8B)
private val Pale = Color(0xFFEAF0FF)

@Composable
fun SettingsScreen(onNavigationSelected: (HomeDestination) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dundueni_settings", Context.MODE_PRIVATE) }
    var buttonSize by remember { mutableStateOf(prefs.getString("button_size", "보통") ?: "보통") }
    var edgeSnap by remember { mutableStateOf(prefs.getBoolean("edge_snap", true)) }
    var opacity by remember { mutableFloatStateOf(prefs.getFloat("button_opacity", .85f)) }
    var fontScale by remember { mutableFloatStateOf(prefs.getFloat("font_scale", 1f)) }
    var haptics by remember { mutableStateOf(prefs.getBoolean("haptics", true)) }
    var localBackup by remember { mutableStateOf(prefs.getBoolean("local_backup", true)) }
    var retention by remember { mutableStateOf(prefs.getString("retention", "90일 보관") ?: "90일 보관") }
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
    var familyDialog by remember { mutableStateOf(false) }
    var editingFamily by remember { mutableIntStateOf(-1) }
    var familyName by remember { mutableStateOf("") }
    var familyPhone by remember { mutableStateOf("") }
    BackHandler { onNavigationSelected(HomeDestination.HOME) }

    fun save(key: String, value: Any) {
        prefs.edit().apply {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is String -> putString(key, value)
            }
        }.apply()
    }
    Scaffold(
        containerColor = Bg,
        bottomBar = { HomeBottomNavigation(HomeDestination.SETTINGS, onNavigationSelected) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().height(58.dp).background(Color.White).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("설정", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onNavigationSelected(HomeDestination.HOME) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("홈 화면으로 돌아가기", color = Blue, fontSize = 14.sp)
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                SettingsSection("플로팅 버튼 설정") {
                    Column(Modifier.background(Color.White, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) { Text("버튼 크기 조절", Modifier.weight(1f), color = Ink, fontWeight = FontWeight.SemiBold); Text("$buttonSize (기본 권장)", color = Blue, fontSize = 12.sp) }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Pale).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("작게", "보통", "크게").forEach { size ->
                                    Box(Modifier.weight(1f).clip(RoundedCornerShape(7.dp)).background(if (buttonSize == size) Color.White else Color.Transparent).clickable { buttonSize = size; save("button_size", size) }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) { Text(size, color = Ink, fontSize = 13.sp) }
                                }
                            }
                        }
                        SettingToggle("화면 가장자리 자석 고정", "손가락을 떼면 가장 가까운 화면 모서리에\n　착 달라붙어요", edgeSnap) { edgeSnap = it; save("edge_snap", it) }
                        Column {
                            Row { Text("버튼 대기 투명도", Modifier.weight(1f), color = Ink, fontWeight = FontWeight.SemiBold); Text("${(opacity * 100).toInt()}%", color = Blue, fontWeight = FontWeight.Bold) }
                            Slider(opacity, { opacity = it; save("button_opacity", it) }, valueRange = .25f..1f, colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue))
                        }
                    }
                }
                SettingsSection("접근성 및 편의") {
                    Column(Modifier.background(Color.White, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Column {
                            Row { Text("글자 크기 미세 조정", Modifier.weight(1f), color = Ink, fontWeight = FontWeight.SemiBold); Text("기본 표준 (${(fontScale * 100).toInt()}%)", color = Muted, fontSize = 12.sp) }
                            Slider(fontScale, { fontScale = it; save("font_scale", it) }, valueRange = .85f..1.2f, colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue))
                        }
                        SettingToggle("촉각 햅틱 경고 진동", "옵션 저장 UI · 검사 경고 진동 연동 준비 중", haptics) { haptics = it; save("haptics", it) }
                    }
                }
                SettingsSection("개인정보 및 데이터 보안") {
                    Column(Modifier.background(Color.White, RoundedCornerShape(16.dp))) {
                        SettingToggle("검사 기록 기기 내 안전 보관", "기록은 기기에 저장됩니다 · 보관 정책 토글 연동 준비 중", localBackup, badge = "온디바이스") { localBackup = it; save("local_backup", it) }
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
                        save("retention", value)
                        showRetention = false
                    }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(retention == value, { retention = value; save("retention", value); showRetention = false })
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
                OutlinedTextField(familyName, { familyName = it }, label = { Text("이름 및 관계") }, singleLine = true)
                OutlinedTextField(familyPhone, { familyPhone = it }, label = { Text("전화번호") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (familyName.isNotBlank() && familyPhone.isNotBlank()) {
                    val updated = family.toMutableList()
                    val row = familyName to familyPhone
                    if (editingFamily >= 0) updated[editingFamily] = row else updated.add(row)
                    family = updated
                    prefs.edit().putString("family_contacts", JSONArray().apply {
                        updated.forEach { (name, phone) -> put(JSONObject().put("name", name).put("phone", phone)) }
                    }.toString()).apply()
                }
                familyDialog = false
            }) { Text("저장") }
        },
        dismissButton = {
            if (editingFamily >= 0) TextButton(onClick = {
                val updated = family.filterIndexed { i, _ -> i != editingFamily }
                family = updated
                prefs.edit().putString("family_contacts", JSONArray().apply {
                    updated.forEach { (name, phone) -> put(JSONObject().put("name", name).put("phone", phone)) }
                }.toString()).apply()
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
    Column(modifier.clip(RoundedCornerShape(9.dp)).background(Pale).clickable(onClick = onClick).padding(10.dp)) { Text(title, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 9.sp) }
}

private fun dial(context: Context, number: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))) }
}
