package ru.localdialog.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SonicOrb(modifier: Modifier = Modifier, active: Boolean = false, level: Float = 0.6f) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val min = size.minDimension
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFD5E7BC), Color(0xFF829F8B), Color(0xFF233F38)),
                    center = center,
                    radius = min * 0.47f,
                ),
                radius = min * 0.47f,
            )
            drawCircle(Color.White.copy(alpha = 0.22f), radius = min * 0.31f, center = Offset(center.x - min * 0.08f, center.y - min * 0.09f), style = Stroke(min * 0.012f))
            val bars = 19
            val width = min * 0.62f
            val barWidth = min * 0.012f
            for (i in 0 until bars) {
                val wave = kotlin.math.sin(i * 1.73 + (if (active) level * 6 else 0f)).toFloat()
                val envelope = kotlin.math.sin(Math.PI * i / (bars - 1)).toFloat()
                val height = min * (0.06f + envelope * (0.16f + kotlin.math.abs(wave) * 0.18f))
                val x = center.x - width / 2 + width * i / (bars - 1)
                drawLine(
                    color = Color(0xFFEDF8D9),
                    start = Offset(x, center.y - height / 2),
                    end = Offset(x, center.y + height / 2),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
fun BackHeader(title: String, subtitle: String? = null, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        actions()
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
}

@Composable
fun PrivacyBadge() {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(Moss, CircleShape))
            Text("Только на устройстве", style = MaterialTheme.typography.labelSmall)
        }
    }
}
