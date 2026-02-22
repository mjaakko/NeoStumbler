package xyz.malkki.neostumbler.report.postprocessor

fun interface ReportPostProcessorProvider {
    suspend fun getReportPostProcessors(): Collection<ReportPostProcessor>
}
