package com.shoop.app.score

import kotlin.math.exp
import kotlin.math.ln

/**
 * Shoop's local scoring. All scores are computed on-device from recorded heart
 * rate; nothing leaves the phone. These are original, transparent formulas,
 * deliberately documented in docs/SCORING.md.
 */
object Scores {

    /**
     * Day strain, 0..21. Cardiovascular load accumulates with time spent at a
     * fraction of heart-rate reserve; higher intensities weigh
     * super-linearly. A resting day stays near 0, an all-out multi-hour day
     * approaches 21.
     *
     * @param hrSamples (bpm, durationSeconds) pairs for the day
     * @param restingHr personal resting heart rate
     * @param maxHr personal max heart rate (default 220 - 30)
     */
    fun strain(
        hrSamples: List<Pair<Int, Double>>,
        restingHr: Int,
        maxHr: Int = 190,
    ): Double {
        val reserve = (maxHr - restingHr).coerceAtLeast(1)
        var load = 0.0
        for ((bpm, seconds) in hrSamples) {
            val intensity = ((bpm - restingHr).toDouble() / reserve).coerceIn(0.0, 1.2)
            if (intensity <= 0.0) continue
            load += Math.pow(intensity, 1.3) * (seconds / 60.0)
        }
        return (21.0 * (1.0 - exp(-load / 180.0))).coerceIn(0.0, 21.0)
    }

    /**
     * Recovery, 0..100, from this morning's HRV and resting HR against the
     * personal baseline (rolling 14-day typical values).
     * Green >= 67, yellow 34..66, red < 34.
     */
    fun recovery(rmssdMs: Double, restingHr: Int, baselineRmssdMs: Double, baselineRestingHr: Int): Int {
        if (rmssdMs <= 0 || baselineRmssdMs <= 0) return 0
        val hrvRatio = ln(rmssdMs) / ln(baselineRmssdMs.coerceAtLeast(1.0))
        val hrvScore = ((hrvRatio - 0.85) / 0.30).coerceIn(0.0, 1.0) // 0.85x .. 1.15x maps to 0..1
        val rhrDelta = restingHr - baselineRestingHr
        val rhrScore = (1.0 - (rhrDelta + 3.0) / 12.0).coerceIn(0.0, 1.0) // +9 bpm above baseline -> 0
        return (100.0 * (0.7 * hrvScore + 0.3 * rhrScore)).toInt().coerceIn(0, 100)
    }

    /**
     * Sleep score, 0..100, from one night's recording.
     * @param asleepMinutes time spent below the sleep HR threshold
     * @param targetMinutes personal sleep need (default 8 h)
     * @param restfulFraction fraction of asleep time with HR below resting + 5
     */
    fun sleep(asleepMinutes: Double, targetMinutes: Double = 480.0, restfulFraction: Double): Int {
        val durationScore = (asleepMinutes / targetMinutes).coerceIn(0.0, 1.0)
        return (100.0 * (0.65 * durationScore + 0.35 * restfulFraction.coerceIn(0.0, 1.0)))
            .toInt().coerceIn(0, 100)
    }

    /** Heart-rate zone (1..5) for a bpm given max HR; 0 when below zone 1. */
    fun zone(bpm: Int, maxHr: Int = 190): Int {
        val pct = bpm.toDouble() / maxHr
        return when {
            pct < 0.50 -> 0
            pct < 0.60 -> 1
            pct < 0.70 -> 2
            pct < 0.80 -> 3
            pct < 0.90 -> 4
            else -> 5
        }
    }

    fun recoveryLabel(score: Int): String = when {
        score >= 67 -> "Green"
        score >= 34 -> "Yellow"
        else -> "Red"
    }
}
