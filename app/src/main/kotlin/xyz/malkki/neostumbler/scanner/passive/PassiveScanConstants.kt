package xyz.malkki.neostumbler.scanner.passive

import kotlin.time.Duration.Companion.seconds

object PassiveScanConstants {
    // Max delay to wait for location. This should not be higher than the age limit for data in
    // reports
    val PASSIVE_LOCATION_MAX_DELAY = 30.seconds
    val PASSIVE_LOCATION_INTERVAL = 15.seconds
}
