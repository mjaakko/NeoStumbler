package xyz.malkki.neostumbler.utils

// Note: this always returns true to avoid requesting the permission on the gplay variant
val android.content.Context.hasLocalNetworkPermission: Boolean
    get() = true
