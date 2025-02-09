package xyz.malkki.neostumbler.scanner.source

import android.content.Context
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import timber.log.Timber
import xyz.malkki.neostumbler.domain.BluetoothBeacon
import xyz.malkki.neostumbler.extensions.buffer

private val BEACON_BUFFER_WINDOW = 10.seconds

class BeaconLibraryBluetoothBeaconSource(context: Context) : BluetoothBeaconSource {
    private val appContext = context.applicationContext

    private fun getBeaconFlow(context: Context): Flow<Collection<Beacon>> = callbackFlow {
        val beaconManager = BeaconManager.getInstanceForApplication(context)

        val rangeNotifier: (Collection<Beacon>, Region) -> Unit =
            { beacons: Collection<Beacon>, _: Region ->
                trySendBlocking(beacons)
            }

        beaconManager.addRangeNotifier(rangeNotifier)

        val region =
            Region("all_beacons_${Random.Default.nextInt(0, Int.MAX_VALUE)}", null, null, null)

        @Suppress("TooGenericExceptionCaught")
        try {
            beaconManager.startRangingBeacons(region)
        } catch (ex: Exception) {
            /**
             * Beacon scanning can cause a crash if the beacon service has been disabled
             *
             * This can happen e.g. when a custom ROM is used, see:
             * https://github.com/mjaakko/NeoStumbler/issues/272
             */
            Timber.w(ex, "Failed to start scanning Bluetooth beacons")
        }

        awaitClose {
            beaconManager.stopRangingBeacons(region)

            beaconManager.removeRangeNotifier(rangeNotifier)
        }
    }

    override fun getBluetoothBeaconFlow(): Flow<List<BluetoothBeacon>> =
        getBeaconFlow(appContext)
            .flowOn(
                // Beacon listener has to run on the main thread because of Android Beacon Library
                Dispatchers.Main
            )
            .map { beacons -> beacons.map { beacon -> BluetoothBeacon.fromBeacon(beacon) } }
            /*
             * Beacon library can give us the same beacon many times in a short succession
             * To avoid creating a lot of reports with just a single beacon,
             * buffer them here and only publish the latest data by MAC address
             */
            .buffer(BEACON_BUFFER_WINDOW)
            .map { beacons ->
                beacons
                    .flatten()
                    .groupingBy { it.macAddress }
                    .reduce { _, a, b ->
                        maxOf(a, b, Comparator.comparingLong(BluetoothBeacon::timestamp))
                    }
                    .values
                    .toList()
            }
            .flowOn(Dispatchers.Default)
}
