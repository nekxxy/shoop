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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoop.app.ui.components.HealthCell
import com.shoop.app.ui.components.ShoopCardBox
import com.shoop.app.ui.components.ZoneBar
import com.shoop.app.ui.theme.ShoopBlue
import com.shoop.app.ui.theme.ShoopGreen
import com.shoop.app.ui.theme.ShoopMuted
import com.shoop.app.ui.theme.ShoopYellow

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
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isDemo) {
            Text("DEMO DATA - simulated, not your band", color = ShoopYellow, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
        }
        ShoopCardBox(Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    state.bpm?.toString() ?: "--",
                    color = ShoopGreen, fontSize = 88.sp, fontWeight = FontWeight.Bold,
                )
                Text("BPM", color = ShoopMuted, fontSize = 14.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(14.dp))
                ZoneBar(activeZone = state.zone)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HealthCell(
                null, "HRV (RMSSD)",
                state.rmssdMs?.let { "%.0f ms".format(it) } ?: "--",
                Modifier.weight(1f), ShoopBlue,
            )
            HealthCell(
                null, "Battery",
                state.batteryPercent?.let { if (it < 0) "N/A" else "$it%" } ?: "--",
                Modifier.weight(1f), ShoopGreen,
            )
        }
        Spacer(Modifier.height(12.dp))
        ShoopCardBox(Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(state.connectionText, color = ShoopMuted, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = if (state.scanning) onStop else onScan,
                        colors = ButtonDefaults.buttonColors(containerColor = ShoopGreen),
                    ) { Text(if (state.scanning) "Stop scan" else "Find band", color = Color.Black) }
                    OutlinedButton(onClick = onToggleDemo) {
                        Text(if (state.isDemo) "Use real band" else "Demo mode")
                    }
                }
                if (state.deviceNames.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    state.deviceNames.take(4).forEach { name ->
                        OutlinedButton(onClick = onConnectFirst, modifier = Modifier.fillMaxWidth()) {
                            Text("Connect: $name")
                        }
                    }
                }
            }
        }
    }
}
