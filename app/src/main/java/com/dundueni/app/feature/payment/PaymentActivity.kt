package com.dundueni.app.feature.payment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dundueni.app.ui.theme.DundueniFETheme
import com.dundueni.app.ui.theme.DundueniDefaultShape
import com.dundueni.app.ui.theme.DundueniDimens
import com.dundueni.app.ui.theme.ProtectiveBlue
import com.dundueni.app.ui.theme.SurfaceLight
import com.dundueni.app.ui.component.DundueniButton
import com.dundueni.app.ui.component.DundueniButtonType
import com.dundueni.app.feature.floatingbutton.FloatingButtonManager
import java.util.UUID

class PaymentActivity : ComponentActivity() {
    private val overlayOwner = "payment:${UUID.randomUUID()}"

    override fun onStart() {
        super.onStart()
        FloatingButtonManager.suspendForActivity(overlayOwner)
    }

    override fun onStop() {
        FloatingButtonManager.resumeForActivity(overlayOwner)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DundueniFETheme {
                PaymentScreen(
                    remaining = BasicInspectionQuota(this).remaining(),
                    onClose = ::finish,
                )
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, PaymentActivity::class.java)
            .addFlags(if (context is ComponentActivity) 0 else Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@Composable
fun PaymentScreen(remaining: Int, onClose: () -> Unit) {
    var annual by rememberSaveable { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPlan by rememberSaveable { mutableStateOf("pro") }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("←", color = Color(0xFF171D2A)) }
            Text("요금제 안내 및 구독 멤버십", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onClose) { Text("✕", color = Color(0xFF171D2A)) }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(20.dp))
            Text("금융 안심 보장 케어", color = ProtectiveBlue, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(12.dp))
            Text("온 가족 금융사기 걱정 없는\n든든이 멤버십을 만나보세요", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(if (annual) "연간 결제 · 2개월 무료" else "내게 맞는 보호 기능을 살펴보세요.", color = Color.Gray)
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth().background(Color(0xFFE3E8F4), RoundedCornerShape(30.dp)).padding(4.dp)) {
                listOf(false to "월간 결제", true to "연간 결제").forEach { (value, label) ->
                    Surface(onClick = { annual = value }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp), color = if (annual == value) Color.White else Color.Transparent) {
                        Box(Modifier.padding(12.dp), contentAlignment = Alignment.Center) { Text(label, fontWeight = FontWeight.Medium) }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            PlanCard("기본 제공", "든든이 베이직", "0원", "평생 무료", listOf("기본 검사 5회", "의심 스미싱 문자 키워드 감지", "비상 안심 보호자 1명 등록"), "남은 검사 $remaining/${BasicInspectionQuota.LIMIT}회 · 현재 이용 중인 플랜", false, false) { dialog = "basic" }
            Spacer(Modifier.height(16.dp))
            PlanCard("가장 인기 있는 선택", "든든이 프로", if (annual) "월 2,900원 상당" else "월 3,900원", if (annual) "연간 결제 · 2개월 무료" else "연 결제 시 월 2,900원 상당", listOf("실시간 무제한 상시 플로팅 화면 검사", "수신 전화번호 즉시 사기 DB 정밀 대조", "위험 악성 URL 및 피싱 웹 실시간 차단", "AI 딥페이크 및 변조 음성/이미지 판별"), if (annual) "연간 프로 플랜 선택하기" else "14일 무료 체험으로 시작하기", true, selectedPlan == "pro") { selectedPlan = "pro"; dialog = "payment" }
            Spacer(Modifier.height(16.dp))
            PlanCard("부모님 안심 추천", "온가족 든든 패밀리", if (annual) "월 5,500원 상당" else "월 6,900원", if (annual) "연간 결제 · 2개월 무료" else "연 결제 시 월 5,500원 상당", listOf("프로 플랜 기능을 가족 4명까지 이용", "부모님 위험 감지 즉시 보호자 알림"), "온가족 안심 패밀리 선택하기", true, selectedPlan == "family") { selectedPlan = "family"; dialog = "payment" }
            Spacer(Modifier.height(24.dp))
            Text("요금제 한눈에 비교하기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("화면 검사: 베이직 5회 · 프로/패밀리 무제한", color = Color.DarkGray)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = { dialog = "terms" }) { Text("구독 이용약관") }
                TextButton(onClick = { dialog = "privacy" }) { Text("개인정보 제3자 제공 동의") }
            }
            Spacer(Modifier.height(30.dp))
            DundueniButton(onClick = { dialog = "payment" }, modifier = Modifier.fillMaxWidth(), type = DundueniButtonType.SECONDARY,
                text = when {
                    selectedPlan == "family" && annual -> "연간 패밀리 플랜 선택하기"
                    selectedPlan == "family" -> "온가족 안심 패밀리 선택하기"
                    annual -> "연간 프로 플랜 선택하기"
                    else -> "14일간 0원으로 프로 플랜 시작하기"
                })
            Spacer(Modifier.height(28.dp))
        }
    }
    if (dialog != null) AlertDialog(
        onDismissRequest = { dialog = null },
        title = { Text(when (dialog) {
            "basic" -> "현재 이용 중인 플랜"
            "terms" -> "구독 이용약관"
            "privacy" -> "개인정보 제3자 제공 동의"
            else -> "결제 준비 중"
        }) },
        text = { Text(when (dialog) {
            "basic" -> "베이직 검사가 $remaining/${BasicInspectionQuota.LIMIT}회 남아 있어요."
            "terms", "privacy" -> "해당 문서가 아직 앱에 연결되지 않았어요."
            else -> "${if (selectedPlan == "family") "온가족 든든 패밀리" else "든든이 프로"} ${if (annual) "연간" else "월간"} 플랜은 현재 앱에서 결제와 무료 체험을 시작할 수 없어요."
        }) },
        confirmButton = { TextButton(onClick = { dialog = null }) { Text("확인") } },
    )
}

@Composable
private fun PlanCard(badge: String, title: String, price: String, priceDetail: String, benefits: List<String>, action: String, highlighted: Boolean, selected: Boolean, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = SurfaceLight, shadowElevation = 2.dp,
        border = if (selected) BorderStroke(2.dp, ProtectiveBlue) else null) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(badge, color = ProtectiveBlue, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(price, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(priceDetail, color = if (highlighted) ProtectiveBlue else Color.Gray, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(20.dp))
            benefits.forEach { Text("✓  $it", modifier = Modifier.padding(bottom = 10.dp), style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(8.dp))
            if (highlighted) {
                DundueniButton(text = action, onClick = onClick, modifier = Modifier.fillMaxWidth(), type = DundueniButtonType.SECONDARY)
            } else {
                OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(DundueniDimens.ButtonHeight), shape = DundueniDefaultShape) { Text(action) }
            }
        }
    }
}
