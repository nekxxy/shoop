package com.shoop.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoop.app.ui.theme.ShoopMuted
import com.shoop.app.ui.theme.ShoopSurface

/** Circular progress ring with a big centered value. */
@Composable
fun MetricRing(
    fraction: Float,
    valueText: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    subText: String? = null,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            drawArc(
                color = Color(0xFF2A2A33),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - stroke.width, this.size.height - stroke.width),
                style = stroke,
            )
            drawArc(
                color = color,
                startAngle = -90f, sweepAngle = 360f * fraction.coerceIn(0f, 1f), useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - stroke.width, this.size.height - stroke.width),
                style = stroke,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(valueText, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(label, color = ShoopMuted, fontSize = 13.sp)
            if (subText != null) Text(subText, color = ShoopMuted, fontSize = 11.sp)
        }
    }
}

/** Small titled stat card. */
@Composable
fun StatCard(title: String, value: String, accent: Color, modifier: Modifier = Modifier, subtitle: String? = null) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ShoopSurface),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = ShoopMuted, fontSize = 12.sp)
            Text(value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, color = ShoopMuted, fontSize = 11.sp)
        }
    }
}

/** 5-zone heart rate bar with the active zone highlighted. */
@Composable
fun ZoneBar(activeZone: Int, modifier: Modifier = Modifier) {
    val colors = listOf(
        Color(0xFF38BDF8), Color(0xFF4ADE80), Color(0xFFFACC15),
        Color(0xFFFB923C), Color(0xFFF87171),
    )
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        colors.forEachIndexed { i, c ->
            val zone = i + 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (zone == activeZone) 14.dp else 8.dp),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(
                        color = if (zone == activeZone) c else c.copy(alpha = 0.25f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    )
                }
            }
        }
    }
}

/** Minimal line chart for trend history (newest last). */
@Composable
fun TrendChart(values: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val max = values.max()
        val span = (max - min).takeIf { it > 0.001f } ?: 1f
        val stepX = size.width / (values.size - 1)
        fun y(v: Float) = size.height - ((v - min) / span) * (size.height * 0.85f) - size.height * 0.07f
        // baseline grid
        for (i in 1..3) {
            val gy = size.height * i / 4
            drawLine(Color(0xFF2A2A33), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
        }
        var prev = Offset(0f, y(values.first()))
        values.forEachIndexed { i, v ->
            val p = Offset(stepX * i, y(v))
            if (i > 0) drawLine(color, prev, p, strokeWidth = 5f, cap = StrokeCap.Round)
            drawCircle(color, radius = 4.dp.toPx() / 2, center = p)
            prev = p
        }
    }
}
