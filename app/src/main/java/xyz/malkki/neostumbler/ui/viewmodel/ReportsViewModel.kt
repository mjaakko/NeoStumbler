package xyz.malkki.neostumbler.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.db.ReportDatabaseManager
import xyz.malkki.neostumbler.db.entities.ReportWithStats

class ReportsViewModel(reportDatabaseManager: ReportDatabaseManager) : ViewModel() {
    private val db = reportDatabaseManager.reportDb

    val reportsTotal = db
        .flatMapLatest { it.reportDao().getReportCount() }
        .distinctUntilChanged()
    val reportsNotUploaded = db
        .flatMapLatest { it.reportDao().getReportCountNotUploaded() }
        .distinctUntilChanged()

    val reports: Flow<PagingData<ReportWithStats>> = db
        .flatMapLatest {
            Pager(PagingConfig(pageSize = 40, prefetchDistance = 5)) {
                it.reportDao().getAllReportsWithStats()
            }.flow
        }

    val lastUpload = db
        .flatMapLatest { it.reportDao().getLastUploadTime() }
        .distinctUntilChanged()

    fun deleteReport(reportId: Long) = viewModelScope.launch {
        db.value.reportDao().delete(reportId)
    }
}
