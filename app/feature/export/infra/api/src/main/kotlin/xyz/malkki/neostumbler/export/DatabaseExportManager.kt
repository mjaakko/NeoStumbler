package xyz.malkki.neostumbler.export

interface DatabaseExportManager {
    fun startExportRawDatabase(outputFile: String, compress: Boolean)
}
