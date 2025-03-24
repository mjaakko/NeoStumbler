package xyz.malkki.neostumbler.ui.composables.shared

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.util.Date

@Composable
fun formattedDate(instant: Instant): String {
    val context = LocalContext.current

    return remember(context, instant) {
        val date = Date.from(instant)

        "${DateFormat.getMediumDateFormat(context).format(date)} ${DateFormat.getTimeFormat(context).format(date)}"
    }
}
