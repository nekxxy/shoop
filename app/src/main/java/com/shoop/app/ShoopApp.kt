package com.shoop.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.shoop.app.ble.BandClient
import com.shoop.app.ui.screens.LiveScreen
import com.shoop.app.ui.screens.LiveUiState
import com.shoop.app.ui.screens.ActivityRowData
import com.shoop.app.ui.screens.DashboardCardState
import com.shoop.app.ui.screens.JournalUiState
import com.shoop.app.ui.screens.OverviewScreen
import com.shoop.app.ui.screens.OverviewUiState
import com.shoop.app.ui.theme.ShoopBlue
import com.shoop.app.ui.theme.ShoopTeal
import com.shoop.app.ui.theme.ShoopYellow
import com.shoop.app.ui.screens.SleepHistoryItem
import com.shoop.app.ui.screens.SleepScreen
import com.shoop.app.ui.screens.SleepUiState
import com.shoop.app.ui.screens.TrendsScreen
import com.shoop.app.ui.screens.TrendsUiState
import com.shoop.app.ui.screens.formatNightDate
import com.shoop.app.ui.theme.ShoopBackground
import com.shoop.app.ui.theme.ShoopGreen
import com.shoop.app.ui.theme.ShoopMuted
import com.shoop.app.ui.theme.ShoopTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Tab(val label: String) { LIVE("Live"), OVERVIEW("My Day"), SLEEP("Sleep"), TRENDS("Trends") }

