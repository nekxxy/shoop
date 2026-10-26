package com.shoop.app.data

import kotlinx.serialization.Serializable

/** One recorded heart-rate sample. */
@Serializable
data class HrSample(
    val epochMs: Long,
    val bpm: Int,
    val rrMs: List<Int> = emptyList(),
)

/** A continuous recording stretch (e.g. one workout or one night). */
@Serializable
data class RecordingSession(
    val id: String,
    val startedEpochMs: Long,
    val endedEpochMs: Long,
    val label: String,
    val samples: List<HrSample>,
) {
    val durationMinutes: Double get() = (endedEpochMs - startedEpochMs) / 60000.0
    val averageHr: Int get() = if (samples.isEmpty()) 0 else samples.map { it.bpm }.average().toInt()
    val minHr: Int get() = samples.minOfOrNull { it.bpm } ?: 0
    val maxHr: Int get() = samples.maxOfOrNull { it.bpm } ?: 0
}

/** Per-day rollup stored for trends. */
@Serializable
data class DaySummary(
    val date: String, // ISO yyyy-mm-dd, device local
    val strain: Double,
    val recovery: Int?,
    val sleepScore: Int?,
    val asleepMinutes: Double,
    val restingHr: Int?,
    val rmssdMs: Double?,
    val sampleCount: Int,
)

/** Personal baselines, learned from stored history. */
@Serializable
data class Baseline(
    val restingHr: Int = 60,
    val rmssdMs: Double = 40.0,
    val maxHr: Int = 190,
    val sleepNeedMinutes: Double = 480.0,
)

/** Live reading shown on the Live tab. */
data class LiveReading(
    val bpm: Int?,
    val rmssdMs: Double?,
    val batteryPercent: Int?,
    val zone: Int,
    val connectedDeviceName: String?,
)

/** A manually logged activity (no fake sensor data - time and label only). */
@Serializable
data class ManualActivity(
    val id: String,
    val label: String,
    val startedEpochMs: Long,
    val durationMinutes: Int,
    val avgHr: Int? = null,   // filled when HR samples overlap the window
    val strain: Double? = null,
)

/** Habit journal: named habits with per-date checkmarks (ISO date strings). */
@Serializable
data class HabitJournal(
    val habits: List<String> = listOf("Workout", "No late caffeine", "Reading"),
    val checks: Map<String, List<String>> = emptyMap(), // date -> checked habit names
)
