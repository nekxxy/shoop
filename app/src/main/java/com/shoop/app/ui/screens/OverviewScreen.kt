package com.shoop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoop.app.score.Scores
import com.shoop.app.ui.components.HealthCell
import com.shoop.app.ui.components.MetricRing
import com.shoop.app.ui.components.SectionHeader
import com.shoop.app.ui.components.ShoopCardBox
import com.shoop.app.ui.components.TrendChart
import com.shoop.app.ui.theme.ShoopBlue
import com.shoop.app.ui.theme.ShoopCard
import com.shoop.app.ui.theme.ShoopGreen
import com.shoop.app.ui.theme.ShoopMuted
import com.shoop.app.ui.theme.ShoopTeal
import com.shoop.app.ui.theme.ShoopText
import com.shoop.app.ui.theme.ShoopTrack
import com.shoop.app.ui.theme.ShoopYellow
import com.shoop.app.ui.theme.recoveryColor
import java.util.Locale

data class ActivityRowData(
    val id: String,
    val label: String,
    val durationText: String,     // "6:38"
    val timeRangeText: String,    // "0:02 - 7:17"
    val accent: Color,
    val strainText: String?,      // "3.6"
)

data class JournalUiState(
    val habits: List<String>,
    val todayChecked: List<String>,
    /** last 7 days, oldest first: (weekday letter, fraction accomplished) */
    val week: List<Pair<String, Float>>,
    val todayFraction: Float,
)

data class DashboardCardState(
    val sleepDebtMinutes: Double,
    val sleepConsistencyPct: Int,
    val sleepPerformancePct: Int,
    val sleepEfficiencyPct: Int,
    val nightsRecorded: Int,
    val restingHr: Int,
    val restingHrDelta: Int,
    val zones13WeeklyMinutes: Double,
    val strainSeries: List<Float>,
    val recoverySeries: List<Float>,
)

data class OverviewUiState(
    val dateText: String,
    val strain: Double,
    val recovery: Int?,
    val sleepScore: Int?,
    val restingHr: Int,
    val rmssdMs: Double?,
    val activities: List<ActivityRowData>,
    val activeActivityLabel: String?,
    val journal: JournalUiState,
    val dashboard: DashboardCardState,
)

