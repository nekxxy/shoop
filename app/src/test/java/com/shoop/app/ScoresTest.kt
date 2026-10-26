package com.shoop.app

import com.shoop.app.score.Scores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoresTest {

    @Test
    fun `strain is zero at rest`() {
        val s = Scores.strain(listOf(55 to 600.0), restingHr = 60)
        assertEquals(0.0, s, 0.001)
    }

    @Test
    fun `strain grows with intensity and duration`() {
        val easy = Scores.strain(List(60) { 100 to 60.0 }, restingHr = 60)
        val hard = Scores.strain(List(60) { 160 to 60.0 }, restingHr = 60)
        assertTrue(hard > easy)
        assertTrue(easy > 0)
    }

    @Test
    fun `strain capped at 21`() {
        val s = Scores.strain(List(10_000) { 185 to 60.0 }, restingHr = 60)
        assertTrue(s <= 21.0)
        assertTrue(s > 20.0) // ~asymptote
    }

    @Test
    fun `recovery at baseline is mid-high`() {
        val r = Scores.recovery(rmssdMs = 40.0, restingHr = 60, baselineRmssdMs = 40.0, baselineRestingHr = 60)
        assertTrue("expected mid recovery at baseline, got $r", r in 45..75)
    }

    @Test
    fun `recovery drops with low hrv and high rhr`() {
        val good = Scores.recovery(60.0, 58, 40.0, 60)
        val bad = Scores.recovery(20.0, 70, 40.0, 60)
        assertTrue(good > bad)
        assertTrue(bad < 34)
        assertTrue(good >= 67)
    }

    @Test
    fun `sleep score rewards duration and restfulness`() {
        val full = Scores.sleep(480.0, 480.0, 0.9)
        val short = Scores.sleep(240.0, 480.0, 0.9)
        val restless = Scores.sleep(480.0, 480.0, 0.2)
        assertTrue(full > short)
        assertTrue(full > restless)
        assertTrue(full <= 100)
    }

    @Test
    fun `zones map correctly`() {
        assertEquals(0, Scores.zone(80, 190))
        assertEquals(1, Scores.zone(100, 190))
        assertEquals(3, Scores.zone(140, 190))
        assertEquals(5, Scores.zone(180, 190))
    }
}
