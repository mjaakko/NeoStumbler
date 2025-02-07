package xyz.malkki.neostumbler.geosubmit

import xyz.malkki.neostumbler.geosubmit.dto.ReportDto

interface Geosubmit {
    suspend fun sendReports(reports: List<ReportDto>)
}
