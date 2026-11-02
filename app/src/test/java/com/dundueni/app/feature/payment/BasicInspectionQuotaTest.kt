package com.dundueni.app.feature.payment

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BasicInspectionQuotaTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before fun clearQuota() {
        context.getSharedPreferences("basic_inspection_quota", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun fiveCompletedInspectionsAreAllowedAndDuplicateResultDoesNotConsumeAnotherUse() {
        val quota = BasicInspectionQuota(context)
        assertTrue(quota.recordCompleted("first"))
        assertTrue(quota.recordCompleted("first"))
        assertEquals(4, quota.remaining())
        (2..5).forEach { assertTrue(quota.recordCompleted("result-$it")) }
        assertEquals(0, quota.remaining())
        assertFalse(quota.canInspect())
        assertFalse(quota.recordCompleted("sixth"))
    }

    @Test fun remainingUsesPersistAcrossInstances() {
        val firstInstance = BasicInspectionQuota(context)
        assertEquals(5, firstInstance.remaining())
        assertTrue(firstInstance.recordCompleted("result-1"))
        val restoredInstance = BasicInspectionQuota(context)
        assertEquals(4, restoredInstance.remaining())
        assertTrue(restoredInstance.recordCompleted("result-1"))
        assertEquals(4, firstInstance.remaining())
    }

    @Test fun concurrentCompletionsCannotExceedFiveUses() {
        val appContext = context
        val pool = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        try {
            val results = (1..20).map { index ->
                pool.submit<Boolean> {
                    start.await()
                    BasicInspectionQuota(appContext).recordCompleted("parallel-$index")
                }
            }
            start.countDown()
            assertEquals(5, results.count { it.get(10, TimeUnit.SECONDS) })
            assertEquals(0, BasicInspectionQuota(appContext).remaining())
        } finally {
            pool.shutdownNow()
        }
    }
}
