package xyz.malkki.neostumbler.extensions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Returns the estimated size of the database in bytes */
suspend fun SupportSQLiteDatabase.getEstimatedSize(): Long =
    withContext(Dispatchers.IO) {
        query("SELECT page_count * page_size FROM pragma_page_count, pragma_page_size").use { cursor
            ->
            if (!cursor.moveToFirst()) {
                throw IOException()
            }

            cursor.getLong(0)
        }
    }

/** Returns all table names without including metadata tables */
suspend fun SupportSQLiteDatabase.getTableNames(): Collection<String> =
    withContext(Dispatchers.IO) {
        query(
                """
                    SELECT name FROM sqlite_master
                        WHERE type = 'table'
                            AND name != 'android_metadata'
                            AND name != 'sqlite_sequence'
                            AND name != 'room_master_table'
                    """
            )
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(0))
                    }
                }
            }
    }

/**
 * Copies database contents to the specified file
 *
 * @param target File where the contents are copied to
 */
suspend fun SupportSQLiteDatabase.copyTo(target: Path) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        copyToR(target)
    } else {
        copyToLegacy(target)
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private suspend fun SupportSQLiteDatabase.copyToR(target: Path) =
    withContext(Dispatchers.IO) {
        // Android 11+ supports VACUUM INTO
        query("VACUUM main INTO '${target.toAbsolutePath()}'").use { it.moveToFirst() }
    }

private suspend fun SupportSQLiteDatabase.copyToLegacy(target: Path) =
    withContext(Dispatchers.IO) {
        // First, create a WAL checkpoint to make sure that all data is in the main database file
        query("PRAGMA wal_checkpoint(full)").use { it.moveToFirst() }

        ensureActive()

        // Then copy the database file to the target
        Paths.get(path).copyTo(target, overwrite = true)
    }
