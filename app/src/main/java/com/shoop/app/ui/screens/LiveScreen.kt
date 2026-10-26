package com.shoop.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoop.app.ui.components.StatCard
import com.shoop.app.ui.components.ZoneBar
import com.shoop.app.ui.theme.ShoopGreen
import com.shoop.app.ui.theme.ShoopMuted

data class LiveUiState(
    val bpm: Int?,
    val rmssdMs: Double?,
    val batteryPercent: Int?,
    val zone: Int,
    val connectionText: String,
    val isDemo: Boolean,
    val scanning: Boolean,
    val deviceNames: List<String>,
)

@Composable
fun LiveScreen(
    state: LiveUiState,
    onScan: () -> Unit,
    onStop: () -> Unit,
    onConnectFirst: () -> Unit,
    onToggleDemo: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isDemo) {
            Text("DEMO DATA - simulated, not your band", color = com.shoop.app.ui.theme.ShoopYellow, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            state.bpm?.toString() ?: "--",
            color = ShoopGreen, fontSize = 96.sp, fontWeight = FontWeight.Bold,
        )
        Text("BPM", color = ShoopMuted, fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))
        ZoneBar(activeZone = state.zone, modifier = Modifier.padding(horizontal = 8.dp))
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "HRV (RMSSD)",
                value = state.rmssdMs?.let { "%.0f ms".format(it) } ?: "--",
                accent = com.shoop.app.ui.theme.ShoopBlue,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = "Battery",
                value = state.batteryPercent?.let { "$it%" } ?: "--",
                accent = ShoopGreen,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(state.connectionText, color = ShoopMuted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = if (state.scanning) onStop else onScan,
                colors = ButtonDefaults.buttonColors(containerColor = ShoopGreen),
            ) { Text(if (state.scanning) "Stop scan" else "Find band", color = androidx.compose.ui.graphics.Color.Black) }
            OutlinedButton(onClick = onToggleDemo) {
                Text(if (state.isDemo) "Use real band" else "Demo mode")
            }
        }
        if (state.deviceNames.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            state.deviceNames.take(4).forEach { name ->
                OutlinedButton(onClick = onConnectFirst, modifier = Modifier.fillMaxWidth()) {
                    Text("Connect: $name")
                }
            }
        }
    }
}
