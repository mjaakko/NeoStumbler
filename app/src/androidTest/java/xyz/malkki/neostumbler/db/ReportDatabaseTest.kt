package xyz.malkki.neostumbler.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import xyz.malkki.neostumbler.db.dao.CellTowerDao
import xyz.malkki.neostumbler.db.dao.PositionDao
import xyz.malkki.neostumbler.db.dao.ReportDao
import xyz.malkki.neostumbler.db.entities.CellTowerEntity
import xyz.malkki.neostumbler.db.entities.PositionEntity
import xyz.malkki.neostumbler.db.entities.Report
import xyz.malkki.neostumbler.extensions.copyTo
import xyz.malkki.neostumbler.extensions.getEstimatedSize
import xyz.malkki.neostumbler.extensions.getTableNames

@RunWith(AndroidJUnit4::class)
class ReportDatabaseTest {
    private lateinit var db: ReportDatabase

    private lateinit var reportDao: ReportDao
    private lateinit var positionDao: PositionDao
    private lateinit var cellTowerDao: CellTowerDao

    @Rule @JvmField val tmpFolder = TemporaryFolder()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        db = Room.inMemoryDatabaseBuilder(context, ReportDatabase::class.java).build()

        reportDao = db.reportDao()
        positionDao = db.positionDao()
        cellTowerDao = db.cellTowerDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testGettingTableNames() = runTest {
        val tableNames = db.openHelper.readableDatabase.getTableNames()

        assertTrue(tableNames.isNotEmpty())
    }

    private suspend fun RoomDatabase.addReport() {
        withTransaction {
            val reportId =
                reportDao.insert(
                    Report(
                        id = null,
                        timestamp = Instant.now(),
                        uploaded = false,
                        uploadTimestamp = null,
                    )
                )

            positionDao.insert(
                PositionEntity(
                    reportId = reportId,
                    id = null,
                    latitude = 78.2356,
                    longitude = 13.415,
                    accuracy = 500.0,
                    age = 1000,
                    altitude = null,
                    altitudeAccuracy = null,
                    heading = null,
                    speed = null,
                    pressure = null,
                    source = "gps",
                )
            )

            cellTowerDao.insertAll(
                CellTowerEntity(
                    reportId = reportId,
                    id = null,
                    radioType = "LTE",
                    mobileCountryCode = "111",
                    mobileNetworkCode = "01",
                    cellId = 451566,
                    locationAreaCode = 7,
                    asu = null,
                    primaryScramblingCode = 400,
                    serving = null,
                    signalStrength = -100,
                    timingAdvance = null,
                    arfcn = null,
                    age = 500,
                )
            )
        }
    }

    @Test
    fun testGettingDbSizeAfterInsertingData() = runTest {
        db.addReport()

        val estimatedDbSize = db.openHelper.readableDatabase.getEstimatedSize()
        assertNotEquals(0, estimatedDbSize)
    }

    // Note: this test requires min. Android 11
    @Test
    fun testCopyingDbToFile() = runTest {
        db.addReport()

        val tempFile = tmpFolder.newFile("db_export.db").toPath()

        db.openHelper.writableDatabase.copyTo(tempFile)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val exportedDb =
            Room.databaseBuilder(
                    context,
                    ReportDatabase::class.java,
                    tempFile.toAbsolutePath().toString(),
                )
                .build()

        try {
            val reportsWithLocation =
                exportedDb.reportDao().getAllReportsWithLocation().firstOrNull()
            assertNotNull(reportsWithLocation)
            assertEquals(1, reportsWithLocation?.size)
            assertEquals(78.2356, reportsWithLocation?.first()?.latitude)
            assertEquals(13.415, reportsWithLocation?.first()?.longitude)
        } finally {
            exportedDb.close()
        }
    }
}
