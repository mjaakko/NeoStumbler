package xyz.malkki.neostumbler.crashlog

import kotlinx.coroutines.flow.Flow

interface CrashLogManager {
    suspend fun clearEntries()

    fun getEntries(): Flow<List<String>>

    suspend fun deleteEntry(entry: String)

    suspend fun getLogsForEntry(entry: String): String?
}
