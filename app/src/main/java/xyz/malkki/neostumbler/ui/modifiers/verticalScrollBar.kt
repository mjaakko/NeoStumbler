package xyz.malkki.neostumbler.ui.modifiers

import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.verticalScrollbar(
    scrollState: ScrollState,
    width: Dp = 4.dp,
    scrollBarColor: Color = MaterialTheme.colorScheme.primary,
    scrollBarCornerRadius: Float = 4f,
    endPadding: Dp = 1.5.dp,
): Modifier {
    return drawWithContent {
        drawContent()

        // Draw scroll bar only when the content size is known and the content does not fit the
        // viewport
        if (scrollState.maxValue != 0 && scrollState.maxValue != Int.MAX_VALUE) {
            val viewportHeight = size.height

            val contentHeight = scrollState.maxValue.toFloat() + viewportHeight

            val scrollPos = scrollState.value.toFloat()

            val scrollBarHeight = (viewportHeight / contentHeight) * viewportHeight
            val scrollBarStartOffset = (scrollPos / contentHeight) * viewportHeight

            drawRoundRect(
                cornerRadius = CornerRadius(scrollBarCornerRadius),
                color = scrollBarColor,
                topLeft = Offset(size.width - endPadding.toPx(), scrollBarStartOffset),
                size = Size(width.toPx(), scrollBarHeight),
            )
        }
    }
}
