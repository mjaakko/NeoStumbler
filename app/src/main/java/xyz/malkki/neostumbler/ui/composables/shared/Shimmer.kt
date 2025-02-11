package xyz.malkki.neostumbler.ui.composables.shared

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun Shimmer(modifier: Modifier, duration: Duration = 1.seconds) {
    val shimmerColors =
        listOf(
            Color.White.copy(alpha = 0.3f),
            Color.White.copy(alpha = 0.5f),
            Color.White.copy(alpha = 1.0f),
            Color.White.copy(alpha = 0.5f),
            Color.White.copy(alpha = 0.3f),
        )

    val transition = rememberInfiniteTransition()

    val translateAnimation =
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

    val brush =
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(x = translateAnimation.value - 500, y = 0.0f),
            end = Offset(x = translateAnimation.value, y = 270f),
        )

    Box(modifier = modifier) { Spacer(modifier = Modifier.matchParentSize().background(brush)) }
}
