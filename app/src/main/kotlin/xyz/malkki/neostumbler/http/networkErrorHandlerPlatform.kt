package xyz.malkki.neostumbler.http

import java.io.IOException
import java.net.SocketTimeoutException

fun IOException.isRetryablePlatform(): Boolean {
    // Retry timeouts -> we are probably just temporarily disconnected
    return this is SocketTimeoutException
}
