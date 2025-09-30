package xyz.malkki.neostumbler.utils

import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

fun openUrl(url: String): Intent {
    return CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setUrlBarHidingEnabled(false)
        .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        .build()
        .intent
        .apply { data = url.toUri() }
}
