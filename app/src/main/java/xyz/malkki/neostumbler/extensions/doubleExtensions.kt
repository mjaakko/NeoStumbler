package xyz.malkki.neostumbler.extensions

import java.math.BigDecimal
import java.math.RoundingMode

fun Double.roundToString(scale: Int): String = BigDecimal.valueOf(this).setScale(scale, RoundingMode.HALF_UP).toPlainString()