package com.lingjing.feature.attribute.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.lingjing.domain.model.Attribute
import com.lingjing.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val AXIS_KEYS = listOf("wisdom", "physique", "perception", "energy", "will")
private val AXIS_LABELS = listOf("悟性", "体魄", "神识", "精力", "意志")

private val AXIS_COLORS = listOf(
    Color(0xFF7C4DFF), // 悟性 - deep purple
    Color(0xFFFF6B6B), // 体魄 - coral red
    Color(0xFF4ECDC4), // 神识 - teal
    Color(0xFFFFD93D), // 精力 - gold
    Color(0xFFFF8C42), // 意志 - orange
)

@Composable
fun RadarChart(
    attributes: List<Attribute>,
    modifier: Modifier = Modifier,
    animationMs: Int = 1200
) {
    val requiredKeys = setOf("wisdom", "physique", "perception", "energy", "will")
    val providedKeys = attributes.map { it.key }.toSet()
    val isComplete = requiredKeys.all { it in providedKeys }

    if (!isComplete) {
        val textMeasurer = rememberTextMeasurer()
        Canvas(modifier = modifier.size(280.dp)) {
            val textResult = textMeasurer.measure(
                text = AnnotatedString("属性加载中..."),
                style = TextStyle(color = LightInk, fontSize = 14.sp, textAlign = TextAlign.Center)
            )
            drawText(textResult, topLeft = Offset((size.width - textResult.size.width) / 2f, (size.height - textResult.size.height) / 2f))
        }
        return
    }

    val levelMap: Map<String, Int> = attributes.associate { it.key to it.level }
    val maxLevel = attributes.maxOfOrNull { it.level }?.coerceAtLeast(1) ?: 1

    val progress = remember { Animatable(0f) }
    LaunchedEffect(attributes) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(animationMs))
    }

    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    val gridColorMajor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val bgFillColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

    Canvas(modifier = modifier.size(280.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = size.minDimension / 2f - 44f

            // Background fill
            val bgPath = buildRing(cx, cy, maxR)
            drawPath(bgPath, color = bgFillColor, style = Fill)

            // Grid rings
            val ringFractions = listOf(0.25f, 0.50f, 0.75f, 1.00f)
            for ((idx, fraction) in ringFractions.withIndex()) {
                val r = maxR * fraction
                val ringPath = buildRing(cx, cy, r)
                drawPath(
                    ringPath,
                    color = if (idx == ringFractions.lastIndex) gridColorMajor else gridColor,
                    style = Stroke(width = if (idx == ringFractions.lastIndex) 1.5f else 0.8f)
                )
            }

            // Axis lines from center
            for (i in AXIS_KEYS.indices) {
                val angle = axisAngle(i)
                val endX = cx + maxR * cos(angle)
                val endY = cy + maxR * sin(angle)
                drawLine(
                    color = gridColor,
                    start = Offset(cx, cy),
                    end = Offset(endX, endY),
                    strokeWidth = 1f
                )

                // Axis label
                val label = AXIS_LABELS[i]
                val offsetR = 12f
                val labelX = cx + (maxR + offsetR) * cos(angle)
                val labelY = cy + (maxR + offsetR) * sin(angle)
                val labelResult = textMeasurer.measure(
                    text = AnnotatedString(label),
                    style = TextStyle(color = AXIS_COLORS[i], fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                )
                drawText(labelResult, topLeft = Offset(labelX - labelResult.size.width / 2f, labelY - labelResult.size.height / 2f))

                // Level number above label
                val level = levelMap[AXIS_KEYS[i]] ?: 0
                val levelStr = "Lv.$level"
                val levelResult = textMeasurer.measure(
                    text = AnnotatedString(levelStr),
                    style = TextStyle(color = AXIS_COLORS[i].copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                )
                val innerR = maxR - 42f
                val lvX = cx + innerR * cos(angle)
                val lvY = cy + innerR * sin(angle)
                drawText(levelResult, topLeft = Offset(lvX - levelResult.size.width / 2f, lvY - levelResult.size.height / 2f))
            }

            // Animated fill polygon with gradient
            val currentProgress = progress.value
            val polygonPath = Path()
            var minR = Float.MAX_VALUE
            for (i in AXIS_KEYS.indices) {
                val level = levelMap[AXIS_KEYS[i]] ?: 0
                val fraction = level.toFloat() / maxLevel
                val r = maxR * fraction * currentProgress
                if (r < minR) minR = r
                val angle = axisAngle(i)
                val px = cx + r * cos(angle)
                val py = cy + r * sin(angle)
                if (i == 0) polygonPath.moveTo(px, py) else polygonPath.lineTo(px, py)
            }
            polygonPath.close()

            // Gradient fill
            val gradientCenter = Offset(cx, cy)
            val gradientBrush = Brush.radialGradient(
                colors = listOf(
                    FlowerCyan.copy(alpha = 0.35f),
                    FlowerCyan.copy(alpha = 0.15f),
                    FlowerCyan.copy(alpha = 0.05f)
                ),
                center = gradientCenter,
                radius = maxR
            )
            drawPath(polygonPath, brush = gradientBrush, style = Fill)

            // Polygon edge lines
            for (i in AXIS_KEYS.indices) {
                val nextI = (i + 1) % AXIS_KEYS.size
                val level = levelMap[AXIS_KEYS[i]] ?: 0
                val nextLevel = levelMap[AXIS_KEYS[nextI]] ?: 0
                val r = maxR * (level.toFloat() / maxLevel) * currentProgress
                val nextR = maxR * (nextLevel.toFloat() / maxLevel) * currentProgress
                val ax = cx + r * cos(axisAngle(i))
                val ay = cy + r * sin(axisAngle(i))
                val bx = cx + nextR * cos(axisAngle(nextI))
                val by = cy + nextR * sin(axisAngle(nextI))
                drawLine(
                    color = FlowerCyan.copy(alpha = 0.7f),
                    start = Offset(ax, ay),
                    end = Offset(bx, by),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }

            // Vertex dots with glow
            for (i in AXIS_KEYS.indices) {
                val level = levelMap[AXIS_KEYS[i]] ?: 0
                val r = maxR * (level.toFloat() / maxLevel) * currentProgress
                val angle = axisAngle(i)
                val px = cx + r * cos(angle)
                val py = cy + r * sin(angle)
                // Outer glow
                drawCircle(color = AXIS_COLORS[i].copy(alpha = 0.3f), radius = 8f, center = Offset(px, py))
                // Solid dot
                drawCircle(color = AXIS_COLORS[i], radius = 4.5f, center = Offset(px, py))
                // White center
                drawCircle(color = Color.White, radius = 2f, center = Offset(px, py))
            }
    }
}

private fun axisAngle(index: Int): Float = (-PI / 2 + index * 2 * PI / 5).toFloat()

private fun DrawScope.buildRing(cx: Float, cy: Float, r: Float): Path {
    val path = Path()
    for (i in 0 until 5) {
        val angle = axisAngle(i)
        val px = cx + r * cos(angle)
        val py = cy + r * sin(angle)
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    return path
}
