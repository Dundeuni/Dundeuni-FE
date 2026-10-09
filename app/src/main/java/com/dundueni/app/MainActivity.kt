package com.dundueni.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.dundueni.app.feature.floatingbutton.FloatingButtonManager
import com.dundueni.app.ui.theme.DundueniFETheme

/**
 * 사용자가 플로팅 버튼 기능을 시작하는 앱 진입 화면이다.
 * 시작 버튼을 누르면 FloatingButtonManager를 통해 Overlay 권한을 확인한다.
 * Overlay는 다른 앱 위에 View를 표시하는 기능이며 별도 권한이 필요하다.
 * 권한이 있으면 서비스를 시작하고, 없으면 Android 설정 화면을 연다.
 * 실제 플로팅 버튼과 메뉴는 FloatingButtonService가 관리한다.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            DundueniFETheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Button(onClick = { startFloatingButton() }) {
                            Text("플로팅 버튼 시작")
                        }
                        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                            Button(onClick = {
                                startActivity(Intent().setClassName(
                                    this@MainActivity,
                                    "com.dundueni.app.feature.analysisresult.AnalysisResultPreviewActivity",
                                ))
                            }) {
                                Text("PB-05 분석 결과 미리보기")
                            }
                        }
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
            // 설정에서 돌아온 뒤에는 시작 버튼을 다시 눌러 권한을 확인한다.
            FloatingButtonManager.requestOverlayPermission(this)
        }
    }
}
