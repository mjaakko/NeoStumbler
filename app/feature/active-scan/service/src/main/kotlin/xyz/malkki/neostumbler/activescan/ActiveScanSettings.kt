package xyz.malkki.neostumbler.activescan

import xyz.malkki.neostumbler.core.values.Distance
import xyz.malkki.neostumbler.data.emitter.ActiveWifiAccessPointSource

data class ActiveScanSettings(
    /** Wi-Fi scan interval (meters) */
    val wifiScanDistance: Distance,
    /** Cell tower scan interval (meters) */
    val cellScanDistance: Distance,
    /** Bluetooth beacon scan interval (meters) */
    val bleScanDistance: Distance,
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
    val wifiRttRangingMode: ActiveWifiAccessPointSource.RangingMode,
)
