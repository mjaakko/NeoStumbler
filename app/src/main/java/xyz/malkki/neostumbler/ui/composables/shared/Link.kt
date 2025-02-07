package xyz.malkki.neostumbler.ui.composables.shared

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink

@Composable
fun Link(text: String, onClick: () -> Unit, style: TextStyle = MaterialTheme.typography.bodySmall) {
    Text(
        text = buildAnnotatedString {
            withLink(
                link = LinkAnnotation.Clickable(
                    tag = "link",
                    styles = TextLinkStyles(style.copy(color = MaterialTheme.colorScheme.primary).toSpanStyle()),
                    linkInteractionListener = {
                        onClick.invoke()
                    }
                )
            ) {
                append(text)
            }
        },
        style = style
    )
}

@Composable
fun Link(text: String, url: String, style: TextStyle = MaterialTheme.typography.bodySmall) {
    val context = LocalContext.current

    Link(
        text = text,
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        },
        style = style
    )
}