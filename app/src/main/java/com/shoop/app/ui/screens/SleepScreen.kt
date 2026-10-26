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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoop.app.ui.components.HealthCell
import com.shoop.app.ui.components.MetricRing
import com.shoop.app.ui.components.SectionHeader
import com.shoop.app.ui.components.ShoopCardBox
import com.shoop.app.ui.theme.ShoopBlue
import com.shoop.app.ui.theme.ShoopMuted
import com.shoop.app.ui.theme.ShoopYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SleepUiState(
    val lastScore: Int?,
    val lastAsleepMinutes: Double?,
    val lastRestingHr: Int?,
    val history: List<SleepHistoryItem>,
)

data class SleepHistoryItem(
    val label: String,
    val dateText: String,
    val durationMinutes: Double,
    val minHr: Int,
    val avgHr: Int,
)

@Composable
fun SleepScreen(state: SleepUiState) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Sleep", color = ShoopMuted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        ShoopCardBox(Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                MetricRing(
                    fraction = (state.lastScore ?: 0) / 100f,
                    valueText = state.lastScore?.let { "$it%" } ?: "--",
                    label = "Last night",
                    color = ShoopYellow,
                    size = 170.dp,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    HealthCell(
                        null, "Time asleep",
                        state.lastAsleepMinutes?.let { "%dh %02dm".format((it / 60).toInt(), (it % 60).toInt()) } ?: "--",
                        Modifier.weight(1f), ShoopBlue,
                    )
                    HealthCell(null, "Lowest HR", state.lastRestingHr?.let { "$it bpm" } ?: "--", Modifier.weight(1f), ShoopBlue)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        SectionHeader("Recorded nights", Modifier.align(Alignment.Start))
        if (state.history.isEmpty()) {
            Text(
                "Wear your band to bed with Shoop connected and your nights appear here.",
                color = ShoopMuted, fontSize = 12.sp,
            )
        }
        state.history.forEach { item ->
            ShoopCardBox(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(item.dateText, color = ShoopMuted, fontSize = 11.sp)
                        Text(
                            "%dh %02dm".format((item.durationMinutes / 60).toInt(), (item.durationMinutes % 60).toInt()),
                            color = ShoopYellow, fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        )
                    }
                    Text("min ${item.minHr} / avg ${item.avgHr} bpm", color = ShoopMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

fun formatNightDate(epochMs: Long): String =
    SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()).format(Date(epochMs))
