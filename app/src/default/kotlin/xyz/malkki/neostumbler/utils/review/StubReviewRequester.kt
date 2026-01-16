package xyz.malkki.neostumbler.utils.review

import android.app.Activity

object StubReviewRequester : ReviewRequester {
    override val isReviewSupported: Boolean = false

    override suspend fun requestReview(activity: Activity) {
        // no-op, review not supported
    }
}
