package xyz.malkki.neostumbler.ichnaea

import xyz.malkki.neostumbler.ichnaea.dto.ReportDto

interface Geosubmit {
    suspend fun sendReports(reports: List<ReportDto>)
}
