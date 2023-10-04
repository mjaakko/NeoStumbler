package xyz.malkki.wifiscannerformls.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.distinctUntilChanged
import xyz.malkki.wifiscannerformls.WifiScannerApplication

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = getApplication<WifiScannerApplication>().reportDb

    val reportsTotal = db.reportDao().getReportCount().distinctUntilChanged()
    val reportsNotUploaded = db.reportDao().getReportCountNotUploaded().distinctUntilChanged()

    val reports = db.reportDao().getAllReportsWithWifiAccessPointCount().distinctUntilChanged()

    val lastUpload = db.reportDao().getLastUploadTime().distinctUntilChanged()
}