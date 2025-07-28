package xyz.malkki.neostumbler.data.reports

import java.nio.file.Path

interface RawReportImportExport {
    /**
     * Validates whether the raw report storage file can be imported with [importRawReports]
     *
     * @return `true` if the file can be imported
     */
    suspend fun validateRawReports(file: Path): Boolean

    suspend fun importRawReports(file: Path)

    /**
     * Exports raw report storage file to the given path
     *
     * @param file Path where to export the storage file
     */
    suspend fun exportRawReports(file: Path)
}
