package xyz.malkki.neostumbler.http

import java.io.IOException
import org.chromium.net.CronetException
import org.chromium.net.NetworkException

private val Throwable.underlyingCronetException: CronetException?
    get() = this as? CronetException ?: cause?.underlyingCronetException

fun IOException.isRetryable(): Boolean {
    if (isRetryablePlatform()) {
        return true
    }

    val cronetException = underlyingCronetException
    if (cronetException is NetworkException) {
        return cronetException.immediatelyRetryable()
    }

    return false
}
