package xyz.malkki.neostumbler.activescan.adapter

fun interface ScanStartErrorHandler {
    fun handleScanStartFailed(exception: Exception)
}
