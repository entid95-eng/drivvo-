package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.LogRecord
import com.example.model.RecordType
import com.example.model.calculateVehicleStats
import com.example.ui.GarageDashboardData
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.ExpenseIndigo
import com.example.ui.theme.FuelAmber
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ServiceEmerald
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(data: GarageDashboardData) {
    var filterVehicleId by remember { mutableStateOf("ALL") }
    val numFormat = remember { NumberFormat.getIntegerInstance(Locale("ru")) }

    val scopedRecords = remember(data.records, filterVehicleId) {
        if (filterVehicleId == "ALL") data.records
        else data.records.filter { it.vehicleId == filterVehicleId }
    }
    val stats = remember(scopedRecords) { calculateVehicleStats(scopedRecords) }
    val totalLiters = remember(scopedRecords) {
        scopedRecords.filter { it.safeRecordType() == RecordType.REFUEL }.sumOf { it.liters }
    }

    val monthlyBuckets = remember(scopedRecords) { buildLast6MonthsBuckets(scopedRecords) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Отчёты и графики", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Аналитика затрат и расхода топлива в стиле Drivvo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (data.vehicles.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterVehicleId == "ALL",
                        onClick = { filterVehicleId = "ALL" },
                        label = { Text("Весь гараж") }
                    )
                    data.vehicles.forEach { v ->
                        FilterChip(
                            selected = filterVehicleId == v.id,
                            onClick = { filterVehicleId = v.id },
                            label = { Text(v.displayTitle()) }
                        )
                    }
                }
            }
        }

        // KPI Grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnalyticsMetricCard(
                    title = "Всего затрат",
                    value = numFormat.format(stats.totalSpent.toLong()),
                    subtitle = "${scopedRecords.size} записей",
                    color = CobaltPrimary,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricCard(
                    title = "Средний расход",
                    value = stats.avgLitersPer100Km?.let { String.format(Locale.US, "%.1f л", it) } ?: "—",
                    subtitle = "на 100 км • ${String.format(Locale.US, "%.0f", totalLiters)} л",
                    color = FuelAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Donut Breakdown Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Структура расходов", style = MaterialTheme.typography.titleMedium)
                    if (stats.totalSpent <= 0.0) {
                        Text(
                            "Добавьте заправки, ТО или прочие расходы, чтобы увидеть диаграмму.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            DonutChart(
                                fuel = stats.fuelSpent,
                                service = stats.serviceSpent,
                                expense = stats.expenseSpent,
                                modifier = Modifier.size(130.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                LegendItem("Топливо", stats.fuelSpent, stats.totalSpent, FuelAmber, numFormat)
                                LegendItem("Сервис / ТО", stats.serviceSpent, stats.totalSpent, ServiceEmerald, numFormat)
                                LegendItem("Расходы", stats.expenseSpent, stats.totalSpent, ExpenseIndigo, numFormat)
                            }
                        }
                    }
                }
            }
        }

        // 6-Month Stacked Bar Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Динамика за 6 месяцев", style = MaterialTheme.typography.titleMedium)
                    val maxTotal = monthlyBuckets.maxOfOrNull { it.fuel + it.service + it.expense } ?: 0.0
                    if (maxTotal <= 0.0) {
                        Text(
                            "Недостаточно данных за последние месяцы.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        StackedBarChart(
                            buckets = monthlyBuckets,
                            maxTotal = maxTotal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            monthlyBuckets.forEach { b ->
                                Text(
                                    text = b.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsMetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = color
            )
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DonutChart(
    fuel: Double,
    service: Double,
    expense: Double,
    modifier: Modifier = Modifier
) {
    val total = (fuel + service + expense).coerceAtLeast(0.001)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier) {
        val strokeWidth = 22.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth)
        )

        var startAngle = -90f
        listOf(
            fuel to FuelAmber,
            service to ServiceEmerald,
            expense to ExpenseIndigo
        ).forEach { (value, color) ->
            if (value > 0) {
                val sweep = ((value / total) * 360.0).toFloat()
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    amount: Double,
    total: Double,
    color: Color,
    numFormat: NumberFormat
) {
    val pct = if (total > 0) ((amount / total) * 100).toInt() else 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = "${numFormat.format(amount.toLong())} ($pct%)",
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

private data class MonthBucket(
    val key: String,
    val label: String,
    val fuel: Double = 0.0,
    val service: Double = 0.0,
    val expense: Double = 0.0
)

private fun buildLast6MonthsBuckets(records: List<LogRecord>): List<MonthBucket> {
    val keyFmt = SimpleDateFormat("yyyy-MM", Locale.US)
    val labelFmt = SimpleDateFormat("MMM", Locale("ru"))
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.add(Calendar.MONTH, -5)

    val buckets = (0..5).map { idx ->
        val d = cal.time
        val item = MonthBucket(key = keyFmt.format(d), label = labelFmt.format(d).take(3))
        cal.add(Calendar.MONTH, 1)
        item
    }.associateBy { it.key }.toMutableMap()

    records.forEach { r ->
        val k = keyFmt.format(Date(r.eventTimestampMs))
        val cur = buckets[k] ?: return@forEach
        buckets[k] = when (r.safeRecordType()) {
            RecordType.REFUEL -> cur.copy(fuel = cur.fuel + r.amountCost)
            RecordType.SERVICE -> cur.copy(service = cur.service + r.amountCost)
            RecordType.EXPENSE -> cur.copy(expense = cur.expense + r.amountCost)
        }
    }
    return buckets.values.toList()
}

@Composable
private fun StackedBarChart(
    buckets: List<MonthBucket>,
    maxTotal: Double,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    Canvas(modifier = modifier) {
        val barAreaHeight = size.height - 8.dp.toPx()
        val count = buckets.size.coerceAtLeast(1)
        val slotWidth = size.width / count
        val barWidth = (slotWidth * 0.45f).coerceAtMost(32.dp.toPx())

        drawLine(
            color = gridColor,
            start = Offset(0f, barAreaHeight),
            end = Offset(size.width, barAreaHeight),
            strokeWidth = 1.dp.toPx()
        )

        buckets.forEachIndexed { idx, bucket ->
            val x = idx * slotWidth + (slotWidth - barWidth) / 2f
            var yBottom = barAreaHeight
            listOf(
                bucket.fuel to FuelAmber,
                bucket.service to ServiceEmerald,
                bucket.expense to ExpenseIndigo
            ).forEach { (amount, color) ->
                if (amount > 0 && maxTotal > 0) {
                    val h = ((amount / maxTotal) * barAreaHeight).toFloat().coerceAtLeast(4f)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, yBottom - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    yBottom -= h
                }
            }
        }
    }
}
