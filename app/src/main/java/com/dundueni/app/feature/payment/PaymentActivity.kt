package com.dundueni.app.feature.payment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dundueni.app.feature.floatingbutton.FloatingButtonManager
import com.dundueni.app.ui.theme.BrandInk
import com.dundueni.app.ui.theme.DisabledSurface
import com.dundueni.app.ui.theme.DundueniDefaultShape
import com.dundueni.app.ui.theme.DundueniDimens
import com.dundueni.app.ui.theme.DundueniFETheme
import com.dundueni.app.ui.theme.DundueniPillShape
import com.dundueni.app.ui.theme.LocalEasyMode
import com.dundueni.app.ui.theme.PageBackground
import com.dundueni.app.ui.theme.ProtectiveBlue
import com.dundueni.app.ui.theme.SecondaryInk
import com.dundueni.app.ui.theme.SoftBlue
import com.dundueni.app.ui.theme.SurfaceLight
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentScreen(remaining: Int, onClose: () -> Unit, initialAnnual: Boolean = false) {
    var annual by rememberSaveable { mutableStateOf(initialAnnual) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPlan by rememberSaveable { mutableStateOf("pro") }
    val easy = LocalEasyMode.current
    val typography = MaterialTheme.typography
    val detailStyle = if (easy) typography.bodyLarge else typography.bodyMedium

    Scaffold(
        containerColor = PageBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(color = PageBackground, shadowElevation = 1.dp) {
                Row(
                    Modifier.fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(if (easy) DundueniDimens.ButtonHeight else DundueniDimens.MinTouchSize),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = BrandInk)
                    }
                    Text(
                        "요금제 안내 및 구독 멤버십",
                        modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                        color = BrandInk,
                        style = if (easy) typography.titleLarge else typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (easy) 24.dp else 20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (easy) {
                    PlanBadge("쉬운 모드", SoftBlue, ProtectiveBlue)
                } else {
                    Text("금융 안심 보장 케어", color = ProtectiveBlue, style = typography.labelLarge)
                }
                Text(
                    buildAnnotatedString {
                        append(if (easy) "더 안심하고 쓰실 수 있는\n" else "온 가족 금융사기 걱정 없는\n")
                        withStyle(SpanStyle(color = ProtectiveBlue)) { append("든든이") }
                        append(if (easy) " 요금제" else " 멤버십을 만나보세요")
                    },
                    color = BrandInk,
                    style = typography.headlineSmall,
                )
                Text(
                    if (annual) "연간 결제 · 2개월 무료" else "내게 맞는 보호 기능을 살펴보세요.",
                    color = SecondaryInk,
                    style = detailStyle,
                )
            }

            BillingPeriodSelector(annual = annual, onSelect = { annual = it })

            PlanCard(
                badge = "기본 제공",
                title = "든든이 베이직",
                price = "0원",
                priceDetail = "평생 무료",
                benefits = listOf(
                    "기본 검사 ${BasicInspectionQuota.LIMIT}회",
                    "의심 스미싱 문자 키워드 감지",
                    "비상 안심 보호자 1명 등록",
                ),
                action = "남은 검사 $remaining/${BasicInspectionQuota.LIMIT}회 · 현재 이용 중인 플랜",
                onClick = { dialog = "basic" },
            )
            PlanCard(
                badge = "가장 인기 있는 선택",
                title = "든든이 프로",
                price = if (annual) "월 2,900원 상당" else "월 3,900원",
                priceDetail = if (annual) "연간 결제 · 2개월 무료" else "연 결제 시 월 2,900원 상당",
                benefits = listOf(
                    "실시간 무제한 상시 플로팅 화면 검사",
                    "수신 전화번호 즉시 사기 DB 정밀 대조",
                    "위험 악성 URL 및 피싱 웹 실시간 차단",
                    "AI 딥페이크 및 변조 음성/이미지 판별",
                ),
                action = if (annual) "연간 프로 플랜 선택하기" else "14일 무료 체험으로 시작하기",
                actionColor = ProtectiveBlue,
                selected = selectedPlan == "pro",
                onClick = {
                    selectedPlan = "pro"
                    dialog = "payment"
                },
            )
            PlanCard(
                badge = "부모님 안심 추천",
                title = "온가족 든든 패밀리",
                price = if (annual) "월 5,500원 상당" else "월 6,900원",
                priceDetail = if (annual) "연간 결제 · 2개월 무료" else "연 결제 시 월 5,500원 상당",
                benefits = listOf(
                    "프로 플랜 기능을 가족 4명까지 이용",
                    "부모님 위험 감지 즉시 보호자 알림",
                ),
                action = "온가족 안심 패밀리 선택하기",
                actionColor = BrandInk,
                selected = selectedPlan == "family",
                onClick = {
                    selectedPlan = "family"
                    dialog = "payment"
                },
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(color = DisabledSurface, modifier = Modifier.padding(bottom = 8.dp))
                Text(
                    "요금제 한눈에 비교하기",
                    color = BrandInk,
                    style = if (easy) typography.titleLarge else typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "화면 검사: 베이직 ${BasicInspectionQuota.LIMIT}회 · 프로/패밀리 무제한",
                    color = SecondaryInk,
                    style = detailStyle,
                )
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    TextButton(onClick = { dialog = "terms" }) {
                        Text("구독 이용약관", color = BrandInk, style = detailStyle, textAlign = TextAlign.Center)
                    }
                    TextButton(onClick = { dialog = "privacy" }) {
                        Text(
                            "개인정보 제3자 제공 동의",
                            color = BrandInk,
                            style = detailStyle.copy(lineBreak = LineBreak.Heading),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            PaymentButton(
                text = when {
                    selectedPlan == "family" && annual -> "연간 패밀리 플랜 선택하기"
                    selectedPlan == "family" -> "온가족 안심 패밀리 선택하기"
                    annual -> "연간 프로 플랜 선택하기"
                    else -> "14일간 0원으로 프로 플랜 시작하기"
                },
                color = if (selectedPlan == "family") BrandInk else ProtectiveBlue,
                onClick = { dialog = "payment" },
            )
        }
    }
    if (dialog != null) {
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = {
                Text(
                    when (dialog) {
                        "basic" -> "현재 이용 중인 플랜"
                        "terms" -> "구독 이용약관"
                        "privacy" -> "개인정보 제3자 제공 동의"
                        else -> "결제 준비 중"
                    },
                    style = if (easy) typography.headlineSmall else typography.titleLarge,
                )
            },
            text = {
                Text(
                    when (dialog) {
                        "basic" -> "베이직 검사가 $remaining/${BasicInspectionQuota.LIMIT}회 남아 있어요."
                        "terms", "privacy" -> "해당 문서가 아직 앱에 연결되지 않았어요."
                        else -> {
                            val plan = if (selectedPlan == "family") "온가족 든든 패밀리" else "든든이 프로"
                            val period = if (annual) "연간" else "월간"
                            "$plan $period 플랜은 현재 앱에서 결제와 무료 체험을 시작할 수 없어요."
                        }
                    },
                    style = detailStyle,
                )
            },
            confirmButton = {
                TextButton(onClick = { dialog = null }) { Text("확인", style = detailStyle) }
            },
        )
    }
}

@Composable
private fun BillingPeriodSelector(annual: Boolean, onSelect: (Boolean) -> Unit) {
    val easy = LocalEasyMode.current
    val typography = MaterialTheme.typography
    Row(
        Modifier.fillMaxWidth().selectableGroup()
            .background(DisabledSurface, MaterialTheme.shapes.large).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(false to "월간 결제", true to "연간 결제").forEach { (value, label) ->
            val selected = annual == value
            Surface(
                selected = selected,
                onClick = { onSelect(value) },
                modifier = Modifier.weight(1f),
                shape = DundueniDefaultShape,
                color = if (selected) SurfaceLight else Color.Transparent,
            ) {
                Box(
                    Modifier.heightIn(min = if (easy) DundueniDimens.ButtonHeight else DundueniDimens.MinTouchSize)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = if (selected) ProtectiveBlue else BrandInk,
                        style = if (easy) typography.titleMedium else typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanCard(
    badge: String,
    title: String,
    price: String,
    priceDetail: String,
    benefits: List<String>,
    action: String,
    actionColor: Color? = null,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val easy = LocalEasyMode.current
    val typography = MaterialTheme.typography
    val stackHeader = (easy && actionColor != null) || LocalDensity.current.fontScale >= 1.3f
    val badgeColor = actionColor ?: SoftBlue
    val titleStyle = if (easy) typography.titleLarge else typography.titleMedium
    val priceStyle = if (easy) typography.headlineSmall.copy(fontSize = 28.sp, lineHeight = 36.sp)
        else typography.titleLarge
    val priceColor = if (easy) actionColor ?: BrandInk else BrandInk
    val benefitStyle = if (easy) typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp)
        else typography.bodyMedium

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (easy && selected) SoftBlue else SurfaceLight,
        border = if (selected) BorderStroke(2.dp, ProtectiveBlue) else BorderStroke(1.dp, DisabledSurface),
        shadowElevation = if (easy && selected) 2.dp else 0.dp,
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(if (easy) 16.dp else 12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxItemsInEachRow = if (stackHeader) 1 else 2,
                ) {
                    Column(
                        Modifier.padding(end = if (stackHeader) 0.dp else 10.dp),
                        verticalArrangement = Arrangement.spacedBy(if (stackHeader) 4.dp else 6.dp),
                    ) {
                        PlanBadge(badge, badgeColor, if (actionColor != null) SurfaceLight else ProtectiveBlue)
                        Text(title, color = BrandInk, style = titleStyle, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        price,
                        color = priceColor,
                        style = priceStyle.copy(lineBreak = LineBreak.Heading),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(priceDetail, color = SecondaryInk, style = if (easy) typography.bodyLarge else typography.bodySmall)
            }

            val benefitModifier = if (easy) {
                Modifier.background(if (selected) SurfaceLight else PageBackground, DundueniDefaultShape)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            } else {
                Modifier
            }
            Column(
                Modifier.fillMaxWidth().then(benefitModifier),
                verticalArrangement = Arrangement.spacedBy(if (easy) 8.dp else 6.dp),
            ) {
                benefits.forEach { benefit ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✓", Modifier.clearAndSetSemantics {}, color = ProtectiveBlue, style = benefitStyle)
                        Text(benefit, color = BrandInk, style = benefitStyle)
                    }
                }
            }

            if (actionColor != null) {
                PaymentButton(text = action, color = actionColor, onClick = onClick)
            } else {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = if (easy) 64.dp else DundueniDimens.MinTouchSize),
                    shape = DundueniDefaultShape,
                    border = BorderStroke(1.dp, DisabledSurface),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = PageBackground, contentColor = BrandInk),
                ) {
                    Text(
                        action,
                        style = if (easy) typography.bodyLarge else typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanBadge(text: String, background: Color, foreground: Color) {
    Text(
        text,
        Modifier.background(background, DundueniPillShape).padding(horizontal = 8.dp, vertical = 4.dp),
        color = foreground,
        style = if (LocalEasyMode.current) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun PaymentButton(text: String, color: Color, onClick: () -> Unit) {
    val easy = LocalEasyMode.current
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = if (easy) 64.dp else DundueniDimens.ButtonHeight),
        shape = DundueniDefaultShape,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = SurfaceLight),
    ) {
        Text(
            text,
            style = if (easy) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}