@Composable
fun ShoopApp(viewModel: AppViewModel) {
    val live by viewModel.live.collectAsState()
    val today by viewModel.today.collectAsState()
    val days by viewModel.days.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val connection by viewModel.band.connection.collectAsState()
    val discovered by viewModel.band.discovered.collectAsState()
    val manualActivities by viewModel.manualActivities.collectAsState()
    val journal by viewModel.journal.collectAsState()
    val activeActivity by viewModel.activeActivity.collectAsState()
    val dashboard by viewModel.dashboard.collectAsState()

    var tab by remember { mutableIntStateOf(1) } // open on My Day
    val tabs = Tab.entries

    ShoopTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ShoopBackground,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF121514)) {
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            label = { Text(t.label, fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ShoopGreen,
                                selectedTextColor = ShoopGreen,
                                unselectedIconColor = ShoopMuted,
                                unselectedTextColor = ShoopMuted,
                                indicatorColor = Color(0xFF1E2A24),
                            ),
                            icon = {
                                Icon(
                                    when (t) {
                                        Tab.LIVE -> Icons.Filled.Favorite
                                        Tab.OVERVIEW -> Icons.Filled.Home
                                        Tab.SLEEP -> Icons.Filled.Star
                                        Tab.TRENDS -> Icons.Filled.DateRange
                                    },
                                    contentDescription = t.label,
                                )
                            },
                        )
                    }
                }
            },
        ) { padding ->
            val connText = when (val c = connection) {
                is BandClient.ConnectionState.Idle -> if (mode == SourceMode.DEMO) "Demo generator running" else "Not connected"
                is BandClient.ConnectionState.Scanning -> "Scanning for bands..."
                is BandClient.ConnectionState.Connecting -> "Connecting..."
                is BandClient.ConnectionState.Connected -> "Connected to ${c.deviceName}"
                is BandClient.ConnectionState.Streaming -> "Streaming from ${c.deviceName}"
                is BandClient.ConnectionState.Error -> c.message
            }
            Box(Modifier.padding(padding)) {
                when (tabs[tab]) {
                    Tab.LIVE -> LiveScreen(
                        LiveUiState(
                            bpm = live.bpm,
                            rmssdMs = live.rmssdMs,
                            batteryPercent = live.batteryPercent,
                            zone = live.zone,
                            connectionText = connText,
                            isDemo = mode == SourceMode.DEMO,
                            scanning = connection == BandClient.ConnectionState.Scanning,
                            deviceNames = discovered.map { it.name },
                        ),
                        onScan = { viewModel.band.startScan() },
                        onStop = { viewModel.band.disconnect(); viewModel.band.stopScan() },
                        onConnectFirst = { discovered.firstOrNull()?.let { viewModel.band.connect(it.address) } },
                        onToggleDemo = {
                            viewModel.setMode(if (mode == SourceMode.DEMO) SourceMode.BAND else SourceMode.DEMO)
                        },
                    )
                    Tab.OVERVIEW -> {
                        val timeFmt = SimpleDateFormat("H:mm", Locale.getDefault())
                        val durFmt = { ms: Long ->
                            val mins = (ms / 60000L)
                            "%d:%02d".format(mins / 60, mins % 60)
                        }
                        val startOfToday = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val sessionRows = sessions.filter { it.endedEpochMs >= startOfToday || it.label.contains("Sleep") }
                            .sortedByDescending { it.startedEpochMs }
                            .map { sess ->
                                ActivityRowData(
                                    id = sess.id,
                                    label = sess.label,
                                    durationText = durFmt(sess.endedEpochMs - sess.startedEpochMs),
                                    timeRangeText = "${timeFmt.format(Date(sess.startedEpochMs))} - ${timeFmt.format(Date(sess.endedEpochMs))}",
                                    accent = if (sess.label.contains("Sleep")) ShoopBlue else ShoopTeal,
                                    strainText = null,
                                )
                            }
                        val manualRows = manualActivities.filter { it.startedEpochMs >= startOfToday }.map { a ->
                            ActivityRowData(
                                id = a.id,
                                label = a.label,
                                durationText = "%d:%02d".format(a.durationMinutes / 60, a.durationMinutes % 60),
                                timeRangeText = "${timeFmt.format(Date(a.startedEpochMs))} - ${timeFmt.format(Date(a.startedEpochMs + a.durationMinutes * 60000L))}",
                                accent = ShoopYellow,
                                strainText = a.strain?.let { "%.1f".format(it) },
                            )
                        }
                        val week = (6 downTo 0).map { back ->
                            val date = java.time.LocalDate.now().minusDays(back.toLong())
                            val key = date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                            val frac = if (journal.habits.isEmpty()) 0f
                            else journal.checks[key].orEmpty().size.toFloat() / journal.habits.size
                            date.dayOfWeek.name.first().toString() to frac
                        }
                        OverviewScreen(
                            OverviewUiState(
                                dateText = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
                                strain = today.strain,
                                recovery = today.recovery,
                                sleepScore = today.sleepScore,
                                restingHr = today.restingHr,
                                rmssdMs = live.rmssdMs ?: days.firstOrNull()?.rmssdMs,
                                activities = (manualRows + sessionRows).sortedByDescending { it.timeRangeText }.take(8),
                                activeActivityLabel = activeActivity,
                                journal = JournalUiState(
                                    habits = journal.habits,
                                    todayChecked = journal.checks[java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)].orEmpty(),
                                    week = week,
                                    todayFraction = week.lastOrNull()?.second ?: 0f,
                                ),
                                dashboard = DashboardCardState(
                                    sleepDebtMinutes = dashboard.sleepDebtMinutes,
                                    sleepConsistencyPct = dashboard.sleepConsistencyPct,
                                    sleepPerformancePct = dashboard.sleepPerformancePct,
                                    sleepEfficiencyPct = dashboard.sleepEfficiencyPct,
                                    nightsRecorded = dashboard.nightsRecorded,
                                    restingHr = dashboard.restingHr,
                                    restingHrDelta = dashboard.restingHrDelta,
                                    zones13WeeklyMinutes = dashboard.zones13WeeklyMinutes,
                                    strainSeries = dashboard.strainSeries,
                                    recoverySeries = dashboard.recoverySeries,
                                ),
                            ),
                            onAddActivity = { label, mins ->
                                viewModel.addManualActivity(label, System.currentTimeMillis() - mins * 60000L, mins)
                            },
                            onStartActivity = { viewModel.startActivity("Workout") },
                            onStopActivity = { viewModel.stopActivity() },
                            onToggleHabit = { viewModel.toggleHabit(it) },
                        )
                    }
                    Tab.SLEEP -> SleepScreen(
                        SleepUiState(
                            lastScore = today.sleepScore,
                            lastAsleepMinutes = days.firstOrNull()?.asleepMinutes,
                            lastRestingHr = days.firstOrNull()?.restingHr,
                            history = sessions.filter { it.label.contains("Sleep") }.map {
                                SleepHistoryItem(it.label, formatNightDate(it.startedEpochMs), it.durationMinutes, it.minHr, it.averageHr)
                            },
                        )
                    )
                    Tab.TRENDS -> TrendsScreen(
                        TrendsUiState(
                            rmssdSeries = days.mapNotNull { it.rmssdMs?.toFloat() }.reversed(),
                            restingHrSeries = days.mapNotNull { it.restingHr?.toFloat() }.reversed(),
                            recoverySeries = days.mapNotNull { it.recovery?.toFloat() }.reversed(),
                            dayCount = days.size,
                        )
                    )
                }
            }
        }
    }
}
