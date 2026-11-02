package com.dundueni.app.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dundueni.app.R
import com.dundueni.app.feature.home.QuickScanCard
import com.dundueni.app.ui.component.DundueniButton
import com.dundueni.app.ui.component.DundueniButtonType

private val OnboardingBackground = Color(0xFFF7F8FC)
private val OnboardingInk = Color(0xFF171B24)
private val OnboardingMuted = Color(0xFF626B79)
private val OnboardingBlue = Color(0xFF0758D5)
private val OnboardingPaleBlue = Color(0xFFEAF0FF)

@Composable
fun OnboardingScreen(
    page: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onOverlayPermission: () -> Unit,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("든든이 안내", color = OnboardingInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("$page / 4", color = OnboardingBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color(0xFFE3E8F2))) {
            Box(
                Modifier.fillMaxWidth(page / 4f).height(6.dp).clip(CircleShape).background(OnboardingBlue),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 28.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            when (page) {
                1 -> ServiceIntroPage()
                2 -> InspectionGuidePage()
                3 -> OverlayPermissionPage()
                else -> ScreenCapturePermissionPage()
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(4) { index ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index + 1 == page) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (index + 1 == page) OnboardingBlue else Color(0xFFCBD3E1)),
                )
            }
        }

        if (page < 4) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onSkip, modifier = Modifier.weight(1f)) {
                    Text("건너뛰기", color = OnboardingMuted, fontSize = 15.sp)
                }
                Spacer(Modifier.size(12.dp))
                DundueniButton(
                    text = when (page) {
                        3 -> "권한 설정하기"
                        else -> "다음"
                    },
                    onClick = if (page == 3) onOverlayPermission else onNext,
                    modifier = Modifier.weight(2f),
                    type = DundueniButtonType.PRIMARY,
                )
            }
        } else {
            DundueniButton(
                text = "시작하기",
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                type = DundueniButtonType.PRIMARY,
            )
        }
    }
}

@Composable
private fun ServiceIntroPage() {
    OnboardingTitle("든든이가 지켜드릴게요")
    OnboardingBody("의심스러운 사진, 문자, 전화번호를\n쉽고 빠르게 검사할 수 있어요.")
    Spacer(Modifier.height(8.dp))
    Text(
        "분석 결과와 함께 위험한 이유,\n대처 방법까지 안내해드려요.",
        color = OnboardingMuted,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )
    Spacer(Modifier.height(24.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.dundeuni_mascot_wink),
            contentDescription = "든든이 마스코트",
            modifier = Modifier.size(210.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun InspectionGuidePage() {
    OnboardingTitle("버튼 하나로 간편하게 검사해요")
    OnboardingBody("든든이 버튼을 누른 뒤\n필요한 검사를 선택하세요.")
    Spacer(Modifier.height(18.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InspectionFeature(
                icon = "screen",
                title = "화면 검사",
                description = "지금 보고 있는 스마트폰 화면을 검사해요.",
                modifier = Modifier.weight(1f),
            )
            InspectionFeature(
                icon = "image",
                title = "사진 검사",
                description = "갤러리에서 선택한 사진 한 장을 분석해요.",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InspectionFeature(
                icon = "text",
                title = "문자 검사",
                description = "문자 내용을 분석해 사기 위험을 확인해요.",
                modifier = Modifier.weight(1f),
            )
            InspectionFeature(
                icon = "phone",
                title = "전화번호 검사",
                description = "전화번호의 스팸·사기 신고 정보를 확인해요.",
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Surface(color = OnboardingPaleBlue, shape = RoundedCornerShape(16.dp)) {
        Text(
            "검사 후 결과와 대응 방법을\n바로 확인할 수 있어요.",
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            color = OnboardingInk,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun InspectionFeature(
    icon: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        QuickScanCard(
            icon = icon,
            label = title,
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
        )
        Text(
            text = description,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            color = OnboardingMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OverlayPermissionPage() {
    OnboardingTitle("다른 앱 위에 표시를 허용해주세요")
    OnboardingBody("어떤 앱을 사용 중이든\n든든이 버튼을 띄워 바로 검사할 수 있어요.")
    Spacer(Modifier.height(10.dp))
    Text("이 권한은 처음 한 번만 허용하면 돼요.", color = OnboardingBlue, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(18.dp))
    PermissionExampleImage(
        resource = R.drawable.windowmanagersystem,
        description = "다른 앱 위에 표시 권한 설정 화면 예시",
        height = 250.dp,
    )
}

@Composable
private fun ScreenCapturePermissionPage() {
    OnboardingTitle("화면 검사 시 권한 동의가 필요해요")
    OnboardingBody("화면 검사를 시작하면\n시스템 권한 동의창이 매번 나타나요.")
    Spacer(Modifier.height(16.dp))
    PermissionExampleImage(
        resource = R.drawable.mediaprojectionsystem,
        description = "화면 검사 MediaProjection 시스템 동의창 예시",
        height = 270.dp,
    )
    Spacer(Modifier.height(12.dp))
    Surface(color = OnboardingPaleBlue, shape = RoundedCornerShape(14.dp)) {
        Text(
            "검사하려는 화면을 선택한 뒤\n‘공유’ 또는 ‘시작’ 버튼을 눌러주세요.",
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            color = OnboardingInk,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "화면에 보이는 정보가 함께 표시될 수 있으니\n비밀번호나 개인정보가 없는지 확인해주세요.",
        color = OnboardingMuted,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
}

@Composable
private fun OnboardingTitle(text: String) {
    Text(
        text = text,
        color = OnboardingInk,
        fontSize = 25.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun OnboardingBody(text: String) {
    Text(text, color = OnboardingInk, fontSize = 18.sp, lineHeight = 27.sp)
}

@Composable
private fun FeatureTile(icon: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(OnboardingPaleBlue),
                contentAlignment = Alignment.Center,
            ) {
                Text(icon, color = OnboardingBlue, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(10.dp))
            Text(label, color = OnboardingInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PermissionExampleImage(resource: Int, description: String, height: androidx.compose.ui.unit.Dp) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), shadowElevation = 1.dp) {
        Image(
            painter = painterResource(resource),
            contentDescription = description,
            modifier = Modifier.fillMaxWidth().height(height).padding(8.dp),
            contentScale = ContentScale.Fit,
        )
    }
}
