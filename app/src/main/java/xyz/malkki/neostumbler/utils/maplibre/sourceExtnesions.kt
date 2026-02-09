package xyz.malkki.neostumbler.utils.maplibre

import org.maplibre.android.style.sources.Source

fun Source.needsRecreation(): Boolean = nativePtr == 0L
