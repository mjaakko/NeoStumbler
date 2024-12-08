package xyz.malkki.neostumbler.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.StumblerApplication

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = getApplication<StumblerApplication>().reportDb

    val reportsTotal = db
        .flatMapLatest { it.reportDao().getReportCount() }
        .distinctUntilChanged()
    val reportsNotUploaded = db
        .flatMapLatest { it.reportDao().getReportCountNotUploaded() }
        .distinctUntilChanged()

    val reports = db
        .flatMapLatest {
            it.reportDao().getAllReportsWithStats()
        }
        .distinctUntilChanged()

    val lastUpload = db
        .flatMapLatest { it.reportDao().getLastUploadTime() }
        .distinctUntilChanged()

    fun deleteReport(reportId: Long) = viewModelScope.launch {
        db.value.reportDao().delete(reportId)
    }
}
