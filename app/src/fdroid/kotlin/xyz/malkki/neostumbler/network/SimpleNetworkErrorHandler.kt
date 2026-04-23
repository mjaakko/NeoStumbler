package xyz.malkki.neostumbler.network

import java.io.IOException
import xyz.malkki.neostumbler.http.isRetryablePlatform

class SimpleNetworkErrorHandler : NetworkErrorHandler {
    override fun isRetryable(ioException: IOException): Boolean {
        return ioException.isRetryablePlatform()
    }
}
