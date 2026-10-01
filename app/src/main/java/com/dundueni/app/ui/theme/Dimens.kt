package com.dundueni.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 든든이 앱에서 공통으로 사용하는 크기,간격 값을 모아둔 파일.
 *
 * 버튼 높이, 터치 영역, 플로팅 버튼 크기처럼
 * 여러 화면에서 반복해서 사용하는 값을 한 곳에서 관리한다.
 */
object DundueniDimens {

    // 사용자가 누르기 편하도록 보장하는 최소 터치 영역 크기
    val MinTouchSize = 48.dp

    // 기본 주요 버튼 높이
    val ButtonHeight = 56.dp

    // 검색창 / 입력창 기본 높이
    val SearchBarHeight = 52.dp

    // 플로팅 가디언 버튼 기본 크기
    val FloatingButtonSize = 54.dp

    // 카드, 버튼 등에 사용하는 기본 둥근 모서리 값
    val DefaultRadius = 12.dp
}


