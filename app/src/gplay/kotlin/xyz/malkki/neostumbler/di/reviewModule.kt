package xyz.malkki.neostumbler.di

import android.content.Context
import org.koin.dsl.module
import xyz.malkki.neostumbler.utils.review.GooglePlayReviewRequester
import xyz.malkki.neostumbler.utils.review.ReviewRequester

val reviewModule = module { single<ReviewRequester> { GooglePlayReviewRequester(get<Context>()) } }
