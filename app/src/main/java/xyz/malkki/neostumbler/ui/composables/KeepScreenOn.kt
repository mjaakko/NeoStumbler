package xyz.malkki.neostumbler.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

@Composable
fun KeepScreenOn() {
    val view = LocalView.current

    DisposableEffect(view) {
        view.keepScreenOn = true

        onDispose {
            view.keepScreenOn = false
        }
    }
}