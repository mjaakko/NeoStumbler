package xyz.malkki.neostumbler.network

import java.io.IOException

fun interface NetworkErrorHandler {
    fun isRetryable(ioException: IOException): Boolean
}
