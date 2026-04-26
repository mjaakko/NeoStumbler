package xyz.malkki.neostumbler.report.postprocessor

import xyz.malkki.neostumbler.core.report.ReportData
import xyz.malkki.neostumbler.geography.Circle
import xyz.malkki.neostumbler.geography.Circle.Companion.isInside

/** Filters reports created within areas returned by [restrictedAreasProvider] */
class RestrictedAreaFilterer(
    private val restrictedAreasProvider: suspend () -> Collection<Circle>
) : ReportPostProcessor {
    override suspend fun postProcessReport(reportData: ReportData): ReportData? {
        val restrictedAreas = restrictedAreasProvider()

        return reportData.takeIf {
            restrictedAreas.none { restrictedArea ->
                it.position.position.latLng.isInside(restrictedArea)
            }
        }
    }
}
