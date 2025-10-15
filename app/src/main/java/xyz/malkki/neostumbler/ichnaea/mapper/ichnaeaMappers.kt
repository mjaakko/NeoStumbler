package xyz.malkki.neostumbler.ichnaea.mapper

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.report.ReportEmitter
import xyz.malkki.neostumbler.core.report.ReportPosition
import xyz.malkki.neostumbler.ichnaea.dto.BluetoothBeaconDto
import xyz.malkki.neostumbler.ichnaea.dto.CellTowerDto
import xyz.malkki.neostumbler.ichnaea.dto.ReportDto.PositionDto
import xyz.malkki.neostumbler.ichnaea.dto.WifiAccessPointDto

fun ReportEmitter<BluetoothBeacon, MacAddress>.toDto(
    ageShift: Duration = 0.seconds
): BluetoothBeaconDto {
    return BluetoothBeaconDto(
        macAddress = emitter.macAddress.value,
        name = null,
        beaconType = emitter.beaconType,
        id1 = emitter.id1,
        id2 = emitter.id2,
        id3 = emitter.id3,
        signalStrength = emitter.signalStrength,
        age = age + ageShift.inWholeMilliseconds,
    )
}

fun ReportEmitter<CellTower, String>.toDto(ageShift: Duration = 0.seconds): CellTowerDto {
    return CellTowerDto(
        radioType = emitter.radioType.name.lowercase(),
        mobileCountryCode = emitter.mobileCountryCode?.toIntOrNull(),
        mobileCountryCodeStr = emitter.mobileCountryCode,
        mobileNetworkCode = emitter.mobileNetworkCode?.toIntOrNull(),
        mobileNetworkCodeStr = emitter.mobileNetworkCode,
        locationAreaCode = emitter.locationAreaCode,
        cellId = emitter.cellId,
        asu = emitter.asu,
        primaryScramblingCode = emitter.primaryScramblingCode,
        serving = emitter.serving,
        signalStrength = emitter.signalStrength,
        timingAdvance = emitter.timingAdvance,
        arfcn = emitter.arfcn,
        age = age + ageShift.inWholeMilliseconds,
    )
}

fun ReportEmitter<WifiAccessPoint, MacAddress>.toDto(
    ageShift: Duration = 0.seconds
): WifiAccessPointDto {
    return WifiAccessPointDto(
        macAddress = emitter.macAddress.value,
        radioType = emitter.radioType?.to802String(),
        ssid = emitter.ssid,
        channel = emitter.channel,
        frequency = emitter.frequency,
        signalStrength = emitter.signalStrength,
        signalToNoiseRatio = null,
        age = age + ageShift.inWholeMilliseconds,
    )
}

fun ReportPosition.toDto(ageShift: Duration = 0.seconds): PositionDto {
    return PositionDto(
        latitude = position.latitude,
        longitude = position.longitude,
        accuracy = position.accuracy?.takeUnless { it.isNaN() },
        age = age + ageShift.inWholeMilliseconds,
        altitude = position.altitude?.takeUnless { it.isNaN() },
        altitudeAccuracy = position.altitudeAccuracy?.takeUnless { it.isNaN() },
        heading = position.heading?.takeUnless { it.isNaN() },
        pressure = position.pressure?.takeUnless { it.isNaN() },
        speed = position.speed?.takeUnless { it.isNaN() },
        // Ichnaea Geosubmit officially only supports these sources
        // https://ichnaea.readthedocs.io/en/latest/api/geosubmit2.html#position-fields
        source =
            if (position.source == Position.Source.GPS) {
                "gps"
            } else {
                "fused"
            },
    )
}
