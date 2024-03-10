package xyz.malkki.neostumbler.scanner.source

import android.telephony.CellInfo
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface CellInfoSource {
    fun getCellInfoFlow(interval: Duration): Flow<List<CellInfo>>
}