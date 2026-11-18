package com.dundueni.app.feature.url

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import com.dundueni.app.feature.floatingbutton.FloatingButtonManager
import com.dundueni.app.feature.payment.showLimitIfExhausted
import com.dundueni.app.ui.theme.DundueniFETheme
import java.util.UUID

class UrlInputActivity : ComponentActivity() {
    private val overlayOwner = "url-input:${UUID.randomUUID()}"

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
                Box(modifier = Modifier.safeDrawingPadding()) {
                    UrlInputScreen(
                        onInspect = { url ->
                            if (showLimitIfExhausted(this@UrlInputActivity)) return@UrlInputScreen
                            Toast.makeText(this@UrlInputActivity, "링크 검사는 아직 서버에서 지원하지 않아요.", Toast.LENGTH_SHORT).show()
                        },
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}
