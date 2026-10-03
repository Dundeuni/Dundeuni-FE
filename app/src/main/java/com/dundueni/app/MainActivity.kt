package com.dundueni.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.dundueni.app.feature.floatingbutton.FloatingButtonManager
import com.dundueni.app.ui.theme.DundueniFETheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            DundueniFETheme {

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    Button(
                        modifier = Modifier.padding(innerPadding),
                        onClick = {
                            startFloatingButton()
                        }
                    ) {
                        Text("플로팅 버튼 시작")
                    }
                }
            }
        }
    }

    // Overlay 권한 확인 후 Floating Button 실행
    private fun startFloatingButton() {

        if (FloatingButtonManager.hasOverlayPermission(this)) {

            // 권한 있음 → Foreground Service 실행
            FloatingButtonManager.start(this)

        } else {

            // 권한 없음 → Android Overlay 권한 설정 화면 이동
            FloatingButtonManager.requestOverlayPermission(this)
        }
    }
}