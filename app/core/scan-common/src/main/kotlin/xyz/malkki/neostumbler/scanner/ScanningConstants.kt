package xyz.malkki.neostumbler.scanner

object ScanningConstants {
    /**
     * Location accuracy must be better than this to create reports. Used for filtering bad
     * locations (e.g. low quality network-based locations)
     */
    const val LOCATION_MAX_ACCURACY_METERS = 100
}
