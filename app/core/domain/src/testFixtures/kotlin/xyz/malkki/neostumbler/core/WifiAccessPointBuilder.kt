package xyz.malkki.neostumbler.core

import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint

fun someWifiAccessPoint(macAddress: MacAddress = MacAddress("01:01:01:01:01:01")) =
    WifiAccessPoint(
        macAddress = macAddress,
        radioType = null,
        channel = null,
        frequency = null,
        signalStrength = null,
        ssid = "test_network",
    )
