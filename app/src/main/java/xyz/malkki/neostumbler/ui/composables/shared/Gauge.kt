package xyz.malkki.neostumbler.ui.composables.shared

import androidx.annotation.FloatRange
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val START_ANGLE = 135f
private const val MAX_ANGLE = 270f

@Composable
fun Gauge(
    modifier: Modifier = Modifier,
    @FloatRange(from = 0.0, to = 1.0) percentage: Float,
    gaugeColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
) {
    val gaugeState = animateFloatAsState(targetValue = percentage.coerceIn(0f, 1f))

    val density = LocalDensity.current

    val width = density.run { 2.dp.toPx().coerceAtLeast(0f) }

    Canvas(modifier = modifier) {
        inset(width) {
            drawArc(
                brush = SolidColor(backgroundColor),
                style = Stroke(width = width, cap = StrokeCap.Round),
                startAngle = START_ANGLE,
                sweepAngle = MAX_ANGLE,
                useCenter = false,
            )

            drawArc(
                brush = SolidColor(gaugeColor),
                style = Stroke(width = width, cap = StrokeCap.Round),
                startAngle = START_ANGLE,
                sweepAngle = MAX_ANGLE * gaugeState.value,
                useCenter = false,
            )
        }
    }
}

@Preview
@Composable
private fun GaugePreview() {
    Gauge(percentage = 0.852f)
}
