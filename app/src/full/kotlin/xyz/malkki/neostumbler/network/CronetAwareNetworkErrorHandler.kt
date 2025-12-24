package xyz.malkki.neostumbler.network

import java.io.IOException
import org.chromium.net.CronetException
import org.chromium.net.NetworkException
import xyz.malkki.neostumbler.http.isRetryablePlatform

class CronetAwareNetworkErrorHandler : NetworkErrorHandler {
    override fun isRetryable(ioException: IOException): Boolean {
        if (ioException.isRetryablePlatform()) {
            return true
        }

        val cronetException = ioException.underlyingCronetException
        if (cronetException is NetworkException) {
            return cronetException.immediatelyRetryable()
        }

        return false
    }
}

private val Throwable.underlyingCronetException: CronetException?
    get() = this as? CronetException ?: cause?.underlyingCronetException
