package com.dundueni.app.feature.photopicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

// 버튼 누르면 사진 선택 화면이 뜨고, 사진 선택 후 uri를 받아오는 테스트용 Activity
/**
 * 플로팅 메뉴의 이미지 검사 요청을 받아 사진 선택 화면을 여는 Activity다.
 * 시스템 Photo Picker는 사용자가 공유할 사진을 직접 고르는 선택 화면이다.
 * EXTRA_OPEN_PICKER로 화면 진입 직후 선택기를 열지 여부를 전달받는다.
 * 이 값이 없으면 사용자가 화면의 사진 선택 버튼을 눌러 시작한다.
 * 실제 선택 결과 수신과 분석 화면 이동은 PhotoPickerTestScreen이 담당한다.
 */
class PhotoPickerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_PICKER = "com.dundueni.app.extra.OPEN_PHOTO_PICKER"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PhotoPickerTestScreen(
                openPickerOnLaunch = intent.getBooleanExtra(EXTRA_OPEN_PICKER, false)
            )
        }
    }
}
