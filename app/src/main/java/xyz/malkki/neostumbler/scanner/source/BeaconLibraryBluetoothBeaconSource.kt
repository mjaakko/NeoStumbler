package xyz.malkki.neostumbler.scanner.source

import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import xyz.malkki.neostumbler.domain.BluetoothBeacon
import kotlin.random.Random

class BeaconLibraryBluetoothBeaconSource(context: Context) : BluetoothBeaconSource {
    private val appContext = context.applicationContext

    private fun getBeaconFlow(context: Context): Flow<List<Beacon>> = callbackFlow {
        val beaconManager = BeaconManager.getInstanceForApplication(context)

        val rangeNotifier: (Collection<Beacon>, Region) -> Unit = { beacons: Collection<Beacon>, _: Region ->
            trySendBlocking(beacons.toList())
        }

        beaconManager.addRangeNotifier(rangeNotifier)

        val region = Region("all_beacons_${Random.Default.nextInt(0, Int.MAX_VALUE)}", null, null, null)

        beaconManager.startRangingBeacons(region)

        awaitClose {
            beaconManager.stopRangingBeacons(region)

            beaconManager.removeRangeNotifier(rangeNotifier)
        }
    }

    override fun getBluetoothBeaconFlow(): Flow<List<BluetoothBeacon>> = getBeaconFlow(appContext).map { beacons ->
        beacons.map { beacon ->
            BluetoothBeacon.fromBeacon(beacon)
        }
    }
}