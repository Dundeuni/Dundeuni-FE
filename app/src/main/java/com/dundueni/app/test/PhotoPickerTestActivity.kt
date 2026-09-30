package com.dundueni.app.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent



// 버튼 누르면 사진 선택 화면이 뜨고, 사진 선택 후 uri를 받아오는 테스트용 Activity
class PhotoPickerTestActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PhotoPickerTestScreen()
        }
    }
}

