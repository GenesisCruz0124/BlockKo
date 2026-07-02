package com.blockko.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockko.app.data.db.DayCount
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Renders a 7-day trend of blocked-query counts. [dailyCounts] uses epoch-day keys. */
@Composable
fun BlockedPerDayChart(dailyCounts: List<DayCount>, modifier: Modifier = Modifier) {
    val entryProducer = remember { ChartEntryModelProducer() }
    val byDay = remember(dailyCounts) { dailyCounts.associateBy { it.dayEpoch } }
    val today = System.currentTimeMillis() / TimeUnit.DAYS.toMillis(1)
    val last7Days = remember(today) { (6 downTo 0).map { today - it } }

    LaunchedEffect(byDay) {
        val entries = last7Days.mapIndexed { index, dayEpoch ->
            entryOf(index.toFloat(), (byDay[dayEpoch]?.count ?: 0).toFloat())
        }
        entryProducer.setEntries(entries)
    }

    val dayFormatter = remember { SimpleDateFormat("EEE", Locale.getDefault()) }

    Chart(
        chart = lineChart(),
        chartModelProducer = entryProducer,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(
            valueFormatter = { value, _ ->
                val index = value.toInt().coerceIn(0, last7Days.lastIndex)
                dayFormatter.format(last7Days[index] * TimeUnit.DAYS.toMillis(1))
            }
        ),
        modifier = modifier.fillMaxWidth().height(200.dp)
    )
}
