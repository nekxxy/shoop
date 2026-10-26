package com.shoop.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoop.app.score.Scores
import com.shoop.app.ui.components.MetricRing
import com.shoop.app.ui.components.StatCard
import com.shoop.app.ui.theme.ShoopBlue
import com.shoop.app.ui.theme.ShoopMuted
import com.shoop.app.ui.theme.ShoopYellow
import com.shoop.app.ui.theme.recoveryColor

data class TodayScreenState(
    val strain: Double,
    val recovery: Int?,
    val sleepScore: Int?,
    val sampleCount: Int,
    val restingHr: Int,
)

@Composable
fun TodayScreen(state: TodayScreenState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Today", color = ShoopMuted, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        MetricRing(
            fraction = (state.strain / 21.0).toFloat(),
            valueText = "%.1f".format(state.strain),
            label = "STRAIN",
            color = ShoopBlue,
            subText = "of 21",
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricRing(
                fraction = (state.recovery ?: 0) / 100f,
                valueText = state.recovery?.let { "$it%" } ?: "--",
                label = "RECOVERY",
                color = state.recovery?.let { recoveryColor(it) } ?: ShoopMuted,
                size = 130.dp,
                subText = state.recovery?.let { Scores.recoveryLabel(it) },
            )
            MetricRing(
                fraction = (state.sleepScore ?: 0) / 100f,
                valueText = state.sleepScore?.let { "$it%" } ?: "--",
                label = "SLEEP",
                color = ShoopYellow,
                size = 130.dp,
            )
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Resting HR", "${state.restingHr} bpm", ShoopBlue, Modifier.weight(1f))
            StatCard("Samples today", "${state.sampleCount}", ShoopMuted, Modifier.weight(1f))
        }
    }
}
