package xyz.malkki.neostumbler.http

import java.io.IOException

fun IOException.isRetryable(): Boolean = isRetryablePlatform()
