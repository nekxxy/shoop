package com.shoop.app

import com.shoop.app.score.DashboardMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardMetricsTest {

    @Test
    fun `sleep debt sums shortfalls only`() {
        val debt = DashboardMetrics.sleepDebtMinutes(listOf(400.0, 500.0, 300.0), 480.0)
        assertEquals(260.0, debt, 0.01) // 80 + 0 + 180
    }

    @Test
    fun `sleep debt uses at most 7 nights`() {
        val nights = List(10) { 240.0 }
        val debt = DashboardMetrics.sleepDebtMinutes(nights, 480.0)
        assertEquals(7 * 240.0, debt, 0.01)
    }

    @Test
    fun `consistency is 100 for identical bedtimes`() {
        assertEquals(100, DashboardMetrics.sleepConsistencyPct(listOf(300.0, 300.0, 300.0)))
    }

    @Test
    fun `consistency drops with spread`() {
        val tight = DashboardMetrics.sleepConsistencyPct(listOf(300.0, 306.0, 294.0))
        val loose = DashboardMetrics.sleepConsistencyPct(listOf(240.0, 360.0, 300.0))
        assertEquals(true, tight > loose)
        assertEquals(true, loose in 0..60)
    }

    @Test
    fun `performance caps at 100`() {
        assertEquals(100, DashboardMetrics.sleepPerformancePct(listOf(600.0, 540.0), 480.0))
        assertEquals(50, DashboardMetrics.sleepPerformancePct(listOf(240.0), 480.0))
    }

    @Test
    fun `efficiency ratio`() {
        assertEquals(80, DashboardMetrics.sleepEfficiencyPct(listOf(400.0), listOf(500.0)))
        assertEquals(0, DashboardMetrics.sleepEfficiencyPct(listOf(400.0), listOf(0.0)))
    }

    @Test
    fun `zone minutes bucket by zone`() {
        val z = DashboardMetrics.zoneMinutes(listOf(100 to 60.0, 140 to 60.0, 180 to 120.0), 190)
        assertEquals(1.0, z[1], 0.01)
        assertEquals(1.0, z[3], 0.01)
        assertEquals(2.0, z[5], 0.01)
        assertEquals(0.0, z[0], 0.01)
    }
}
