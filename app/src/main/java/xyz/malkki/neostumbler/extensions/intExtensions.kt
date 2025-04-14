package xyz.malkki.neostumbler.extensions

@Suppress("MagicNumber")
/** Converts an integer to a percentage, e.g. 78 -> 0.78 */
fun Int.toPercentage(): Float = this.toFloat() / 100
