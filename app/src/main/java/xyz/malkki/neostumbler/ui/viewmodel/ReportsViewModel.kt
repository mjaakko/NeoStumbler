package xyz.malkki.neostumbler.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.StumblerApplication

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = getApplication<StumblerApplication>().reportDb

    val reportsTotal = db.reportDao().getReportCount().distinctUntilChanged()
    val reportsNotUploaded = db.reportDao().getReportCountNotUploaded().distinctUntilChanged()

    val reports = db.reportDao().getAllReportsWithStats().distinctUntilChanged()

    val lastUpload = db.reportDao().getLastUploadTime().distinctUntilChanged()

    fun deleteReport(reportId: Long) = viewModelScope.launch {
        db.reportDao().delete(reportId)
    }
}
