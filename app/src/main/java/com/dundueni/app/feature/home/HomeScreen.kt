package com.dundueni.app.feature.home

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dundueni.app.R
import com.dundueni.app.ui.theme.DundueniDimens
import org.json.JSONArray

private val HomeBackground = Color(0xFFF7F8FC)
private val HomeBlue = Color(0xFF0758D5)
private val HomePaleBlue = Color(0xFFEAF0FF)
private val HomeInk = Color(0xFF171B24)
private val HomeMuted = Color(0xFF858B96)
private val HomeBorder = Color(0xFFE9ECF2)

@Composable
fun HomeScreen(
    floatingButtonEnabled: Boolean,
    onFloatingButtonChange: (Boolean) -> Unit,
    onScreenScan: () -> Unit,
    onImageScan: () -> Unit,
    onTextScan: () -> Unit,
    onNotifications: () -> Unit,
    onSettings: () -> Unit,
    onViewAllHistory: () -> Unit,
    onNavigationSelected: (HomeDestination) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var recentRecords by remember(context) { mutableStateOf(loadRecentRecords(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) recentRecords = loadRecentRecords(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = HomeBackground,
        bottomBar = { HomeBottomNavigation(onNavigationSelected) },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
        ) {
            HomeHeader(onNotifications, onSettings)
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp).padding(top = 14.dp, bottom = 22.dp),
            ) {
                FloatingHeroCard(floatingButtonEnabled, onFloatingButtonChange)
                Spacer(Modifier.height(26.dp))
                SectionTitle("빠른 검사")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickScanCard("screen", "AI 화면\n검사", Modifier.weight(1f), onScreenScan)
                    QuickScanCard("image", "이미지 검사", Modifier.weight(1f), onImageScan)
                    QuickScanCard("text", "텍스트 검사", Modifier.weight(1f), onTextScan)
                }
                Spacer(Modifier.height(25.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("최근 검사", Modifier.weight(1f))
                    Surface(onClick = onViewAllHistory, color = Color.Transparent) {
                        Text("전체 기록 보기 ›", color = HomeBlue, fontSize = 12.sp, modifier = Modifier.padding(6.dp))
                    }
                }
                Spacer(Modifier.height(9.dp))
                RecentHistoryCard(recentRecords)
                Spacer(Modifier.height(24.dp))
                SafetyTipCard()
            }
        }
    }
}

enum class HomeDestination { HOME, DIAGNOSIS, REPORT, CONSULTATION, MY_SHELTER }

@Composable
private fun HomeHeader(onNotifications: () -> Unit, onSettings: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(66.dp).background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.dundeuni_logo_wordmark),
            contentDescription = "든든이",
            modifier = Modifier.width(93.dp).height(56.dp),
            contentScale = ContentScale.Fit,
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderAction("알림", "notification", onNotifications)
            HeaderAction("설정", "settings", onSettings)
        }
    }
}

@Composable
private fun HeaderAction(label: String, icon: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = CircleShape,
        modifier = Modifier.size(DundueniDimens.MinTouchSize),
    ) {
        Box(contentAlignment = Alignment.Center) { HomeGlyph(icon, HomeInk, Modifier.size(25.dp)) }
    }
}

@Composable
private fun FloatingHeroCard(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.White, Color(0xFFF0F3FF))),
            ).padding(horizontal = 25.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.dundeuni_mascot_wink),
                contentDescription = "윙크하는 든든이 캐릭터",
                modifier = Modifier.size(112.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (enabled) "든든이가 켜져 있어요" else "든든이가 꺼져 있어요",
                color = HomeInk,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                "다른 앱을 사용하다 의심되는 순간,\n화면 옆 든든이 버튼을 눌러보세요.",
                color = Color(0xFF4D515A),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(17.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE7EBF8)).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("◈", color = HomeInk, fontSize = 19.sp)
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("화면 위 플로팅 버튼", color = HomeInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (enabled) "항상 화면 우측에 고정됨" else "필요할 때 화면 위에 표시",
                        color = Color(0xFF687080),
                        fontSize = 11.sp,
                    )
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, color = HomeInk, fontSize = 19.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun QuickScanCard(icon: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(HomePaleBlue),
                contentAlignment = Alignment.Center,
            ) {
                HomeGlyph(icon, HomeInk, Modifier.size(25.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, color = HomeInk, fontSize = 12.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
        }
    }
}

