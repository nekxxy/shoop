package com.shoop.app.hrv

import kotlin.math.sqrt

/**
 * Rolling beat-to-beat (RR interval) analysis. Pure Kotlin, no Android deps.
 *
 * RMSSD is the standard short-term HRV metric: root mean square of successive
 * differences between adjacent RR intervals. A rolling window keeps the metric
 * responsive while smoothing single-beat noise.
 */
class HrvAnalyzer(private val windowSize: Int = 60) {

    private val rrWindow = ArrayDeque<Int>(windowSize + 1)

    val sampleCount: Int get() = rrWindow.size

    fun addRrInterval(ms: Int) {
        if (ms < 250 || ms > 2000) return // physiologically implausible, drop
        if (rrWindow.size >= windowSize) rrWindow.removeFirst()
        rrWindow.addLast(ms)
    }

    fun clear() = rrWindow.clear()

    /** RMSSD in milliseconds, or null until at least 3 intervals are buffered. */
    fun rmssdMs(): Double? {
        if (rrWindow.size < 3) return null
        var sumSq = 0.0
        var prev = rrWindow.first()
        var n = 0
        for (rr in rrWindow.drop(1)) {
            val d = rr - prev
            sumSq += d.toDouble() * d
            prev = rr
            n++
        }
        if (n == 0) return null
        return sqrt(sumSq / n)
    }

    /** Mean RR in milliseconds over the window. */
    fun meanRrMs(): Double? {
        if (rrWindow.isEmpty()) return null
        return rrWindow.average()
    }
}
