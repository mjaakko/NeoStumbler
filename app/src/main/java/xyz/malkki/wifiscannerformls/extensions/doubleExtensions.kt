package xyz.malkki.wifiscannerformls.extensions

import java.math.BigDecimal
import java.math.RoundingMode

fun Double.roundToString(scale: Int): String = BigDecimal.valueOf(this).setScale(scale, RoundingMode.HALF_UP).toPlainString()