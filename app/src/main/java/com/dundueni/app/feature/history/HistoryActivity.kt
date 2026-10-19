package com.dundueni.app.feature.history

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dundueni.app.ui.theme.DundueniFETheme

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DundueniFETheme { HistoryRoute(onHome = { finish() }) } }
    }
}
