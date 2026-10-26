package xyz.malkki.neostumbler.ui.animation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

val fadeInFadeOut = fadeIn(tween(500)) togetherWith fadeOut(tween(500))