private data class HomeRecentRecord(val id: String, val risk: String, val savedAt: Long, val aiScore: Int?)

private fun loadRecentRecords(context: Context): List<HomeRecentRecord> = runCatching {
    val saved = context.getSharedPreferences("analysis_result_history", Context.MODE_PRIVATE)
        .getString("results", "[]") ?: "[]"
    val json = JSONArray(saved)
    (0 until json.length()).mapNotNull { index ->
        val item = json.optJSONObject(index) ?: return@mapNotNull null
        val id = item.optString("id").takeIf(String::isNotBlank) ?: return@mapNotNull null
        HomeRecentRecord(
            id = id,
            risk = item.optString("riskLevel", ""),
            savedAt = item.optLong("savedAt"),
            aiScore = item.optDouble("aiScore").takeIf { it.isFinite() && it in 0.0..100.0 }?.toInt(),
        )
    }.take(2)
}.getOrDefault(emptyList())

@Composable
private fun RecentHistoryCard(records: List<HomeRecentRecord>) {
    Surface(shape = RoundedCornerShape(17.dp), color = Color.White, shadowElevation = 1.dp) {
        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) {
                Text("아직 저장된 검사 기록이 없어요", color = HomeMuted, fontSize = 13.sp)
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                records.forEachIndexed { index, record ->
                    RecentHistoryRow(record)
                    if (index < records.lastIndex) Spacer(Modifier.fillMaxWidth().height(1.dp).background(HomeBorder))
                }
            }
        }
    }
}

