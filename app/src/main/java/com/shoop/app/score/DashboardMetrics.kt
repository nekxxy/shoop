package com.shoop.app.score

import kotlin.math.sqrt

/**
 * Dashboard metrics, all computed from recorded HR sessions or manual entries.
 * Transparent formulas - no invented sensors.
 */
object DashboardMetrics {

    /** Cumulative sleep shortfall vs need over the given nights, in minutes. */
    fun sleepDebtMinutes(asleepMinutesPerNight: List<Double>, needMinutes: Double): Double {
        var debt = 0.0
        for (a in asleepMinutesPerNight.takeLast(7)) {
            debt += (needMinutes - a).coerceAtLeast(0.0)
        }
        return debt
    }

    /**
     * Sleep consistency 0..100: how similar bedtimes are across nights.
     * 100 = identical bedtimes; a 2 h spread maps to 0.
     * @param bedtimesMinutes minutes after 18:00 of each night's start (wraps past midnight)
     */
    fun sleepConsistencyPct(bedtimesMinutes: List<Double>): Int {
        if (bedtimesMinutes.size < 2) return 0
        val mean = bedtimesMinutes.average()
        val variance = bedtimesMinutes.map { (it - mean) * (it - mean) }.average()
        val stdev = sqrt(variance)
        return (100.0 * (1.0 - stdev / 120.0)).toInt().coerceIn(0, 100)
    }

    /** Sleep performance 0..100: average achieved sleep vs need. */
    fun sleepPerformancePct(asleepMinutesPerNight: List<Double>, needMinutes: Double): Int {
        if (asleepMinutesPerNight.isEmpty() || needMinutes <= 0) return 0
        val avg = asleepMinutesPerNight.takeLast(7).map { (it / needMinutes).coerceAtMost(1.0) }.average()
        return (avg * 100).toInt().coerceIn(0, 100)
    }

    /** Sleep efficiency 0..100: asleep time as a share of time in bed. */
    fun sleepEfficiencyPct(asleepMinutes: List<Double>, inBedMinutes: List<Double>): Int {
        if (asleepMinutes.isEmpty() || asleepMinutes.size != inBedMinutes.size) return 0
        val pairs = asleepMinutes.zip(inBedMinutes).filter { it.second > 0 }
        if (pairs.isEmpty()) return 0
        val avg = pairs.map { (it.first / it.second).coerceIn(0.0, 1.0) }.average()
        return (avg * 100).toInt().coerceIn(0, 100)
    }

    /** Minutes spent in each HR zone (index 0..5) from (bpm, seconds) samples. */
    fun zoneMinutes(samples: List<Pair<Int, Double>>, maxHr: Int): DoubleArray {
        val out = DoubleArray(6)
        for ((bpm, seconds) in samples) {
            out[Scores.zone(bpm, maxHr)] += seconds / 60.0
        }
        return out
    }
}
