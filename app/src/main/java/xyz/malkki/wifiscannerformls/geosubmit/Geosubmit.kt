package xyz.malkki.wifiscannerformls.geosubmit

interface Geosubmit {
    suspend fun sendReports(reports: List<Report>)
}