@Composable
private fun RecentHistoryRow(record: HomeRecentRecord) {
    val badge = when (record.risk.uppercase()) {
        "DANGER" -> "위험"
        "CAUTION" -> "주의"
        "SAFE" -> "안전"
        else -> record.aiScore?.let { "AI 생성 $it%" } ?: "분석 완료"
    }
    val badgeColor = when (record.risk.uppercase()) {
        "DANGER" -> Color(0xFFFFE4E2)
        "CAUTION" -> Color(0xFFFFF0D9)
        "SAFE" -> Color(0xFFE2F5EA)
        else -> Color(0xFFE6E4FF)
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(77.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(badgeColor), contentAlignment = Alignment.Center) {
            HomeGlyph(if (record.aiScore != null) "robot" else "warning", if (record.risk == "DANGER") Color(0xFFCB201C) else HomeBlue, Modifier.size(23.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("저장한 검사 결과", color = HomeInk, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(DateUtils.getRelativeTimeSpanString(record.savedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(), color = Color(0xFF737984), fontSize = 11.sp)
        }
        Text(badge, color = badgeTextColor(record.risk), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(CircleShape).background(badgeColor).padding(horizontal = 11.dp, vertical = 6.dp))
    }
}

private fun badgeTextColor(risk: String) = when (risk.uppercase()) {
    "DANGER" -> Color(0xFFBB1717)
    "CAUTION" -> Color(0xFFAA6A00)
    "SAFE" -> Color(0xFF167344)
    else -> Color(0xFF3F35C8)
}

@Composable
private fun SafetyTipCard() {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFFEDF2FF))
            .padding(horizontal = 17.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFDCE5FF)), contentAlignment = Alignment.Center) {
            HomeGlyph("bulb", HomeBlue, Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text("문자나 메신저로 온 알 수 없는 내용은 든든이로 먼저 확인하세요.", color = Color(0xFF525966), fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun HomeBottomNavigation(onNavigationSelected: (HomeDestination) -> Unit) {
    val items = listOf(
        Triple(HomeDestination.HOME, "home", "홈"),
        Triple(HomeDestination.DIAGNOSIS, "diagnosis", "진단"),
        Triple(HomeDestination.REPORT, "report", "리포트"),
        Triple(HomeDestination.CONSULTATION, "chat", "맞춤 상담"),
        Triple(HomeDestination.MY_SHELTER, "profile", "My 쉼터"),
    )
    NavigationBar(containerColor = Color.White, tonalElevation = 3.dp, windowInsets = WindowInsets.navigationBars) {
        items.forEach { (destination, icon, label) ->
            val selected = destination == HomeDestination.DIAGNOSIS
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigationSelected(destination) },
                icon = { HomeGlyph(icon, if (selected) HomeInk else Color(0xFF9BA0A8), Modifier.size(24.dp)) },
                label = { Text(label, fontSize = 10.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = HomeInk,
                    selectedTextColor = HomeInk,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = Color(0xFF9BA0A8),
                    unselectedTextColor = Color(0xFF9BA0A8),
                ),
            )
        }
    }
}

@Composable
private fun HomeGlyph(name: String, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        val stroke = Stroke(width = 2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (name) {
            "home" -> {
                val path = Path().apply { moveTo(3f*sx, 10f*sy); lineTo(12f*sx, 3f*sy); lineTo(21f*sx, 10f*sy); lineTo(19f*sx, 10f*sy); lineTo(19f*sx, 21f*sy); lineTo(5f*sx, 21f*sy); lineTo(5f*sx, 10f*sy); close() }
                drawPath(path, color, style = stroke)
                drawLine(color, p(10f, 21f), p(10f, 15f), strokeWidth = 2f*sx)
                drawLine(color, p(10f, 15f), p(14f, 15f), strokeWidth = 2f*sx)
                drawLine(color, p(14f, 15f), p(14f, 21f), strokeWidth = 2f*sx)
            }
            "diagnosis" -> {
                drawRoundRect(color, p(3f, 4f), Size(13f*sx, 16f*sy), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f*sx), style = stroke)
                drawCircle(color, 4f*sx, p(16f, 16f), style = stroke)
                drawLine(color, p(19f, 19f), p(22f, 22f), strokeWidth = 2f*sx, cap = StrokeCap.Round)
                listOf(7f, 10f, 13f).forEach { drawLine(color, p(6f, it), p(12f, it), strokeWidth = 1.2f*sx) }
            }
            "report" -> {
                drawRoundRect(color, p(5f, 3f), Size(14f*sx, 18f*sy), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f*sx), style = stroke)
                listOf(8f, 12f, 16f).forEach { drawLine(color, p(8f, it), p(15f, it), strokeWidth = 1.6f*sx) }
                drawLine(color, p(16f, 19f), p(21f, 14f), strokeWidth = 2f*sx, cap = StrokeCap.Round)
            }
            "chat" -> {
                drawRoundRect(color, p(2f, 4f), Size(15f*sx, 12f*sy), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f*sx), style = stroke)
                val tail = Path().apply { moveTo(6f*sx,16f*sy); lineTo(5f*sx,20f*sy); lineTo(10f*sx,16f*sy) }
                drawPath(tail, color, style = stroke)
                drawRoundRect(color, p(9f, 9f), Size(13f*sx, 11f*sy), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f*sx), style = stroke)
            }
            "profile" -> {
                drawCircle(color, 4f*sx, p(12f, 7f), style = stroke)
                drawArc(color, 190f, 160f, false, p(3f, 12f), Size(18f*sx, 12f*sy), style = stroke)
            }
            "notification" -> {
                val bell = Path().apply { moveTo(5f*sx, 17f*sy); lineTo(7f*sx, 15f*sy); lineTo(7f*sx, 10f*sy); cubicTo(7f*sx, 3f*sy, 17f*sx, 3f*sy, 17f*sx, 10f*sy); lineTo(17f*sx, 15f*sy); lineTo(19f*sx, 17f*sy); close() }
                drawPath(bell, color, style = stroke)
                drawLine(color, p(5f,17f), p(19f,17f), strokeWidth = 2f*sx, cap = StrokeCap.Round)
                drawArc(color, 0f, 180f, false, p(9f,17f), Size(6f*sx, 5f*sy), style = stroke)
            }
            "settings" -> {
                drawCircle(color, 7f*sx, p(12f, 12f), style = stroke)
                drawCircle(color, 2.5f*sx, p(12f, 12f), style = stroke)
                for (index in 0 until 8) {
                    val angle = index * (Math.PI / 4.0)
                    val x1 = 12f + kotlin.math.cos(angle).toFloat() * 8f
                    val y1 = 12f + kotlin.math.sin(angle).toFloat() * 8f
                    val x2 = 12f + kotlin.math.cos(angle).toFloat() * 10f
                    val y2 = 12f + kotlin.math.sin(angle).toFloat() * 10f
                    drawLine(color, p(x1,y1), p(x2,y2), strokeWidth = 2.2f*sx, cap = StrokeCap.Round)
                }
            }
            "screen" -> {
                drawLine(color, p(4f,9f), p(4f,5f), strokeWidth = 2f*sx); drawLine(color, p(4f,5f), p(8f,5f), strokeWidth = 2f*sx)
                drawLine(color, p(16f,5f), p(20f,5f), strokeWidth = 2f*sx); drawLine(color, p(20f,5f), p(20f,9f), strokeWidth = 2f*sx)
                drawLine(color, p(4f,15f), p(4f,19f), strokeWidth = 2f*sx); drawLine(color, p(4f,19f), p(8f,19f), strokeWidth = 2f*sx)
                drawLine(color, p(16f,19f), p(20f,19f), strokeWidth = 2f*sx); drawLine(color, p(20f,19f), p(20f,15f), strokeWidth = 2f*sx)
                drawLine(color, p(8f,12f), p(16f,12f), strokeWidth = 2f*sx)
            }
            "image" -> {
                drawRoundRect(color, p(3f,4f), Size(17f*sx,16f*sy), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f*sx), style = stroke)
                drawCircle(color, 2f*sx, p(9f,9f))
                val mountain = Path().apply { moveTo(5f*sx,17f*sy); lineTo(10f*sx,12f*sy); lineTo(13f*sx,15f*sy); lineTo(16f*sx,11f*sy); lineTo(20f*sx,16f*sy) }
                drawPath(mountain, color, style = stroke)
                drawCircle(color, 3.5f*sx, p(19f,18f), style = stroke)
                drawLine(color, p(21.5f,20.5f), p(23f,22f), strokeWidth = 1.8f*sx, cap = StrokeCap.Round)
            }
            "text" -> {
                drawRoundRect(color, p(3f,4f), Size(18f*sx,15f*sy), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f*sx), style = stroke)
                val tail = Path().apply { moveTo(7f*sx,19f*sy); lineTo(7f*sx,16f*sy); lineTo(4f*sx,19f*sy) }
                drawPath(tail, color, style = stroke)
                listOf(8f, 12f, 16f).forEach { drawLine(color, p(6f,it), p(17f,it), strokeWidth = 1.4f*sx) }
            }
            "warning" -> {
                val triangle = Path().apply { moveTo(12f*sx,3f*sy); lineTo(22f*sx,20f*sy); lineTo(2f*sx,20f*sy); close() }
                drawPath(triangle, color, style = stroke)
                drawLine(color, p(12f,9f), p(12f,14f), strokeWidth = 2f*sx, cap = StrokeCap.Round)
                drawCircle(color, 1f*sx, p(12f,17f))
            }
            "robot" -> {
                drawRoundRect(color, p(4f,7f), Size(16f*sx,13f*sy), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f*sx), style = stroke)
                drawLine(color, p(12f,4f), p(12f,7f), strokeWidth = 2f*sx)
                drawCircle(color, 1f*sx, p(12f,3f)); drawCircle(color, 1f*sx, p(9f,12f)); drawCircle(color, 1f*sx, p(15f,12f))
                drawLine(color, p(8f,17f), p(16f,17f), strokeWidth = 1.5f*sx)
            }
            "bulb" -> {
                drawCircle(color, 7f*sx, p(12f,10f), style = stroke)
                drawLine(color, p(9f,17f), p(15f,17f), strokeWidth = 2f*sx, cap = StrokeCap.Round)
                drawLine(color, p(10f,20f), p(14f,20f), strokeWidth = 2f*sx, cap = StrokeCap.Round)
                drawLine(color, p(12f,1f), p(12f,3f), strokeWidth = 1.8f*sx)
            }
        }
    }
}
