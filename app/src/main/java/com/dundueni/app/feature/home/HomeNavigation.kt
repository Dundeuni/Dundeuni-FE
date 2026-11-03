package com.dundueni.app.feature.home

import android.content.Intent
import androidx.activity.ComponentActivity
import com.dundueni.app.MainActivity

/** Clears intermediate scan Activities and brings the existing home task to the foreground. */
fun ComponentActivity.returnToHome() {
    startActivity(Intent(this, MainActivity::class.java).apply {
        addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP,
        )
    })
    finish()
}
