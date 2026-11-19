package com.dundueni.app.feature.payment

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.dundueni.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject

/** Local Basic allowance. Each completed inspection consumes one of the five available uses. */
class BasicInspectionQuota(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun remaining(): Int = synchronized(lock) { (LIMIT - completedIds().size).coerceAtLeast(0) }

    fun canInspect(): Boolean = remaining() > 0

    internal fun usageBetween(startMillis: Long, endMillis: Long): Usage = synchronized(lock) {
        val ids = completedIds()
        val completionTimes = completionTimes()
        val timestamps = ids.map { completionTimes.optLong(it, 0L) }
        val usedInPeriod = if (timestamps.any { it <= 0L }) null
            else timestamps.count { it in startMillis until endMillis }
        Usage((LIMIT - ids.size).coerceAtLeast(0), usedInPeriod)
    }

    internal data class Usage(val remaining: Int, val usedInPeriod: Int?)

    /** Returns false when another completed inspection has already filled the allowance. */
    fun recordCompleted(id: String): Boolean = synchronized(lock) {
        require(id.isNotBlank())
        val ids = completedIds()
        if (id in ids) return@synchronized true
        if (ids.size >= LIMIT) return@synchronized false
        val completionTimes = completionTimes().put(id, System.currentTimeMillis())
        preferences.edit()
            .putStringSet(KEY_IDS, ids + id)
            .putString(KEY_COMPLETION_TIMES, completionTimes.toString())
            .commit()
    }

    internal fun resetForDebug(): Boolean {
        if (!BuildConfig.DEBUG) return false
        val reset = synchronized(lock) {
            preferences.edit().remove(KEY_IDS).remove(KEY_COMPLETION_TIMES).commit()
        }
        if (reset) resetRevision.value = resetRevision.value + 1L
        return reset
    }

    private fun completedIds(): Set<String> = preferences.getStringSet(KEY_IDS, emptySet())?.toSet().orEmpty()

    private fun completionTimes(): JSONObject = runCatching {
        JSONObject(preferences.getString(KEY_COMPLETION_TIMES, "{}") ?: "{}")
    }.getOrElse { JSONObject() }

    companion object {
        internal const val PREFERENCES_NAME = "basic_inspection_quota"
        const val LIMIT = 5
        private const val KEY_COMPLETION_TIMES = "completion_times"
        private const val KEY_IDS = "completed_ids"
        private val lock = Any()
        val resetRevision = MutableStateFlow(0L)
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
