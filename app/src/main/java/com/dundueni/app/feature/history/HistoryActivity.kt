package com.dundueni.app.feature.history

import android.os.Bundle
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dundueni.app.ui.theme.DundueniFETheme

class HistoryActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_RECORD_ID = "history_record_id"
        fun createIntent(context: Context, recordId: String): Intent =
            Intent(context, HistoryActivity::class.java).putExtra(EXTRA_RECORD_ID, recordId)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DundueniFETheme { HistoryRoute(onHome = { finish() }, initialRecordId = intent.getStringExtra(EXTRA_RECORD_ID)) } }
    }
}
