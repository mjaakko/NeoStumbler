package xyz.malkki.neostumbler.scanner.postprocess

import xyz.malkki.neostumbler.core.report.ReportData

/**
 * Post-processes a report, e.g. by filtering its data
 *
 * Postprocessors must not assume that they are executed in a specific order
 */
fun interface ReportPostProcessor {
    fun postProcessReport(reportData: ReportData): ReportData?
}
