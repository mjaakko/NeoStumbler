package xyz.malkki.neostumbler.utils.charts

import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.chart.dimensions.HorizontalDimensions
import com.patrykandpatrick.vico.core.chart.draw.ChartDrawContext
import com.patrykandpatrick.vico.core.context.MeasureContext
import kotlin.math.ceil
import kotlin.math.round

/**
 * Helper for placing text labels more nicely on the x-axis. Hacky solution, might not work for all screen sizes etc..
 */
class TextLabelItemPlacer(private val textWidthSp: Float = 60f, private val defaultItemPlacer: AxisItemPlacer.Horizontal = AxisItemPlacer.Horizontal.default()) : AxisItemPlacer.Horizontal {
    override fun getAddFirstLabelPadding(context: MeasureContext): Boolean = defaultItemPlacer.getAddFirstLabelPadding(context)

    override fun getAddLastLabelPadding(context: MeasureContext): Boolean = defaultItemPlacer.getAddLastLabelPadding(context)

    override fun getEndHorizontalAxisInset(
        context: MeasureContext,
        horizontalDimensions: HorizontalDimensions,
        tickThickness: Float
    ): Float = defaultItemPlacer.getEndHorizontalAxisInset(context, horizontalDimensions, tickThickness)

    override fun getLabelValues(
        context: ChartDrawContext,
        visibleXRange: ClosedFloatingPointRange<Float>,
        fullXRange: ClosedFloatingPointRange<Float>
    ): List<Float> {
        val textWidth = context.spToPx(textWidthSp)
        val countByTextWidth = (context.chartBounds.width() / textWidth).toInt()

        val rangePct = (fullXRange.endInclusive - fullXRange.start) / (visibleXRange.endInclusive - visibleXRange.start)

        val labels = defaultItemPlacer.getLabelValues(context, visibleXRange, fullXRange)
        return if (labels.size <= countByTextWidth || rangePct >= 1.5) {
            labels
        } else {
            val rangeDivider = if (rangePct >= 1.0) {
                1
            } else {
                round(1 / rangePct).toInt()
            }

            val labelsAfterRangeFilter = labels.filterIndexed { index, _ -> index % rangeDivider == 0 }
            val divider = ceil((labelsAfterRangeFilter.size.toDouble() / countByTextWidth)).toInt()

            return labelsAfterRangeFilter.filterIndexed { index, _ -> index % divider == 0 }
        }
    }

    override fun getLineValues(
        context: ChartDrawContext,
        visibleXRange: ClosedFloatingPointRange<Float>,
        fullXRange: ClosedFloatingPointRange<Float>
    ): List<Float>? = defaultItemPlacer.getLineValues(context, visibleXRange, fullXRange)

    override fun getMeasuredLabelValues(
        context: MeasureContext,
        horizontalDimensions: HorizontalDimensions,
        fullXRange: ClosedFloatingPointRange<Float>
    ): List<Float> = defaultItemPlacer.getMeasuredLabelValues(context, horizontalDimensions, fullXRange)

    override fun getStartHorizontalAxisInset(
        context: MeasureContext,
        horizontalDimensions: HorizontalDimensions,
        tickThickness: Float
    ): Float = defaultItemPlacer.getStartHorizontalAxisInset(context, horizontalDimensions, tickThickness)
}