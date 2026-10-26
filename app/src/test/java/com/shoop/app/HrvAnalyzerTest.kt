package com.shoop.app

import com.shoop.app.hrv.HrvAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.sqrt

class HrvAnalyzerTest {

    @Test
    fun `rmssd needs at least 3 intervals`() {
        val a = HrvAnalyzer()
        a.addRrInterval(800)
        a.addRrInterval(820)
        assertNull(a.rmssdMs())
        a.addRrInterval(810)
        assertEquals(true, a.rmssdMs() != null)
    }

    @Test
    fun `rmssd matches hand computation`() {
        val a = HrvAnalyzer()
        listOf(800, 810, 790, 820).forEach { a.addRrInterval(it) }
        // successive diffs: 10, -20, 30 -> squares 100+400+900=1400 /3 -> sqrt
        val expected = sqrt(1400.0 / 3)
        assertEquals(expected, a.rmssdMs()!!, 0.001)
    }

    @Test
    fun `constant rr gives zero rmssd`() {
        val a = HrvAnalyzer()
        repeat(10) { a.addRrInterval(850) }
        assertEquals(0.0, a.rmssdMs()!!, 0.0001)
    }

    @Test
    fun `implausible intervals rejected`() {
        val a = HrvAnalyzer()
        a.addRrInterval(100)   // too fast
        a.addRrInterval(5000)  // too slow
        assertEquals(0, a.sampleCount)
    }

    @Test
    fun `window evicts oldest`() {
        val a = HrvAnalyzer(windowSize = 5)
        repeat(10) { a.addRrInterval(800 + it) }
        assertEquals(5, a.sampleCount)
        assertEquals(809.0, a.meanRrMs()!! - 4.0 + 4.0, 5.0) // mean of 805..809 = 807
    }
}
