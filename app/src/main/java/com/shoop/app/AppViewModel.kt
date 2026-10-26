package com.shoop.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shoop.app.ble.BandClient
import com.shoop.app.data.Baseline
import com.shoop.app.data.DaySummary
import com.shoop.app.data.DemoSource
import com.shoop.app.data.HrSample
import com.shoop.app.data.LiveReading
import com.shoop.app.data.RecordingSession
import com.shoop.app.data.SessionStore
import com.shoop.app.hrv.HrvAnalyzer
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

    private var baseline = Baseline()
    private val todaySamples = mutableListOf<HrSample>()
    private var collectJob: Job? = null

    init {
        viewModelScope.launch {
            baseline = store.loadBaseline()
            _days.value = store.listDaySummaries()
            _sessions.value = store.listSessions()
            refreshToday()
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
        }
    }

    fun updateBaseline(newBaseline: Baseline) {
        baseline = newBaseline
        viewModelScope.launch { store.saveBaseline(newBaseline) }
    }

    fun currentBaseline(): Baseline = baseline

    override fun onCleared() {
        collectJob?.cancel()
        demo.stop()
        band.disconnect()
    }
}
