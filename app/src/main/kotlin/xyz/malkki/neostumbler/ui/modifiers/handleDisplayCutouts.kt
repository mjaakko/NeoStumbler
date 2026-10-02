package xyz.malkki.neostumbler.ui.modifiers

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Modifier.handleDisplayCutouts(): Modifier {
    return windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
}
