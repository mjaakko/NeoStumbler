package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsItem(
    title: String,
    description: String? = null,
    maxTitleLines: Int = 2,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .wrapContentHeight()
                .defaultMinSize(minHeight = 48.dp)
                .clickable { onClick.invoke() },
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, maxLines = maxTitleLines, overflow = TextOverflow.Ellipsis)
        if (description != null) {
            Text(
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                text = description,
            )
        }
    }
}
