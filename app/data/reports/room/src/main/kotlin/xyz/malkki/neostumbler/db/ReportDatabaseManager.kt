package xyz.malkki.neostumbler.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.copyTo
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

class ReportDatabaseManager(context: Context) {
    companion object {
        private const val DATABASE_NAME = "report-db"

        /**
         * Validates that the file contains valid report database
         *
         * @param dbFile File that contains the database
         * @return true if the file contains valid report database
         */
        // If an generic exception is caught, it's assumed that the database is not valid
        @Suppress("TooGenericExceptionCaught")
        internal suspend fun validateDatabase(context: Context, dbFile: Path): Boolean =
            withContext(Dispatchers.IO) {
                val tempFile = createTempFile(context.cacheDir.toPath(), "temp", ".db")
                var db: ReportDatabase? = null

                return@withContext try {
                    dbFile.copyTo(tempFile, StandardCopyOption.REPLACE_EXISTING)

                    db =
                        Room.databaseBuilder(
                                context.applicationContext,
                                ReportDatabase::class.java,
                                tempFile.toAbsolutePath().toString(),
                            )
                            .build()

                    db.openHelper.writableDatabase.query("PRAGMA integrity_check").use { cursor ->
                        if (!cursor.moveToFirst() || !cursor.getString(0).contains("ok")) {
                            return@withContext false
                        }
                    }

                    /*
                     * Check that the database contains at least one report.
                     * While an empty database is technically valid, it makes no sense to import such
                     */
                    db.reportDao().getReportCount().first() > 0
                } catch (ex: Exception) {
                    Timber.w(ex, "Exception while validating database, assuming it to be invalid")

                    false
                } finally {
                    db?.close()
                    tempFile.deleteIfExists()
                }
            }
    }

    private val appContext = context.applicationContext

    private val _reportDb by lazy { MutableStateFlow(createDatabaseInstance()) }

    internal val reportDb: StateFlow<ReportDatabase>
        get() = _reportDb.asStateFlow()

    suspend fun importDb(dbFile: Path) =
        withContext(Dispatchers.IO) {
            val oldDb = reportDb.value
            val oldDbPath = Paths.get(oldDb.openHelper.writableDatabase.path!!)

            // Close old database instance (this will also delete WAL and SHM files)
            oldDb.close()

            // Copy imported file to the same location as the old database
            dbFile.copyTo(oldDbPath, StandardCopyOption.REPLACE_EXISTING)

            _reportDb.value = createDatabaseInstance()
        }

    private fun createDatabaseInstance(): ReportDatabase {
        return Room.databaseBuilder(appContext, ReportDatabase::class.java, DATABASE_NAME)
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onOpen(connection: SQLiteConnection) {
                        super.onOpen(connection)

                        /**
                         * Sending reports fails when there are reports without a position. This
                         * might be caused e.g. when the insertion is not completed fully
                         *
                         * -> Let's fix this by removing these broken reports
                         */
                        connection.execSQL(
                            "DELETE FROM Report WHERE id NOT IN (SELECT reportId FROM PositionEntity)"
                        )
                    }
                }
            )
            .build()
    }
}
