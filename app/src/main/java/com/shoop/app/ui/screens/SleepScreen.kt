package com.shoop.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoop.app.ui.components.MetricRing
import com.shoop.app.ui.components.StatCard
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
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Sleep", color = ShoopMuted, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        MetricRing(
            fraction = (state.lastScore ?: 0) / 100f,
            valueText = state.lastScore?.let { "$it%" } ?: "--",
            label = "LAST NIGHT",
            color = ShoopYellow,
        )
        Spacer(Modifier.height(16.dp))
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                "Time asleep",
                state.lastAsleepMinutes?.let { "%dh %02dm".format((it / 60).toInt(), (it % 60).toInt()) } ?: "--",
                ShoopBlue, Modifier.weight(1f),
            )
            StatCard("Lowest HR", state.lastRestingHr?.let { "$it bpm" } ?: "--", ShoopBlue, Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        Text("Recorded nights", color = ShoopMuted, fontSize = 13.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(state.history) { item ->
                StatCard(
                    title = item.dateText,
                    value = "%dh %02dm".format((item.durationMinutes / 60).toInt(), (item.durationMinutes % 60).toInt()),
                    accent = ShoopYellow,
                    subtitle = "min ${item.minHr} / avg ${item.avgHr} bpm - ${item.label}",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }
        }
    }
}

fun formatNightDate(epochMs: Long): String =
    SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()).format(Date(epochMs))
