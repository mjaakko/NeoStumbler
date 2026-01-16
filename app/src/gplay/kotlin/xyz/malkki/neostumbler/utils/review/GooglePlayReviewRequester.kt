package xyz.malkki.neostumbler.utils.review

import android.app.Activity
import android.content.Context
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

class GooglePlayReviewRequester(private val reviewManager: ReviewManager) : ReviewRequester {
    constructor(context: Context) : this(ReviewManagerFactory.create(context.applicationContext))

    override val isReviewSupported: Boolean = true

    override suspend fun requestReview(activity: Activity) {
        reviewManager.launchReview(activity, reviewManager.requestReview())
    }
}
