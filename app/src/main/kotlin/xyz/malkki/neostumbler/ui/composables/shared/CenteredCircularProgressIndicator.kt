package xyz.malkki.neostumbler.ui.composables.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun CenteredCircularProgressIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun CenteredCircularProgressIndicator() {
    CenteredCircularProgressIndicator(modifier = Modifier.fillMaxSize())
}
