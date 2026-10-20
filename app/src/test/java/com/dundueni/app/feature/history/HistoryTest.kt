package com.dundueni.app.feature.history

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dundueni.app.feature.analysisresult.RiskLevel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HistoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preferences = context.getSharedPreferences("analysis_result_history", Context.MODE_PRIVATE)
    private val store = HistoryStore(context)

    @Before fun reset() { preferences.edit().clear().commit() }

    @Test fun readsExistingPb05RecordsWithoutInventingMissingContent() {
        preferences.edit().putString("results", """[{"id":"old","riskLevel":"SAFE","savedAt":100,"fraudScore":12}]""").commit()
        val item = store.read().single()
        assertEquals(HistoryType.UNKNOWN, item.type)
        assertNull(item.preview)
        assertEquals(12.0, item.fraudScore!!, 0.0)
        assertNull(item.aiScore)
    }

    @Test fun deleteOnePreservesOtherRecordsAndClearPersistsEmptyState() {
        preferences.edit().putString("results", """[{"id":"one","riskLevel":"SAFE","savedAt":100},{"id":"two","riskLevel":"DANGER","savedAt":200}]""").commit()
        assertTrue(store.delete("one"))
        assertEquals(listOf("two"), HistoryStore(context).read().map { it.id })
        assertTrue(store.clear())
        assertTrue(HistoryStore(context).read().isEmpty())
    }

    @Test fun malformedEntriesDoNotHideValidRecordsAndBadScoresAreOmitted() {
        preferences.edit().putString("results", """[{"id":"bad","riskLevel":"ALIEN","savedAt":100},{"id":"valid","riskLevel":"CAUTION","savedAt":200,"aiScore":101}]""").commit()
        assertEquals("valid", store.read().single().id)
        assertNull(store.read().single().aiScore)
    }

    @Test fun filtersCombineTypeAndQueryAndKeepNewestFirst() {
        val records = listOf(
            HistoryRecord("old", RiskLevel.SAFE, 100, HistoryType.TEXT),
            HistoryRecord("new", RiskLevel.DANGER, 300, HistoryType.TEXT),
            HistoryRecord("image", RiskLevel.DANGER, 200, HistoryType.IMAGE),
        )
        assertEquals(listOf("new"), filterHistory(records, HistoryType.TEXT, "위험").map { it.id })
        assertEquals(listOf("new", "image", "old"), filterHistory(records, null, "").map { it.id })
        assertTrue(filterHistory(records, null, "없는 검색어").isEmpty())
    }

    @Test fun resultSerializationAndStoragePreserveEveryInputTypeWithoutOriginalContent() {
        HistoryType.entries.filter { it != HistoryType.UNKNOWN }.forEachIndexed { index, type ->
            val result = com.dundueni.app.feature.analysisresult.AnalysisResult(
                id = "typed-$index", riskLevel = RiskLevel.SAFE, summary = "summary",
                historyType = type, sourceText = "private-message", sourceUrl = "https://private.example",
            )
            val decoded = com.dundueni.app.feature.analysisresult.AnalysisResultJsonAdapter.decode(
                com.google.gson.Gson().toJson(result),
            )
            assertTrue(store.save(decoded, 100L + index))
            assertEquals(result.id, filterHistory(HistoryStore(context).read(), type, "").single().id)
        }
        val stored = preferences.getString("results", "")!!
        assertFalse(stored.contains("private-message"))
        assertFalse(stored.contains("private.example"))
        assertEquals(HistoryType.entries.size - 1, store.read().size)
    }

    @Test fun savingTypedRecordPreservesLegacyRecordAndItsDeletion() {
        preferences.edit().putString("results", """[{"id":"old","riskLevel":"SAFE","savedAt":100}]""").commit()
        val result = com.dundueni.app.feature.analysisresult.AnalysisResult(
            "new", RiskLevel.SAFE, "summary", historyType = HistoryType.IMAGE,
        )
        assertTrue(store.save(result, 200))
        assertTrue(store.save(result, 300))
        assertEquals(2, store.read().size)
        assertEquals(HistoryType.UNKNOWN, store.read().last().type)
        assertTrue(store.delete("old"))
        assertEquals("new", store.read().single().id)
    }
}
