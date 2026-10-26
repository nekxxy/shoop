package com.shoop.app.data

import com.shoop.app.ble.HeartRateParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/**
 * Simulated heart-rate source for demo mode: exercises the exact same parsing,
 * HRV and scoring pipeline as a real band, without hardware. Clearly labelled
 * as demo data in the UI; nothing here pretends to be a sensor.
 */
class DemoSource {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var running = false
    private var tick = 0

    private val _heartRate = MutableStateFlow<HeartRateParser.Measurement?>(null)
    val heartRate: StateFlow<HeartRateParser.Measurement?> = _heartRate

    private val _battery = MutableStateFlow<Int?>(null)
    val battery: StateFlow<Int?> = _battery

    fun start() {
        if (running) return
        running = true
        _battery.value = 87
        scope.launch {
            while (isActive && running) {
                tick++
                // Slow breathing wave + drift + noise around a base of 62 bpm.
                val base = 62.0 + 6.0 * sin(tick / 45.0) + 2.0 * sin(tick / 7.0)
                val bpm = (base + Random.nextDouble(-1.5, 1.5)).toInt().coerceIn(45, 110)
                // Plausible RR intervals around the beat with natural jitter.
                val rrMean = 60000.0 / bpm
                val rr = List(2) { (rrMean / 2 + Random.nextDouble(-28.0, 28.0)).toInt().coerceIn(300, 1400) }
                _heartRate.value = HeartRateParser.Measurement(bpm, rr)
                delay(1000)
            }
        }
    }

    fun stop() {
        running = false
        _heartRate.value = null
    }

    /** A synthetic 7.2-hour night of 1-minute HR samples for exercising sleep/scoring code and screenshots. */
    fun syntheticNight(restingHr: Int, endEpochMs: Long): RecordingSession {
        val start = endEpochMs - (7.2 * 60 * 60 * 1000).toLong()
        val samples = mutableListOf<HrSample>()
        var t = start
        var i = 0
        while (t < endEpochMs) {
            val phase = (t - start).toDouble() / (endEpochMs - start)
            val cycles = 3.0 * sin(phase * Math.PI * 4.6) // ~4-5 sleep cycles
            val bpm = (restingHr - 4 + cycles + Random.nextDouble(-1.2, 1.2)).toInt().coerceAtLeast(38)
            samples.add(HrSample(t, bpm, listOf(60000 / bpm + Random.nextInt(-30, 30))))
            t += 60_000
            i++
        }
        return RecordingSession(
            id = "demo-night-$endEpochMs",
            startedEpochMs = start,
            endedEpochMs = endEpochMs,
            label = "Sleep (demo)",
            samples = samples,
        )
    }

    /** A synthetic day of 1-minute HR samples for strain/trends demos. */
    fun syntheticDay(restingHr: Int, startEpochMs: Long, hours: Double): RecordingSession {
        val end = startEpochMs + (hours * 3600_000).toLong()
        val samples = mutableListOf<HrSample>()
        var t = startEpochMs
        while (t < end) {
            val hour = (t - startEpochMs) / 3600_000.0
            val activity = when {
                hour < 7 -> 0.0                       // asleep-ish
                hour in 12.0..13.0 -> 0.55            // workout block
                hour in 18.0..18.5 -> 0.35            // evening walk
                else -> 0.12                          // desk day
            }
            val bpm = (restingHr + activity * 110 + Random.nextDouble(-2.0, 2.0)).toInt()
            samples.add(HrSample(t, bpm, listOf(60000 / bpm + Random.nextInt(-25, 25))))
            t += 60_000
        }
        return RecordingSession("demo-day-$startEpochMs", startEpochMs, end, "Day (demo)", samples)
    }
}
