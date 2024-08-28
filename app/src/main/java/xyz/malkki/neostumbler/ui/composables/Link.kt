package xyz.malkki.neostumbler.ui.composables

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle

@Composable
fun Link(text: String, url: String, style: TextStyle = MaterialTheme.typography.bodySmall) {
    val context = LocalContext.current

    ClickableText(
        text = AnnotatedString(text),
        style = style.copy(color = MaterialTheme.colorScheme.primary),
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    )
}