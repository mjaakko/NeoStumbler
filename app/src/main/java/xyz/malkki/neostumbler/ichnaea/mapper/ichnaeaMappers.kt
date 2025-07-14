package xyz.malkki.neostumbler.ichnaea.mapper

import xyz.malkki.neostumbler.db.entities.BluetoothBeaconEntity
import xyz.malkki.neostumbler.db.entities.CellTowerEntity
import xyz.malkki.neostumbler.db.entities.PositionEntity
import xyz.malkki.neostumbler.db.entities.WifiAccessPointEntity
import xyz.malkki.neostumbler.ichnaea.dto.BluetoothBeaconDto
import xyz.malkki.neostumbler.ichnaea.dto.CellTowerDto
import xyz.malkki.neostumbler.ichnaea.dto.ReportDto.PositionDto
import xyz.malkki.neostumbler.ichnaea.dto.WifiAccessPointDto

fun BluetoothBeaconEntity.toDto(): BluetoothBeaconDto {
    return BluetoothBeaconDto(
        macAddress = macAddress,
        name = name,
        beaconType = beaconType,
        id1 = id1,
        id2 = id2,
        id3 = id3,
        age = age,
        signalStrength = signalStrength,
    )
}

fun CellTowerEntity.toDto(): CellTowerDto {
    return CellTowerDto(
        radioType = radioType,
        mobileCountryCode = mobileCountryCode?.toIntOrNull(),
        mobileCountryCodeStr = mobileCountryCode,
        mobileNetworkCode = mobileNetworkCode?.toIntOrNull(),
        mobileNetworkCodeStr = mobileNetworkCode,
        locationAreaCode = locationAreaCode,
        cellId = cellId,
        age = age,
        asu = asu,
        primaryScramblingCode = primaryScramblingCode,
        serving = serving,
        signalStrength = signalStrength,
        timingAdvance = timingAdvance,
        arfcn = arfcn,
    )
}

fun WifiAccessPointEntity.toDto(): WifiAccessPointDto {
    return WifiAccessPointDto(
        macAddress = macAddress,
        radioType = radioType,
        age = age,
        channel = channel,
        frequency = frequency,
        signalStrength = signalStrength,
        signalToNoiseRatio = signalToNoiseRatio,
        ssid = ssid,
    )
}

fun PositionEntity.toDto(): PositionDto {
    // Ichnaea Geosubmit officially only supports these sources
    // https://ichnaea.readthedocs.io/en/latest/api/geosubmit2.html#position-fields
    val source =
        if (source == "gps") {
            "gps"
        } else {
            "fused"
        }

    return PositionDto(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy?.takeUnless { it.isNaN() },
        age = age,
        altitude = altitude?.takeUnless { it.isNaN() },
        altitudeAccuracy = altitudeAccuracy?.takeUnless { it.isNaN() },
        heading = heading?.takeUnless { it.isNaN() },
        pressure = pressure?.takeUnless { it.isNaN() },
        speed = speed?.takeUnless { it.isNaN() },
        source = source,
    )
}
