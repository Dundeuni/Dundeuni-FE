package com.dundueni.app.feature.floatingbutton

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

// Overlay 권한 확인/요청 + Floating Button Foreground Service 시작/종료 담당

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