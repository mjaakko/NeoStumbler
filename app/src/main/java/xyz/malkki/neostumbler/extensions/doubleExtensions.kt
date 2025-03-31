package xyz.malkki.neostumbler.extensions

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

fun Double.roundToString(scale: Int): String =
    BigDecimal.valueOf(this).setScale(scale, RoundingMode.HALF_UP).toPlainString()

fun Double.roundToMultipleOf(multiple: Double) = (this / multiple).roundToInt() * multiple
