package xyz.malkki.neostumbler.data.reports

import kotlinx.coroutines.flow.Flow

interface ReportStorageMetadataProvider {
    /** Returns the estimated size of the database in bytes */
    fun getEstimatedSize(): Flow<Long>

    fun getSchemaVersion(): Int
}
