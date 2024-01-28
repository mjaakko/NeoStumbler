package xyz.malkki.neostumbler.location

import android.content.Context


class LocationSourceProvider(private val context: Context) {
    fun getLocationSource(): LocationSource {
        return PlatformLocationSource(context)
    }
}