package xyz.malkki.neostumbler.utils

import android.content.Context
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener

/** [LinkInteractionListener] which opens the URL with Custom Tabs */
class CustomTabsLinkInteractionListener(private val context: Context) : LinkInteractionListener {
    override fun onClick(link: LinkAnnotation) {
        if (link is LinkAnnotation.Url) {
            context.startActivity(openUrl(link.url))
        }
    }
}
