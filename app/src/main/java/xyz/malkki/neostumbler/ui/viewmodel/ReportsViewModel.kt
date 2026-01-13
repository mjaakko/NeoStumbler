package xyz.malkki.neostumbler.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.core.report.ReportWithStats
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.data.reports.ReportRemover

class ReportsViewModel(reportProvider: ReportProvider, private val reportRemover: ReportRemover) :
    ViewModel() {
    val reportsTotal = reportProvider.getReportCount().distinctUntilChanged()
    val reportsNotUploaded = reportProvider.getNotUploadedReportCount().distinctUntilChanged()
    val lastUpload = reportProvider.getLastReportUploadTime().distinctUntilChanged()

    val reports: Flow<PagingData<ReportWithStats>> =
        reportProvider.getReportsWithStats().cachedIn(viewModelScope)

    fun deleteReport(reportId: Long) =
        viewModelScope.launch { reportRemover.deleteReport(reportId) }
}
