package com.shoop.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shoop.app.ble.BandClient
import com.shoop.app.data.Baseline
import com.shoop.app.data.DaySummary
import com.shoop.app.data.ActivityStore
import com.shoop.app.data.DemoSource
import com.shoop.app.data.HabitJournal
import com.shoop.app.data.ManualActivity
import com.shoop.app.data.HrSample
import com.shoop.app.data.LiveReading
import com.shoop.app.data.RecordingSession
import com.shoop.app.data.SessionStore
import com.shoop.app.hrv.HrvAnalyzer
import com.shoop.app.score.DashboardMetrics
import com.shoop.app.score.Scores
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** UI mode: live BLE band or built-in demo generator. */
enum class SourceMode { BAND, DEMO }

data class TodayUiState(
    val strain: Double,
    val recovery: Int?,
    val sleepScore: Int?,
    val sampleCount: Int,
    val restingHr: Int,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SessionStore(app)
    val band = BandClient(app)
    private val demo = DemoSource()
    private val hrv = HrvAnalyzer(windowSize = 60)

    private val _mode = MutableStateFlow(SourceMode.BAND)
    val mode: StateFlow<SourceMode> = _mode

    private val _live = MutableStateFlow(LiveReading(null, null, null, 0, null))
    val live: StateFlow<LiveReading> = _live

    private val _today = MutableStateFlow(TodayUiState(0.0, null, null, 0, 60))
    val today: StateFlow<TodayUiState> = _today

    private val _days = MutableStateFlow<List<DaySummary>>(emptyList())
    val days: StateFlow<List<DaySummary>> = _days

    private val _sessions = MutableStateFlow<List<RecordingSession>>(emptyList())
    val sessions: StateFlow<List<RecordingSession>> = _sessions

    private val activityStore = ActivityStore(app)

    private val _manualActivities = MutableStateFlow<List<ManualActivity>>(emptyList())
    val manualActivities: StateFlow<List<ManualActivity>> = _manualActivities

    private val _journal = MutableStateFlow(HabitJournal())
    val journal: StateFlow<HabitJournal> = _journal

    /** Label of the currently recording activity, null when idle. */
    private val _activeActivity = MutableStateFlow<String?>(null)
    val activeActivity: StateFlow<String?> = _activeActivity
    private var activeActivityStartIndex = -1

    private val _dashboard = MutableStateFlow(DashboardUiState())
    val dashboard: StateFlow<DashboardUiState> = _dashboard

    private var baseline = Baseline()
    private val todaySamples = mutableListOf<HrSample>()
    private var collectJob: Job? = null

    init {
        viewModelScope.launch {
            baseline = store.loadBaseline()
            _days.value = store.listDaySummaries()
            _sessions.value = store.listSessions()
            _manualActivities.value = activityStore.listActivities()
            _journal.value = activityStore.loadJournal()
            refreshToday()
            refreshDashboard()
        }
        // Wire the live pipeline once; which source feeds it depends on mode.
        collectJob = viewModelScope.launch {
            band.heartRate.collect { m -> if (_mode.value == SourceMode.BAND && m != null) onSample(m.heartRateBpm, m.rrIntervalsMs) }
        }
        viewModelScope.launch {
            demo.heartRate.collect { m -> if (_mode.value == SourceMode.DEMO && m != null) onSample(m.heartRateBpm, m.rrIntervalsMs) }
        }
        viewModelScope.launch {
            band.battery.collect { b -> if (_mode.value == SourceMode.BAND) _live.value = _live.value.copy(batteryPercent = b) }
        }
        viewModelScope.launch {
            demo.battery.collect { b -> if (_mode.value == SourceMode.DEMO) _live.value = _live.value.copy(batteryPercent = b) }
        }
        viewModelScope.launch {
            band.connection.collect { c ->
                val name = (c as? BandClient.ConnectionState.Streaming)?.deviceName
                    ?: (c as? BandClient.ConnectionState.Connected)?.deviceName
                _live.value = _live.value.copy(connectedDeviceName = name)
            }
        }
    }

    private fun onSample(bpm: Int, rrIntervals: List<Int>) {
        rrIntervals.forEach { hrv.addRrInterval(it) }
        val rmssd = hrv.rmssdMs()
        _live.value = _live.value.copy(
            bpm = bpm,
            rmssdMs = rmssd,
            zone = Scores.zone(bpm, baseline.maxHr),
        )
        todaySamples.add(HrSample(System.currentTimeMillis(), bpm, rrIntervals))
        refreshToday()
    }

    private fun refreshToday() {
        val strain = Scores.strain(todaySamples.map { it.bpm to 1.0 }, baseline.restingHr, baseline.maxHr)
        _today.value = _today.value.copy(
            strain = strain,
            sampleCount = todaySamples.size,
            restingHr = baseline.restingHr,
        )
    }

    fun setMode(mode: SourceMode) {
        if (_mode.value == mode) return
        _mode.value = mode
        hrv.clear()
        _live.value = LiveReading(null, null, null, 0, null)
        if (mode == SourceMode.DEMO) {
            band.disconnect()
            demo.start()
            seedDemoHistoryIfEmpty()
        } else {
            demo.stop()
        }
    }

    /** Populate one demo night + day so Sleep/Today/Trends have content to show. */
    private fun seedDemoHistoryIfEmpty() {
        viewModelScope.launch {
            if (store.listSessions().isNotEmpty()) return@launch
            val now = System.currentTimeMillis()
            val night = demo.syntheticNight(baseline.restingHr, now - 2 * 3600_000)
            store.saveSession(night)
            val dayStart = now - 16 * 3600_000
            store.saveSession(demo.syntheticDay(baseline.restingHr, dayStart, 14.0))
            _sessions.value = store.listSessions()
            computeSleepAndRecovery()
            refreshDashboard()
        }
    }

    /** Score last night's sleep + this morning's recovery from stored sessions. */
    fun computeSleepAndRecovery() {
        viewModelScope.launch {
            val night = _sessions.value.firstOrNull { it.label.contains("Sleep") }
                ?: _sessions.value.firstOrNull() ?: return@launch
            val sleepHrThreshold = baseline.restingHr + 8
            val asleep = night.samples.filter { it.bpm <= sleepHrThreshold }
            val asleepMinutes = asleep.size.toDouble() // 1 sample ~ 1 minute for stored sessions
            val restful = if (asleep.isEmpty()) 0.0
            else asleep.count { it.bpm <= baseline.restingHr + 5 }.toDouble() / asleep.size
            val sleepScore = Scores.sleep(asleepMinutes, baseline.sleepNeedMinutes, restful)

            val rr = night.samples.flatMap { it.rrMs }
            val nightRmssd = if (rr.size >= 3) {
                var s = 0.0; var n = 0
                for (i in 1 until rr.size) { val d = rr[i] - rr[i - 1]; s += d.toDouble() * d; n++ }
                kotlin.math.sqrt(s / n)
            } else baseline.rmssdMs
            val recovery = Scores.recovery(nightRmssd, night.minHr.coerceAtLeast(35), baseline.rmssdMs, baseline.restingHr)

            _today.value = _today.value.copy(sleepScore = sleepScore, recovery = recovery)

            val summary = DaySummary(
                date = SessionStore.todayString(),
                strain = _today.value.strain,
                recovery = recovery,
                sleepScore = sleepScore,
                asleepMinutes = asleepMinutes,
                restingHr = night.minHr,
                rmssdMs = nightRmssd,
                sampleCount = night.samples.size,
            )
            store.saveDaySummary(summary)
            _days.value = store.listDaySummaries()
            refreshDashboard()
        }
    }

    /** End the current stretch of samples and store it as a session. */
    fun saveCurrentSession(label: String) {
        if (todaySamples.size < 10) return
        val session = RecordingSession(
            id = UUID.randomUUID().toString(),
            startedEpochMs = todaySamples.first().epochMs,
            endedEpochMs = todaySamples.last().epochMs,
            label = label,
            samples = todaySamples.toList(),
        )
        viewModelScope.launch {
            store.saveSession(session)
            _sessions.value = store.listSessions()
            refreshDashboard()
        }
    }

    fun updateBaseline(newBaseline: Baseline) {
        baseline = newBaseline
        viewModelScope.launch { store.saveBaseline(newBaseline) }
    }

    fun currentBaseline(): Baseline = baseline

    // ---- manual activities ----

    fun addManualActivity(label: String, startedEpochMs: Long, durationMinutes: Int) {
        if (label.isBlank() || durationMinutes <= 0) return
        val end = startedEpochMs + durationMinutes * 60_000L
        val overlapping = todaySamples.filter { it.epochMs in startedEpochMs..end }
        val strain = if (overlapping.size >= 5)
            Scores.strain(overlapping.map { it.bpm to 1.0 }, baseline.restingHr, baseline.maxHr) else null
        val entry = ManualActivity(
            id = UUID.randomUUID().toString(),
            label = label.trim(),
            startedEpochMs = startedEpochMs,
            durationMinutes = durationMinutes,
            avgHr = if (overlapping.isEmpty()) null else overlapping.map { it.bpm }.average().toInt(),
            strain = strain,
        )
        _manualActivities.value = (_manualActivities.value + entry)
            .sortedByDescending { it.startedEpochMs }
        activityStore.saveActivities(_manualActivities.value)
    }

    /** Begin recording an activity against the live HR stream. */
    fun startActivity(label: String) {
        if (_activeActivity.value != null) return
        activeActivityStartIndex = todaySamples.size
        _activeActivity.value = label
    }

    /** Stop the active activity and store it (and a session) if it captured data. */
    fun stopActivity() {
        val label = _activeActivity.value ?: return
        _activeActivity.value = null
        val slice = todaySamples.drop(activeActivityStartIndex.coerceAtLeast(0))
        activeActivityStartIndex = -1
        if (slice.size < 5) return
        val minutes = ((slice.last().epochMs - slice.first().epochMs) / 60000L).toInt().coerceAtLeast(1)
        val strain = Scores.strain(slice.map { it.bpm to 1.0 }, baseline.restingHr, baseline.maxHr)
        val entry = ManualActivity(
            id = UUID.randomUUID().toString(),
            label = label,
            startedEpochMs = slice.first().epochMs,
            durationMinutes = minutes,
            avgHr = slice.map { it.bpm }.average().toInt(),
            strain = strain,
        )
        _manualActivities.value = (_manualActivities.value + entry).sortedByDescending { it.startedEpochMs }
        activityStore.saveActivities(_manualActivities.value)
    }

    // ---- habit journal ----

    fun toggleHabit(name: String) {
        val j = _journal.value
        val today = SessionStore.todayString()
        val todays = j.checks[today].orEmpty()
        val updated = if (name in todays) todays - name else todays + name
        val next = j.copy(checks = j.checks + (today to updated))
        _journal.value = next
        activityStore.saveJournal(next)
    }

    // ---- dashboard ----

    private fun refreshDashboard() {
        val nights = _sessions.value.filter { it.label.contains("Sleep") }.take(7)
        val sleepThreshold = baseline.restingHr + 8
        val asleepPerNight = nights.map { n -> n.samples.count { it.bpm <= sleepThreshold }.toDouble() }
        val inBedPerNight = nights.map { it.durationMinutes }
        val bedtimes = nights.map {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.startedEpochMs }
            // minutes after 18:00, wrapping past midnight so 23:00 and 01:00 compare sanely
            ((cal.get(java.util.Calendar.HOUR_OF_DAY) - 18 + 24) % 24) * 60.0 + cal.get(java.util.Calendar.MINUTE)
        }

        val weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
        val weekSamples = _sessions.value.filter { it.endedEpochMs >= weekAgo }
            .flatMap { sess ->
                sess.samples.zipWithNext { a, b ->
                    a.bpm to ((b.epochMs - a.epochMs).coerceAtMost(60_000L) / 1000.0)
                }
            } + todaySamples.zipWithNext { a, b ->
                a.bpm to ((b.epochMs - a.epochMs).coerceAtMost(60_000L) / 1000.0)
            }
        val zones = DashboardMetrics.zoneMinutes(weekSamples, baseline.maxHr)

        val recentRhr = _days.value.mapNotNull { it.restingHr }.take(7)
        val latestRhr = recentRhr.firstOrNull() ?: baseline.restingHr
        val rhrDelta = if (recentRhr.size >= 2) latestRhr - (recentRhr.drop(1).average().toInt()) else 0

        _dashboard.value = DashboardUiState(
            sleepDebtMinutes = DashboardMetrics.sleepDebtMinutes(asleepPerNight, baseline.sleepNeedMinutes),
            sleepConsistencyPct = DashboardMetrics.sleepConsistencyPct(bedtimes),
            sleepPerformancePct = DashboardMetrics.sleepPerformancePct(asleepPerNight, baseline.sleepNeedMinutes),
            sleepEfficiencyPct = DashboardMetrics.sleepEfficiencyPct(asleepPerNight, inBedPerNight),
            nightsRecorded = nights.size,
            restingHr = latestRhr,
            restingHrDelta = rhrDelta,
            zones13WeeklyMinutes = zones[1] + zones[2] + zones[3],
            strainSeries = _days.value.mapNotNull { it.strain.toFloat() }.reversed(),
            recoverySeries = _days.value.mapNotNull { it.recovery?.toFloat() }.reversed(),
        )
    }

    override fun onCleared() {
        collectJob?.cancel()
        demo.stop()
        band.disconnect()
    }
}

/** All dashboard card values, computed from recorded history only. */
data class DashboardUiState(
    val sleepDebtMinutes: Double = 0.0,
    val sleepConsistencyPct: Int = 0,
    val sleepPerformancePct: Int = 0,
    val sleepEfficiencyPct: Int = 0,
    val nightsRecorded: Int = 0,
    val restingHr: Int = 60,
    val restingHrDelta: Int = 0,
    val zones13WeeklyMinutes: Double = 0.0,
    val strainSeries: List<Float> = emptyList(),
    val recoverySeries: List<Float> = emptyList(),
)
