package com.dundueni.app.feature.auth

import android.content.Context

/** Auth regressions start past the independent first-run tour and restore its prior setting. */
internal class AuthQaOnboardingState(context: Context) {
    private val preferences = context.getSharedPreferences("dundueni_onboarding", Context.MODE_PRIVATE)
    private val previous = if (preferences.contains("completed")) preferences.getBoolean("completed", false) else null
    fun skip() { check(preferences.edit().putBoolean("completed", true).commit()) }
    fun restore() {
        check(preferences.edit().apply {
            if (previous == null) remove("completed") else putBoolean("completed", previous)
        }.commit())
    }
}
