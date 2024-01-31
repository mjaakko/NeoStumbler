package xyz.malkki.neostumbler.utils.charts

import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.vertical.VerticalAxis
import com.patrykandpatrick.vico.core.chart.draw.ChartDrawContext
import com.patrykandpatrick.vico.core.context.MeasureContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

class MultiplesOfTenItemPlacer(val defaultItemPlacer: AxisItemPlacer.Vertical = AxisItemPlacer.Vertical.default()) : AxisItemPlacer.Vertical {
    override fun getLabelValues(
        context: ChartDrawContext,
        axisHeight: Float,
        maxLabelHeight: Float,
        position: AxisPosition.Vertical
    ): List<Float> {
        val maxValue = context.chartValuesProvider.getChartValues().maxY

        val labelCount = floor(axisHeight / maxLabelHeight).toInt()

        val step = (maxValue / labelCount).let {
            if (it <= 5.0) {
                5
            } else if (it <= 10.0) {
                10
            } else {
                val scale = 10.0.pow(floor(log10(it.toDouble()))).toInt()

                (ceil(it / scale) * scale).toInt()
            }
        }

        return (0..minOf(labelCount, (maxValue / step).toInt()))
            .mapIndexed { index, _ -> (index * step).toFloat() }
            .toList()
    }

    override fun getTopVerticalAxisInset(
        verticalLabelPosition: VerticalAxis.VerticalLabelPosition,
        maxLabelHeight: Float,
        maxLineThickness: Float
    ): Float {
        return defaultItemPlacer.getTopVerticalAxisInset(verticalLabelPosition, maxLabelHeight, maxLineThickness)
    }

    override fun getWidthMeasurementLabelValues(
        context: MeasureContext,
        axisHeight: Float,
        maxLabelHeight: Float,
        position: AxisPosition.Vertical
    ): List<Float> {
        return defaultItemPlacer.getWidthMeasurementLabelValues(context, axisHeight, maxLabelHeight, position)
    }

    override fun getBottomVerticalAxisInset(
        verticalLabelPosition: VerticalAxis.VerticalLabelPosition,
        maxLabelHeight: Float,
        maxLineThickness: Float
    ): Float {
        return defaultItemPlacer.getBottomVerticalAxisInset(
            verticalLabelPosition,
            maxLabelHeight,
            maxLineThickness
        )
    }

    override fun getHeightMeasurementLabelValues(
        context: MeasureContext,
        position: AxisPosition.Vertical
    ): List<Float> {
        return defaultItemPlacer.getHeightMeasurementLabelValues(context, position)
    }
}