package xyz.malkki.neostumbler.export

import java.time.Instant

interface CsvExportManager {
    fun startExport(fromInstant: Instant, toInstant: Instant, outputFile: String)
}
