package xyz.malkki.neostumbler.activescan

import xyz.malkki.neostumbler.core.values.Distance

data class ActiveScanSettings(
    /** Wi-Fi scan interval (meters) */
    val wifiScanDistance: Distance,
    /** Cell tower scan interval (meters) */
    val cellScanDistance: Distance,
    /** Whether to ignore Android Wi-Fi scan throttling */
    val ignoreWifiScanThrottling: Boolean,
    /**
     * Threshold for low battery level when to pause scanning.
     *
     * [0-100], `ǹull` if disabled
     */
    val lowBatteryThreshold: Int?,
    /** Whether to pause scanning when the device is overheating */
    val pauseWhenOverheating: Boolean,
)
