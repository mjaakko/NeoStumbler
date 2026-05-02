package xyz.malkki.neostumbler.crashlog

import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface CrashLogManager {
    suspend fun clearEntries()

    fun getEntries(): Flow<List<CrashLogEntry>>

    suspend fun deleteEntry(entryId: String)

    suspend fun getEntryContent(entryId: String): CrashLogEntryContent?
}

data class CrashLogEntry(val id: String, val timestamp: Instant)

data class CrashLogEntryContent(val entry: CrashLogEntry, val content: String)
