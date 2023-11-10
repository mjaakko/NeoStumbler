package xyz.malkki.neostumbler.geosubmit

interface Geosubmit {
    suspend fun sendReports(reports: List<Report>)
}