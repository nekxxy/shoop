package com.shoop.app.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.shoop.app.ui.screens.LiveScreen
import com.shoop.app.ui.screens.LiveUiState
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
                        restingHr = 54, rmssdMs = 52.0, asleepMinutes = 432.0,
                        sampleCount = 812,
                        recoverySeries = listOf(55f, 62f, 48f, 70f, 74f, 66f, 78f),
                    )
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
