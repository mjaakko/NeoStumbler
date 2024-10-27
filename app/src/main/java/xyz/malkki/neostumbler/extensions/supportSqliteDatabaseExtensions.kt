package xyz.malkki.neostumbler.extensions

import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Returns the estimated size of the database in bytes
 */
suspend fun SupportSQLiteDatabase.getEstimatedSize(): Long = withContext(Dispatchers.IO) {
    query("SELECT page_count * page_size FROM pragma_page_count, pragma_page_size")
        .use { cursor ->
            if (!cursor.moveToFirst()) {
                throw IOException()
            }

            cursor.getLong(0)
        }
}

/**
 * Returns all table names without including metadata tables
 */
suspend fun SupportSQLiteDatabase.getTableNames(): Collection<String> = withContext(Dispatchers.IO) {
    query("SELECT name FROM sqlite_master WHERE type = 'table' AND name != 'android_metadata' AND name != 'sqlite_sequence' AND name != 'room_master_table'")
        .use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0))
                }
            }
        }
}