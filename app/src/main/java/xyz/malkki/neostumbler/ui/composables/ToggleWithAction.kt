package xyz.malkki.neostumbler.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.R

private const val DESCRIPTION_MAX_LINES = 3

private const val DISABLED_OPACITY = 0.38f

@Composable
fun ToggleWithAction(
    title: String,
    description: String? = null,
    warningWhenDisabled: String? = null,
    enabled: Boolean,
    checked: Boolean,
    action: suspend (Boolean) -> Unit,
) {
    val changingState = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val alpha =
        if (!enabled) {
            DISABLED_OPACITY
        } else {
            1f
        }

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .wrapContentHeight()
                .clickable(
                    enabled = enabled && !changingState.value,
                    onClick = {
                        coroutineScope.launch {
                            changingState.value = true
                            action(!checked)
                            changingState.value = false
                        }
                    },
                )
                .defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1.0f), verticalArrangement = Arrangement.Center) {
            Text(
                modifier = Modifier.alpha(alpha),
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (description != null) {
                Text(
                    modifier = Modifier.alpha(alpha),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = DESCRIPTION_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                    text = description,
                )
            }

            if (!enabled && warningWhenDisabled != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val density = LocalDensity.current

                    Icon(
                        modifier =
                            Modifier.requiredSize(
                                with(density) {
                                    MaterialTheme.typography.bodyMedium.fontSize.toDp()
                                }
                            ),
                        painter = painterResource(id = R.drawable.warning_24px),
                        contentDescription = "",
                        tint = MaterialTheme.colorScheme.error,
                    )

                    Text(
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        text = warningWhenDisabled,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Switch(
            modifier = Modifier.wrapContentSize().padding(start = 8.dp),
            enabled = enabled && !changingState.value,
            checked = checked,
            onCheckedChange = null,
        )
    }
}