@Composable
fun OverviewScreen(
    state: OverviewUiState,
    onAddActivity: (label: String, durationMinutes: Int) -> Unit,
    onStartActivity: () -> Unit,
    onStopActivity: () -> Unit,
    onToggleHabit: (String) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
    ) {
        // Top score pills: SLEEP / RECOVERY / STRAIN
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ScorePill("Sleep", state.sleepScore?.let { "$it%" } ?: "--", ShoopBlue, Modifier.weight(1f))
            ScorePill(
                "Recovery", state.recovery?.let { "$it%" } ?: "--",
                state.recovery?.let { recoveryColor(it) } ?: ShoopMuted, Modifier.weight(1f),
            )
            ScorePill("Strain", "%.1f".format(state.strain), ShoopTeal, Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))
        Text("My Day", color = ShoopText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(state.dateText, color = ShoopMuted, fontSize = 11.sp)

        Spacer(Modifier.height(14.dp))
        SectionHeader("Activities")
        state.activities.forEach { ActivityRow(it) }
        if (state.activities.isEmpty()) {
            Text("Nothing logged yet today.", color = ShoopMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { showAddDialog = true }, modifier = Modifier.weight(1f)) {
                Text("+ ADD ACTIVITY", fontSize = 12.sp, letterSpacing = 1.sp)
            }
            if (state.activeActivityLabel == null) {
                OutlinedButton(onClick = onStartActivity, modifier = Modifier.weight(1f)) {
                    Text("▶ START ACTIVITY", fontSize = 12.sp, letterSpacing = 1.sp, color = ShoopGreen)
                }
            } else {
                Button(
                    onClick = onStopActivity,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ShoopGreen),
                ) { Text("■ STOP: ${state.activeActivityLabel}", fontSize = 12.sp, color = Color.Black) }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader("My Journal")
        ShoopCardBox(Modifier.fillMaxWidth()) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    state.journal.week.forEach { (day, frac) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day, color = ShoopMuted, fontSize = 10.sp)
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(if (frac >= 1f) ShoopGreen else if (frac > 0f) ShoopTeal else ShoopTrack),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (frac > 0f) Text("✓", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "${(state.journal.todayFraction * 100).toInt()}% ACCOMPLISHED TODAY",
                    color = ShoopMuted, fontSize = 10.sp, letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { state.journal.todayFraction },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = ShoopGreen, trackColor = ShoopTrack,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.journal.habits.forEach { h ->
                        val done = h in state.journal.todayChecked
                        OutlinedButton(onClick = { onToggleHabit(h) }) {
                            Text((if (done) "✓ " else "") + h, fontSize = 11.sp, color = if (done) ShoopGreen else ShoopMuted)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("My Dashboard", color = ShoopText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        val d = state.dashboard
        if (d.nightsRecorded == 0 && d.strainSeries.isEmpty()) {
            ShoopCardBox(Modifier.fillMaxWidth()) {
                Text(
                    "Dashboard cards fill in as you record nights and days with your band. " +
                        "Everything is computed on this phone from your recordings.",
                    color = ShoopMuted, fontSize = 12.sp,
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HealthCell(
                    null, "Sleep debt",
                    "%d:%02d".format((d.sleepDebtMinutes / 60).toInt(), (d.sleepDebtMinutes % 60).toInt()),
                    Modifier.weight(1f), ShoopYellow,
                )
                HealthCell(null, "Sleep consistency", "${d.sleepConsistencyPct}%", Modifier.weight(1f), ShoopBlue)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HealthCell(null, "Sleep performance", "${d.sleepPerformancePct}%", Modifier.weight(1f), ShoopBlue)
                HealthCell(null, "Sleep efficiency", "${d.sleepEfficiencyPct}%", Modifier.weight(1f), ShoopBlue)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HealthCell(
                    null, "Resting heart rate", "${d.restingHr}",
                    Modifier.weight(1f), ShoopBlue,
                )
                HealthCell(
                    null, "HR zones 1-3 (weekly)",
                    "%d:%02d".format((d.zones13WeeklyMinutes / 60).toInt(), (d.zones13WeeklyMinutes % 60).toInt()),
                    Modifier.weight(1f), ShoopGreen,
                )
            }
            if (d.recoverySeries.size >= 2 || d.strainSeries.size >= 2) {
                Spacer(Modifier.height(12.dp))
                SectionHeader("Strain & recovery")
                ShoopCardBox(Modifier.fillMaxWidth()) {
                    Column {
                        if (d.recoverySeries.size >= 2) {
                            Text("RECOVERY", color = ShoopMuted, fontSize = 10.sp, letterSpacing = 1.sp)
                            TrendChart(d.recoverySeries, ShoopGreen)
                        }
                        if (d.strainSeries.size >= 2) {
                            Spacer(Modifier.height(8.dp))
                            Text("STRAIN", color = ShoopMuted, fontSize = 10.sp, letterSpacing = 1.sp)
                            TrendChart(d.strainSeries, ShoopTeal)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var label by remember { mutableStateOf("") }
        var duration by remember { mutableStateOf("45") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Log activity") },
            text = {
                Column {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Activity (e.g. Run, Yoga)") })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = duration, onValueChange = { duration = it.filter { ch -> ch.isDigit() } }, label = { Text("Duration (minutes)") })
                    Text("Counts from now backwards. If your band was streaming, its HR is used for strain.", color = ShoopMuted, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddActivity(label, duration.toIntOrNull() ?: 0)
                    showAddDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ScorePill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(ShoopCard).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label.uppercase(Locale.US), color = ShoopMuted, fontSize = 9.sp, letterSpacing = 1.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActivityRow(a: ActivityRowData) {
    ShoopCardBox(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(a.accent.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(a.label.first().uppercase(Locale.US), color = a.accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(a.durationText, color = ShoopText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(a.label.uppercase(Locale.US), color = ShoopMuted, fontSize = 10.sp, letterSpacing = 1.sp)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                if (a.strainText != null) Text(a.strainText, color = ShoopTeal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(a.timeRangeText, color = ShoopMuted, fontSize = 10.sp)
            }
        }
    }
}
