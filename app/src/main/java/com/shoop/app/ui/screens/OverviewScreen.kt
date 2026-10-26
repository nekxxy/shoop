package com.shoop.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoop.app.score.Scores
import com.shoop.app.ui.components.HealthCell
import com.shoop.app.ui.components.MetricRing
import com.shoop.app.ui.components.SectionHeader
import com.shoop.app.ui.components.SemiGauge
import com.shoop.app.ui.components.ShoopCardBox
import com.shoop.app.ui.components.TrendChart
import com.shoop.app.ui.theme.ShoopBlue
import com.shoop.app.ui.theme.ShoopGreen
import com.shoop.app.ui.theme.ShoopMuted
import com.shoop.app.ui.theme.ShoopTeal
import com.shoop.app.ui.theme.ShoopYellow
import com.shoop.app.ui.theme.recoveryColor

/** WHOOP-style "My Day" overview: score cards, health monitor grid, trends. */
data class OverviewUiState(
    val dateText: String,
    val strain: Double,
    val recovery: Int?,
    val sleepScore: Int?,
    val restingHr: Int,
    val rmssdMs: Double?,
    val asleepMinutes: Double?,
    val sampleCount: Int,
    val recoverySeries: List<Float>,
)

@Composable
fun OverviewScreen(state: OverviewUiState) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text("My Day", color = ShoopMuted, fontSize = 13.sp)
        Text(state.dateText, color = ShoopMuted, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))

        // Score cards: sleep ring + recovery gauge + strain ring
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ShoopCardBox(Modifier.weight(1f)) {
                Column {
                    MetricRing(
                        fraction = (state.sleepScore ?: 0) / 100f,
                        valueText = state.sleepScore?.let { "$it%" } ?: "--",
                        label = "Sleep",
                        color = ShoopBlue,
                        size = 96.dp,
                    )
                }
            }
            ShoopCardBox(Modifier.weight(1f)) {
                SemiGauge(
                    fraction = (state.recovery ?: 0) / 100f,
                    valueText = state.recovery?.let { "$it%" } ?: "--",
                    label = "Recovery",
                    color = state.recovery?.let { recoveryColor(it) } ?: ShoopMuted,
                    subText = state.recovery?.let { Scores.recoveryLabel(it) },
                )
            }
            ShoopCardBox(Modifier.weight(1f)) {
                MetricRing(
                    fraction = (state.strain / 21.0).toFloat(),
                    valueText = "%.1f".format(state.strain),
                    label = "Strain",
                    color = ShoopTeal,
                    size = 96.dp,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionHeader("Health monitor")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HealthCell(Icons.Filled.Favorite, "Resting HR", "${state.restingHr} bpm", Modifier.weight(1f), ShoopBlue)
            HealthCell(Icons.Filled.Star, "HRV (RMSSD)", state.rmssdMs?.let { "%.0f ms".format(it) } ?: "--", Modifier.weight(1f), ShoopGreen)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            // Not derivable from the WHOOP BLE broadcast: shown honestly as unavailable.
            HealthCell(null, "SpO2", "", Modifier.weight(1f), available = false)
            HealthCell(null, "Resp. rate", "", Modifier.weight(1f), available = false)
        }

        Spacer(Modifier.height(14.dp))
        SectionHeader("Recovery trend")
        ShoopCardBox(Modifier.fillMaxWidth()) {
            if (state.recoverySeries.size >= 2) {
                TrendChart(state.recoverySeries, ShoopGreen)
            } else {
                Text(
                    "Record at least two days to start your trend line. All data stays on this phone.",
                    color = ShoopMuted, fontSize = 12.sp,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionHeader("Tonight")
        ShoopCardBox(Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HealthCell(
                    null, "Time asleep",
                    state.asleepMinutes?.let { "%dh %02dm".format((it / 60).toInt(), (it % 60).toInt()) } ?: "--",
                    Modifier.weight(1f), ShoopYellow,
                )
                HealthCell(null, "Samples today", "${state.sampleCount}", Modifier.weight(1f), ShoopMuted)
            }
        }
    }
}
