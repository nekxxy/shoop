package com.shoop.app.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.shoop.app.ui.screens.LiveScreen
import com.shoop.app.ui.screens.LiveUiState
import androidx.compose.ui.graphics.Color
import com.shoop.app.ui.screens.ActivityRowData
import com.shoop.app.ui.screens.DashboardCardState
import com.shoop.app.ui.screens.JournalUiState
import com.shoop.app.ui.screens.OverviewScreen
import com.shoop.app.ui.screens.OverviewUiState
import com.shoop.app.ui.screens.SleepHistoryItem
import com.shoop.app.ui.screens.SleepScreen
import com.shoop.app.ui.screens.SleepUiState
import com.shoop.app.ui.screens.TrendsScreen
import com.shoop.app.ui.screens.TrendsUiState
import com.shoop.app.ui.theme.ShoopTheme
import org.junit.Rule
import org.junit.Test

/**
 * Real UI screenshots rendered through Android's own layoutlib (Paparazzi) -
 * the same rendering pipeline Android Studio previews use. No emulator needed.
 */
class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    @Test
    fun overview() {
        paparazzi.snapshot {
            ShoopTheme {
                OverviewScreen(
                    OverviewUiState(
                        dateText = "Sunday, 26 October",
                        strain = 11.4, recovery = 78, sleepScore = 84,
                        restingHr = 54, rmssdMs = 52.0,
                        activities = listOf(
                            ActivityRowData("1", "Sleep", "6:38", "0:02 - 7:17", Color(0xFF5BA8F5), null),
                            ActivityRowData("2", "Strength", "0:47", "18:04 - 18:51", Color(0xFFF5C04E), "3.9"),
                        ),
                        activeActivityLabel = null,
                        journal = JournalUiState(
                            habits = listOf("Workout", "No late caffeine", "Reading"),
                            todayChecked = listOf("Workout"),
                            week = listOf("T" to 1f, "W" to 1f, "T" to 0.66f, "F" to 1f, "S" to 0.33f, "S" to 1f, "M" to 0.33f),
                            todayFraction = 0.33f,
                        ),
                        dashboard = DashboardCardState(
                            sleepDebtMinutes = 59.0, sleepConsistencyPct = 57,
                            sleepPerformancePct = 79, sleepEfficiencyPct = 92,
                            nightsRecorded = 5, restingHr = 54, restingHrDelta = -2,
                            zones13WeeklyMinutes = 200.0,
                            strainSeries = listOf(8f, 10f, 6f, 12f, 14f, 9f, 11.4f),
                            recoverySeries = listOf(55f, 62f, 48f, 70f, 74f, 66f, 78f),
                        ),
                    ),
                    onAddActivity = { _, _ -> }, onStartActivity = {}, onStopActivity = {}, onToggleHabit = {},
                )
            }
        }
    }

    @Test
    fun live() {
        paparazzi.snapshot {
            ShoopTheme {
                LiveScreen(
                    LiveUiState(
                        bpm = 64, rmssdMs = 52.0, batteryPercent = 87, zone = 1,
                        connectionText = "Streaming from WHOOP 5.0 (demo)",
                        isDemo = true, scanning = false, deviceNames = emptyList(),
                    ),
                    onScan = {}, onStop = {}, onConnectFirst = {}, onToggleDemo = {},
                )
            }
        }
    }

    @Test
    fun sleep() {
        paparazzi.snapshot {
            ShoopTheme {
                SleepScreen(
                    SleepUiState(
                        lastScore = 84, lastAsleepMinutes = 432.0, lastRestingHr = 51,
                        history = listOf(
                            SleepHistoryItem("Sleep", "Sun 26 Oct, 22:58", 432.0, 51, 56),
                            SleepHistoryItem("Sleep", "Sat 25 Oct, 23:31", 398.0, 53, 58),
                        ),
                    )
                )
            }
        }
    }

    @Test
    fun trends() {
        paparazzi.snapshot {
            ShoopTheme {
                TrendsScreen(
                    TrendsUiState(
                        rmssdSeries = listOf(38f, 41f, 37f, 44f, 47f, 43f, 52f),
                        restingHrSeries = listOf(58f, 57f, 59f, 56f, 55f, 56f, 54f),
                        recoverySeries = listOf(55f, 62f, 48f, 70f, 74f, 66f, 78f),
                        dayCount = 7,
                    )
                )
            }
        }
    }
}
