package com.shoop.app.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoop.app.ui.theme.ShoopCard
import com.shoop.app.ui.theme.ShoopMuted
import com.shoop.app.ui.theme.ShoopText
import com.shoop.app.ui.theme.ShoopTrack
import java.util.Locale

/** Uppercase micro section header, WHOOP-style. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title.uppercase(Locale.US),
        color = ShoopMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.4.sp,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

/** Base card. */
@Composable
fun ShoopCardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShoopCard),
    ) { Box(Modifier.padding(16.dp)) { content() } }
}

/** Semicircular segmented gauge (0..1), big centered value underneath the arc. */
@Composable
fun SemiGauge(
    fraction: Float,
    valueText: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    segments: Int = 40,
    subText: String? = null,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomCenter) {
            Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                val w = size.width
                val h = size.height
                val radius = minOf(w / 2f, h.toFloat()) - stroke.width
                val center = Offset(w / 2f, h.toFloat())
                val topLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2, radius * 2)
                val gapDeg = 2.2f
                val segDeg = (180f - gapDeg * (segments - 1)) / segments
                val lit = (fraction.coerceIn(0f, 1f) * segments).toInt()
                for (i in 0 until segments) {
                    drawArc(
                        color = if (i < lit) color else ShoopTrack,
                        startAngle = 180f + i * (segDeg + gapDeg),
                        sweepAngle = segDeg,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(valueText, color = ShoopText, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                if (subText != null) Text(subText, color = ShoopMuted, fontSize = 11.sp)
            }
        }
        Text(label.uppercase(Locale.US), color = ShoopMuted, fontSize = 12.sp, letterSpacing = 1.2.sp)
    }
}

/** Full circular ring with centered value. */
@Composable
fun MetricRing(
    fraction: Float,
    valueText: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    subText: String? = null,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            drawArc(
                color = ShoopTrack,
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
            Text(valueText, color = ShoopText, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(label.uppercase(Locale.US), color = ShoopMuted, fontSize = 11.sp, letterSpacing = 1.1.sp)
            if (subText != null) Text(subText, color = ShoopMuted, fontSize = 10.sp)
        }
    }
}

/** Health-monitor grid cell: icon, small label, value (or N/A when not derivable). */
@Composable
fun HealthCell(
    icon: ImageVector?,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = ShoopText,
    available: Boolean = true,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ShoopCard),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = ShoopMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.size(6.dp))
                }
                Text(label.uppercase(Locale.US), color = ShoopMuted, fontSize = 10.sp, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (available) value else "N/A",
                color = if (available) valueColor else ShoopMuted,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** 5-zone heart rate bar with the active zone highlighted. */
@Composable
fun ZoneBar(activeZone: Int, modifier: Modifier = Modifier) {
    val colors = listOf(
        Color(0xFF5BC8F5), Color(0xFF35E0A1), Color(0xFFF5C04E),
        Color(0xFFF59A4E), Color(0xFFF26D6D),
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
                        color = if (zone == activeZone) c else c.copy(alpha = 0.22f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    )
                }
            }
        }
    }
}

/** Minimal line chart for trend history (oldest -> newest). */
@Composable
fun TrendChart(values: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(110.dp)) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val max = values.max()
        val span = (max - min).takeIf { it > 0.001f } ?: 1f
        val stepX = size.width / (values.size - 1)
        fun y(v: Float) = size.height - ((v - min) / span) * (size.height * 0.85f) - size.height * 0.07f
        for (i in 1..3) {
            val gy = size.height * i / 4
            drawLine(ShoopTrack, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
        }
        var prev = Offset(0f, y(values.first()))
        values.forEachIndexed { i, v ->
            val p = Offset(stepX * i, y(v))
            if (i > 0) drawLine(color, prev, p, strokeWidth = 5f, cap = StrokeCap.Round)
            drawCircle(color, radius = 3.dp.toPx(), center = p)
            prev = p
        }
    }
}
