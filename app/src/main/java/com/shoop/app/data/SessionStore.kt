package com.shoop.app.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * On-device history. Recordings are JSON files under filesDir/sessions/;
 * day rollups under filesDir/days/; baselines in DataStore. No network, no
 * account, nothing leaves the phone.
 */
class SessionStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private val sessionsDir: File get() = File(context.filesDir, "sessions").apply { mkdirs() }
    private val daysDir: File get() = File(context.filesDir, "days").apply { mkdirs() }

    // ---- recordings ----

    fun saveSession(session: RecordingSession) {
        File(sessionsDir, "${session.id}.json").writeText(json.encodeToString(RecordingSession.serializer(), session))
    }

    fun listSessions(): List<RecordingSession> =
        sessionsDir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { runCatching { json.decodeFromString(RecordingSession.serializer(), it.readText()) }.getOrNull() }
            ?.sortedByDescending { it.startedEpochMs }
            ?: emptyList()

    // ---- day summaries ----

    fun saveDaySummary(summary: DaySummary) {
        File(daysDir, "${summary.date}.json").writeText(json.encodeToString(DaySummary.serializer(), summary))
    }

    fun listDaySummaries(limit: Int = 30): List<DaySummary> =
        daysDir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { runCatching { json.decodeFromString(DaySummary.serializer(), it.readText()) }.getOrNull() }
            ?.sortedByDescending { it.date }
            ?.take(limit)
            ?: emptyList()

    // ---- baseline (DataStore) ----

    private val keyRestingHr = intPreferencesKey("baseline_resting_hr")
    private val keyRmssd = doublePreferencesKey("baseline_rmssd_ms")
    private val keyMaxHr = intPreferencesKey("baseline_max_hr")
    private val keySleepNeed = doublePreferencesKey("baseline_sleep_need_min")

    private val ds get() = context.baselineDataStore

    suspend fun loadBaseline(): Baseline = ds.data.map { p ->
        Baseline(
            restingHr = p[keyRestingHr] ?: 60,
            rmssdMs = p[keyRmssd] ?: 40.0,
            maxHr = p[keyMaxHr] ?: 190,
            sleepNeedMinutes = p[keySleepNeed] ?: 480.0,
        )
    }.first()

    suspend fun saveBaseline(b: Baseline) {
        ds.edit { p ->
            p[keyRestingHr] = b.restingHr
            p[keyRmssd] = b.rmssdMs
            p[keyMaxHr] = b.maxHr
            p[keySleepNeed] = b.sleepNeedMinutes
        }
    }

    companion object {
        fun todayString(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}
