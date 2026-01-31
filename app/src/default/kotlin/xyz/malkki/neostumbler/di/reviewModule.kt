package xyz.malkki.neostumbler.di

import org.koin.dsl.module
import xyz.malkki.neostumbler.utils.review.ReviewRequester
import xyz.malkki.neostumbler.utils.review.StubReviewRequester

val reviewModule = module { single<ReviewRequester> { StubReviewRequester } }
