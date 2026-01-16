package xyz.malkki.neostumbler.utils.review

import android.app.Activity

interface ReviewRequester {
    val isReviewSupported: Boolean

    suspend fun requestReview(activity: Activity)
}
