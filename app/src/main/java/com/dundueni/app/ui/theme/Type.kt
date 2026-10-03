package com.dundueni.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 든든이 앱에서 공통으로 사용할 글자 스타일을 정의하는 파일.
 *
 * 화면마다 글자 크기, 굵기 등을 직접 지정하지 않고
 * MaterialTheme.typography를 통해 공통 스타일을 사용한다.
 *
 * 사용 예시:
 *
 * Text(
 *     text = "검사하기",
 *     style = MaterialTheme.typography.bodyLarge
 * )
 *
 * 현재는 bodyLarge만 직접 정의해두고,
 * 나머지 스타일은 Material3 기본값을 사용한다.
 */

val Typography = Typography(

    // 일반 본문이나 버튼 등에 사용할 기본 글자 스타일
    bodyLarge = TextStyle(

        // 기본 시스템 폰트 사용
        fontFamily = FontFamily.Default,

        // 글자 굵기
        fontWeight = FontWeight.Normal,

        // 글자 크기
        fontSize = 16.sp,

        // 줄 간격
        lineHeight = 24.sp,

        // 글자 사이 간격
        letterSpacing = 0.5.sp
    )

    /*
     * 필요하면 이후 다른 글자 스타일도 추가 가능.
     *
     * 예:
     *
     * titleLarge = 화면 제목
     * labelLarge = 버튼 글자
     * labelSmall = 작은 설명 / 라벨
     *
     * titleLarge = TextStyle(
     *     fontFamily = FontFamily.Default,
     *     fontWeight = FontWeight.Normal,
     *     fontSize = 22.sp,
     *     lineHeight = 28.sp,
     *     letterSpacing = 0.sp
     * ),
     *
     * labelSmall = TextStyle(
     *     fontFamily = FontFamily.Default,
     *     fontWeight = FontWeight.Medium,
     *     fontSize = 11.sp,
     *     lineHeight = 16.sp,
     *     letterSpacing = 0.5.sp
     * )
     */
)