package com.shoop.app

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.shoop.app.ble.BandClient
import com.shoop.app.ui.screens.LiveScreen
import com.shoop.app.ui.screens.LiveUiState
import com.shoop.app.ui.screens.SleepHistoryItem
import com.shoop.app.ui.screens.SleepScreen
import com.shoop.app.ui.screens.SleepUiState
import com.shoop.app.ui.screens.TodayScreen
import com.shoop.app.ui.screens.TodayScreenState
import com.shoop.app.ui.screens.TrendsScreen
import com.shoop.app.ui.screens.TrendsUiState
import com.shoop.app.ui.screens.formatNightDate
import com.shoop.app.ui.theme.ShoopTheme

private enum class Tab(val label: String) { LIVE("Live"), TODAY("Today"), SLEEP("Sleep"), TRENDS("Trends") }

@Composable
fun ShoopApp(viewModel: AppViewModel) {
    val live by viewModel.live.collectAsState()
    val today by viewModel.today.collectAsState()
    val days by viewModel.days.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val connection by viewModel.band.connection.collectAsState()
    val discovered by viewModel.band.discovered.collectAsState()

    var tab by remember { mutableIntStateOf(0) }
    val tabs = Tab.entries

    ShoopTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            label = { Text(t.label) },
                            icon = {
                                Icon(
                                    when (t) {
                                        Tab.LIVE -> Icons.Filled.Favorite
                                        Tab.TODAY -> Icons.Filled.Home
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
            androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
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
                Tab.TODAY -> TodayScreen(
                    TodayScreenState(today.strain, today.recovery, today.sleepScore, today.sampleCount, today.restingHr)
                )
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
