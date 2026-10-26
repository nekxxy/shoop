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
import com.shoop.app.ui.screens.OverviewScreen
import com.shoop.app.ui.screens.OverviewUiState
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
                    Tab.OVERVIEW -> OverviewScreen(
                        OverviewUiState(
                            dateText = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
                            strain = today.strain,
                            recovery = today.recovery,
                            sleepScore = today.sleepScore,
                            restingHr = today.restingHr,
                            rmssdMs = live.rmssdMs ?: days.firstOrNull()?.rmssdMs,
                            asleepMinutes = days.firstOrNull()?.asleepMinutes,
                            sampleCount = today.sampleCount,
                            recoverySeries = days.mapNotNull { it.recovery?.toFloat() }.reversed(),
                        )
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
