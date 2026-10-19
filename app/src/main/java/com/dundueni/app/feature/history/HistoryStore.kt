package com.dundueni.app.feature.history

import android.content.Context
import com.dundueni.app.feature.analysisresult.RiskLevel
import org.json.JSONArray
import org.json.JSONObject

/** Reads PB-05's existing metadata store. No message, URL, or evidence text is persisted. */
class HistoryStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("analysis_result_history", Context.MODE_PRIVATE)

    fun read(): List<HistoryRecord> = synchronized(lock) {
        val array = JSONArray(preferences.getString("results", "[]"))
        (0 until array.length()).mapNotNull { index ->
            runCatching {
                val item = array.getJSONObject(index)
                val id = item.getString("id").also { require(it.isNotBlank()) }
                HistoryRecord(
                    id = id,
                    riskLevel = RiskLevel.fromWireValue(item.getString("riskLevel")),
                    savedAt = item.getLong("savedAt").also { require(it >= 0) },
                    type = HistoryType.entries.firstOrNull { it.name == item.optString("type") } ?: HistoryType.UNKNOWN,
                    fraudScore = item.score("fraudScore"),
                    aiScore = item.score("aiScore"),
                )
            }.getOrNull()
        }.sortedByDescending { it.savedAt }
    }

    fun delete(id: String): Boolean = synchronized(lock) {
        val current = JSONArray(preferences.getString("results", "[]"))
        val remaining = JSONArray()
        for (index in 0 until current.length()) {
            val item = current.optJSONObject(index) ?: continue
            if (item.optString("id") != id) remaining.put(item)
        }
        preferences.edit().putString("results", remaining.toString()).commit()
    }

    fun clear(): Boolean = synchronized(lock) {
        preferences.edit().putString("results", "[]").commit()
    }

    private fun JSONObject.score(key: String): Double? =
        if (isNull(key) || !has(key)) null else optDouble(key).takeIf { it.isFinite() && it in 0.0..100.0 }

    private companion object { val lock = Any() }
}
