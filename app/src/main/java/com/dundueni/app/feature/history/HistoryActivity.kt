package com.dundueni.app.feature.history

import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dundueni.app.ui.theme.DundueniFETheme
import com.dundueni.app.MainActivity
import com.dundueni.app.feature.home.HomeDestination

class HistoryActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_RECORD_ID = "history_record_id"
        internal const val EXTRA_FROM_FAMILY_SHARE = "history_from_family_share"
        fun createIntent(context: Context, recordId: String): Intent =
            Intent(context, HistoryActivity::class.java).putExtra(EXTRA_RECORD_ID, recordId)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DundueniFETheme {
                HistoryRoute(
                    onHome = { finish() },
                    initialRecordId = intent.getStringExtra(EXTRA_RECORD_ID),
                    onNavigationSelected = ::onNavigationSelected,
                )
            }
        }
    }

    private fun onNavigationSelected(destination: HomeDestination) {
        when (destination) {
            HomeDestination.HOME -> {
                if (intent.getBooleanExtra(EXTRA_FROM_FAMILY_SHARE, false)) openMain() else finish()
            }
            HomeDestination.REPORT -> Unit
            HomeDestination.CONSULTATION -> Toast.makeText(this, "준비 중인 기능입니다", Toast.LENGTH_SHORT).show()
            HomeDestination.FAMILY_SHARE -> openMain(MainActivity.EXTRA_OPEN_FAMILY_SHARE)
            HomeDestination.MY_SHELTER -> openMain(MainActivity.EXTRA_OPEN_MY_PAGE)
            HomeDestination.SETTINGS -> openMain(MainActivity.EXTRA_OPEN_SETTINGS)
        }
    }

    private fun openMain(extra: String? = null) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (extra != null) putExtra(extra, true)
        })
        finish()
    }
}
