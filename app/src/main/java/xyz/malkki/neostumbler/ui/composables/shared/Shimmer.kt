package xyz.malkki.neostumbler.ui.composables.shared

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val SHIMMER_COLORS =
    listOf(
        Color.White.copy(alpha = 0.3f),
        Color.White.copy(alpha = 0.5f),
        Color.White.copy(alpha = 1.0f),
        Color.White.copy(alpha = 0.5f),
        Color.White.copy(alpha = 0.3f),
    )

@Composable
fun Shimmer(modifier: Modifier, duration: Duration = 1.seconds) {
    val transition = rememberInfiniteTransition()

    val translateAnimation by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (duration.inWholeMilliseconds + 500).toFloat(),
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = duration.inWholeMilliseconds.toInt(),
                            easing = LinearEasing,
                        ),
                    repeatMode = RepeatMode.Restart,
                ),
        )

    Box(
        modifier =
            modifier.drawBehind {
                val brush =
                    Brush.linearGradient(
                        colors = SHIMMER_COLORS,
                        start = Offset(x = translateAnimation - 500, y = 0.0f),
                        end = Offset(x = translateAnimation, y = 270f),
                    )

                drawRect(brush = brush)
            }
    )
}
