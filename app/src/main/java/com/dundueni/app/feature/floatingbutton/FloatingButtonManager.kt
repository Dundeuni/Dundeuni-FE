package com.dundueni.app.feature.floatingbutton

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

// Overlay 권한 확인/요청 + Floating Button Foreground Service 시작/종료 담당

/**
 * MainActivity와 Android의 권한 설정·서비스 실행을 연결한다.
 * 전달받은 Context로 Overlay 권한을 확인하거나 설정 화면을 연다.
 * start와 stop은 FloatingButtonService의 시작·종료를 요청한다.
 * start 자체는 권한을 확인하지 않으므로 호출 측과 서비스가 확인한다.
 * 버튼 View를 만들거나 화면에 붙이는 일은 서비스에 맡긴다.
 */
object FloatingButtonManager {

    // Overlay 권한이 있는지 확인
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    // Overlay 권한 설정 화면 열기
    fun requestOverlayPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )

        context.startActivity(intent)
    }

    // Floating Button Foreground Service 시작
    fun start(context: Context) {
        val intent = Intent(
            context,
            FloatingButtonService::class.java
        )

        // 알림과 함께 동작하는 서비스의 시작을 요청하고, 실제 알림 등록은 서비스가 맡는다.
        ContextCompat.startForegroundService(
            context,
            intent
        )
    }

    // Floating Button Foreground Service 종료
    fun stop(context: Context) {
        val intent = Intent(
            context,
            FloatingButtonService::class.java
        )

        context.stopService(intent)
    }
}