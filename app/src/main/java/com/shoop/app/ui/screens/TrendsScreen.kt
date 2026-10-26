package com.shoop.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoop.app.ui.components.SectionHeader
import com.shoop.app.ui.components.ShoopCardBox
import com.shoop.app.ui.components.TrendChart
import com.shoop.app.ui.theme.ShoopBlue
import com.shoop.app.ui.theme.ShoopGreen
import com.shoop.app.ui.theme.ShoopMuted
import com.shoop.app.ui.theme.ShoopYellow

data class TrendsUiState(
    val rmssdSeries: List<Float>,   // oldest -> newest
    val restingHrSeries: List<Float>,
    val recoverySeries: List<Float>,
    val dayCount: Int,
)

@Composable
fun TrendsScreen(state: TrendsUiState) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Trends", color = ShoopMuted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        if (state.dayCount == 0) {
            ShoopCardBox(Modifier.fillMaxWidth()) {
                Text(
                    "No history yet. Record a night or a day and your trends build up here, " +
                        "stored only on this phone.",
                    color = ShoopMuted, fontSize = 12.sp,
                )
            }
        } else {
            SectionHeader("HRV (RMSSD)")
            ShoopCardBox(Modifier.fillMaxWidth()) { TrendChart(state.rmssdSeries, ShoopGreen) }
            Spacer(Modifier.height(12.dp))
            SectionHeader("Resting HR")
            ShoopCardBox(Modifier.fillMaxWidth()) { TrendChart(state.restingHrSeries, ShoopBlue) }
            Spacer(Modifier.height(12.dp))
            SectionHeader("Recovery")
            ShoopCardBox(Modifier.fillMaxWidth()) { TrendChart(state.recoverySeries, ShoopYellow) }
            Spacer(Modifier.height(8.dp))
            Text("${state.dayCount} days recorded", color = ShoopMuted, fontSize = 11.sp)
        }
    }
}
