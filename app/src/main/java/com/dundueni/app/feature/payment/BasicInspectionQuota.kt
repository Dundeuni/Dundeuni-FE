package com.dundueni.app.feature.payment

import android.app.Activity
import android.content.Context
import android.content.Intent

/** Local Basic allowance. A completed inspection consumes one of the five available uses. */
class BasicInspectionQuota(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("basic_inspection_quota", Context.MODE_PRIVATE)

    fun remaining(): Int = synchronized(lock) { (LIMIT - completedIds().size).coerceAtLeast(0) }

    fun canInspect(): Boolean = remaining() > 0

    /** Returns false when another completed inspection has already filled the allowance. */
    fun recordCompleted(id: String): Boolean = synchronized(lock) {
        require(id.isNotBlank())
        val ids = completedIds()
        if (id in ids) return@synchronized true
        if (ids.size >= LIMIT) return@synchronized false
        preferences.edit().putStringSet(KEY_IDS, ids + id).commit()
    }

    private fun completedIds(): Set<String> = preferences.getStringSet(KEY_IDS, emptySet())?.toSet().orEmpty()

    companion object {
        const val LIMIT = 5
        private const val KEY_IDS = "completed_ids"
        private val lock = Any()
    }
}

/** Opens the shared exhausted-use popup and returns true when inspection must be blocked. */
fun showLimitIfExhausted(context: Context): Boolean {
    if (BasicInspectionQuota(context).canInspect()) return false
    context.startActivity(Intent(context, InspectionLimitActivity::class.java).apply {
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
    return true
}
