package xyz.malkki.neostumbler.extensions

import java.io.IOException
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

suspend fun Call.executeSuspending(): Response =
    suspendCancellableCoroutine { cancellableContinuation ->
        enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    cancellableContinuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    cancellableContinuation.resume(response) { _, res, _ -> res.close() }
                }
            }
        )

        cancellableContinuation.invokeOnCancellation {
            if (!isCanceled()) {
                cancel()
            }
        }
    }
