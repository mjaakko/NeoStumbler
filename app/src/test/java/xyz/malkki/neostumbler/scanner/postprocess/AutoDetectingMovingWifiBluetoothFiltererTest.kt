package xyz.malkki.neostumbler.scanner.postprocess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.malkki.neostumbler.domain.BluetoothBeacon
import xyz.malkki.neostumbler.domain.Position
import xyz.malkki.neostumbler.domain.WifiAccessPoint
import xyz.malkki.neostumbler.scanner.data.ReportData

class AutoDetectingMovingWifiBluetoothFiltererTest {
    @Test
    fun `Test moving Wi-Fi and Bluetooth are filtered`() {
        val filterer = AutoDetectingMovingWifiBluetoothFilterer(maxDistanceFromExisting = 500.0)

        val position1 =
            Position(
                latitude = 40.689100,
                longitude = -74.044300,
                timestamp = 0,
                source = Position.Source.GPS,
            )

        val report1 =
            ReportData(
                position = position1,
                cellTowers = emptyList(),
                wifiAccessPoints =
                    listOf(
                        WifiAccessPoint(macAddress = "01:01:01:01:01:01", timestamp = 0),
                        WifiAccessPoint(macAddress = "02:02:02:02:02:02", timestamp = 0),
                    ),
                bluetoothBeacons =
                    listOf(BluetoothBeacon(macAddress = "04:04:04:04:04:04", timestamp = 0)),
            )

        filterer.postProcessReport(report1)

        val afterMoving = position1.latLng.destination(700.0, 43.15)

        val position2 =
            Position(
                latitude = afterMoving.latitude,
                longitude = afterMoving.longitude,
                timestamp = 0,
                source = Position.Source.GPS,
            )

        val report2 = report1.copy(position = position2)

        val filteredReport2 = filterer.postProcessReport(report2)

        assertEquals(0, filteredReport2?.wifiAccessPoints?.size)
        assertEquals(0, filteredReport2?.bluetoothBeacons?.size)

        // Test processing the first report again -> content should get filtered
        val filteredReport1 = filterer.postProcessReport(report1)

        assertEquals(0, filteredReport1?.wifiAccessPoints?.size)
        assertEquals(0, filteredReport1?.bluetoothBeacons?.size)
    }

    @Test
    fun `Test stationary Wi-Fi is not filtered`() {
        val filterer = AutoDetectingMovingWifiBluetoothFilterer(maxDistanceFromExisting = 500.0)

        val position1 =
            Position(
                latitude = 40.689100,
                longitude = -74.044300,
                timestamp = 0,
                source = Position.Source.GPS,
            )

        val report1 =
            ReportData(
                position = position1,
                cellTowers = emptyList(),
                wifiAccessPoints =
                    listOf(
                        WifiAccessPoint(macAddress = "01:01:01:01:01:01", timestamp = 0),
                        WifiAccessPoint(macAddress = "02:02:02:02:02:02", timestamp = 0),
                    ),
                bluetoothBeacons = emptyList(),
            )

        filterer.postProcessReport(report1)

        val afterMoving = position1.latLng.destination(700.0, 43.15)

        val position2 =
            Position(
                latitude = afterMoving.latitude,
                longitude = afterMoving.longitude,
                timestamp = 0,
                source = Position.Source.GPS,
            )

        val report2 =
            ReportData(
                position = position2,
                cellTowers = emptyList(),
                bluetoothBeacons = emptyList(),
                wifiAccessPoints =
                    listOf(
                        WifiAccessPoint(macAddress = "03:03:03:03:03:03", timestamp = 0),
                        WifiAccessPoint(macAddress = "04:04:04:04:04:04", timestamp = 0),
                    ),
            )

        val filteredReport = filterer.postProcessReport(report2)

        assertEquals(2, filteredReport?.wifiAccessPoints?.size)
        assertTrue(
            filteredReport?.wifiAccessPoints?.any { it.macAddress == "03:03:03:03:03:03" } == true
        )
    }
}
