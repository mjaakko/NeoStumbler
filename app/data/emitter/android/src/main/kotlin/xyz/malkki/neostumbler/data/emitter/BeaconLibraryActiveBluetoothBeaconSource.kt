package xyz.malkki.neostumbler.data.emitter

import android.content.Context
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.RangeNotifier
import org.altbeacon.beacon.Region
import timber.log.Timber
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.data.emitter.mapper.toBluetoothBeacon

class BeaconLibraryActiveBluetoothBeaconSource(context: Context) : ActiveBluetoothBeaconSource {
    private val appContext = context.applicationContext

    private fun getBeaconFlow(context: Context): Flow<Collection<Beacon>> = callbackFlow {
        val beaconManager = BeaconManager.getInstanceForApplication(context)

        val rangeNotifier = RangeNotifier { beacons: Collection<Beacon>, _: Region ->
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

    override fun getBluetoothBeaconFlow():
        Flow<List<EmitterObservation<BluetoothBeacon, MacAddress>>> =
        getBeaconFlow(appContext)
            .flowOn(
                // Beacon listener has to run on the main thread because of Android Beacon Library
                Dispatchers.Main
            )
            .map { beacons -> beacons.map { beacon -> beacon.toBluetoothBeacon() } }
            .flowOn(Dispatchers.Default)
}
