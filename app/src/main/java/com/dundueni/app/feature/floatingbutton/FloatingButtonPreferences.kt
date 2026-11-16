package com.dundueni.app.feature.floatingbutton

import android.content.Context

/** Shared settings used by the Settings screen, floating overlay, and inspection result UI. */
data class FloatingButtonPreferences(
    val sizeDp: Int,
    val opacity: Float,
    val menuTextScale: Float,
    val hapticsEnabled: Boolean,
) {
    companion object {
        const val PREFERENCES_NAME = "dundueni_settings"
        const val KEY_ENABLED = "floating_button_enabled"
        const val KEY_SIZE = "button_size"
        const val KEY_OPACITY = "button_opacity"
        const val KEY_FONT_SCALE = "font_scale"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_HAPTICS = "haptics"
        const val KEY_EASY_MODE = "easy_mode"

        fun read(context: Context): FloatingButtonPreferences {
            val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            val sizeDp = when (preferences.getString(KEY_SIZE, "보통")) {
                "작게" -> 56
                "크게" -> 88
                else -> 72
            }
            return FloatingButtonPreferences(
                sizeDp = sizeDp,
                opacity = preferences.getFloat(KEY_OPACITY, .85f).coerceIn(.25f, 1f),
                menuTextScale = (sizeDp / 72f).coerceIn(.7f, 1.5f),
                hapticsEnabled = preferences.getBoolean(KEY_HAPTICS, true),
            )
        }

        fun appTextScale(context: Context): Float {
            val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            val legacyScale = preferences.getFloat(KEY_FONT_SCALE, 1f)
            val preset = preferences.getString(KEY_FONT_SIZE, null) ?: when {
                legacyScale < .93f -> "작게"
                legacyScale > 1.08f -> "크게"
                else -> "보통"
            }
            return when (preset) {
                "작게" -> .9f
                "크게" -> 1.2f
                else -> 1f
            }
        }
    }
}
