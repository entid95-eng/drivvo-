package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.LogRecord
import com.example.model.RecordType
import com.example.model.ReminderTask
import com.example.model.TaskPriority
import com.example.model.TaskUrgency
import com.example.model.Vehicle
import com.example.ui.GarageDashboardData
import com.example.ui.silhouettes.VehicleSilhouette
import com.example.ui.silhouettes.parseHexColor
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.ExpenseIndigo
import com.example.ui.theme.FuelAmber
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ServiceEmerald
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    data: GarageDashboardData,
    onSelectVehicle: (String) -> Unit,
    onAddVehicle: () -> Unit,
    onEditVehicle: (Vehicle) -> Unit,
    onQuickOdometer: (Vehicle) -> Unit,
    onEditRecord: (LogRecord) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onToggleTask: (ReminderTask, Boolean, Long) -> Unit,
    onOpenTasksTab: () -> Unit,
    onQuickAddRecord: (RecordType) -> Unit
) {
    var filterType by remember { mutableStateOf<RecordType?>(null) }
    val numFormat = remember { NumberFormat.getIntegerInstance(Locale("ru")) }
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale("ru")) }

    if (data.vehicles.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    VehicleSilhouette(
                        vehicleClass = com.example.model.VehicleClass.CROSSOVER,
                        iconStyle = com.example.model.IconStyle.TWO_TONE,
                        accentColor = CobaltPrimary,
                        modifier = Modifier.width(160.dp)
                    )
                    Text("Ваш гараж пока пуст", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Добавьте автомобиль или мотоцикл, выберите силуэт класса и начните вести учёт заправок, ТО и задач.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onAddVehicle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("empty_add_vehicle_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить первый транспорт")
                    }
                }
            }
        }
        return
    }

    val selected = data.selectedVehicle ?: data.vehicles.first()
    val accentColor = remember(selected.accentColorHex) { parseHexColor(selected.accentColorHex) }

    val upcomingTasks = remember(data.reminders, selected.id, selected.odometerKm) {
        data.reminders
            .filter { !it.completed && it.vehicleId == selected.id }
            .map { it to it.computeUrgency(selected.odometerKm) }
            .sortedBy { it.second.sortScore }
            .take(3)
    }

    val filteredRecords = remember(data.vehicleRecords, filterType) {
        if (filterType == null) data.vehicleRecords
        else data.vehicleRecords.filter { it.safeRecordType() == filterType }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Horizontal Vehicle Switcher
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                data.vehicles.forEach { vehicle ->
                    val isSel = vehicle.id == selected.id
                    val vColor = parseHexColor(vehicle.accentColorHex)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSel) vColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSel) 1.5.dp else 1.dp,
                            color = if (isSel) vColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .clickable { onSelectVehicle(vehicle.id) }
                            .testTag("vehicle_chip_${vehicle.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VehicleSilhouette(
                                vehicleClass = vehicle.safeVehicleClass(),
                                iconStyle = vehicle.safeIconStyle(),
                                accentColor = vColor,
                                showGroundLine = false,
                                modifier = Modifier.width(42.dp)
                            )
                            Text(
                                text = vehicle.displayTitle(),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onAddVehicle,
                    modifier = Modifier.testTag("top_add_vehicle_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Добавить транспорт")
                }
            }
        }

        // Hero Vehicle Summary Card (Drivvo-style)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = selected.displayTitle(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${selected.safeCategory().labelRu} • ${selected.safeVehicleClass().labelRu} (${selected.safeIconStyle().labelRu})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onEditVehicle(selected) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Настроить транспорт")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        VehicleSilhouette(
                            vehicleClass = selected.safeVehicleClass(),
                            iconStyle = selected.safeIconStyle(),
                            accentColor = accentColor,
                            modifier = Modifier.width(150.dp)
                        )
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .clickable { onQuickOdometer(selected) }
                                    .testTag("quick_odometer_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${numFormat.format(selected.odometerKm)} км",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        Icons.Rounded.Edit,
                                        contentDescription = "Изменить пробег",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = selected.fuelType,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 3 KPI tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiTile(
                            label = "Расход",
                            value = data.vehicleStats.avgLitersPer100Km?.let {
                                String.format(Locale.US, "%.1f л/100", it)
                            } ?: "—",
                            accent = FuelAmber,
                            modifier = Modifier.weight(1f)
                        )
                        KpiTile(
                            label = "Затраты",
                            value = numFormat.format(data.vehicleStats.totalSpent.toLong()),
                            accent = CobaltPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        KpiTile(
                            label = "Цена/км",
                            value = data.vehicleStats.costPerKm?.let {
                                String.format(Locale.US, "%.2f", it)
                            } ?: "—",
                            accent = ServiceEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Todoist-style Upcoming Tasks Preview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ближайшие задачи ТО", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = onOpenTasksTab) {
                            Text("Все задачи")
                        }
                    }
                    if (upcomingTasks.isEmpty()) {
                        Text(
                            "Активных задач для ${selected.displayTitle()} нет. Нажмите + чтобы запланировать замену масла или ОСАГО.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        upcomingTasks.forEach { (task, urgencyInfo) ->
                            val stripeColor = when {
                                urgencyInfo.urgency == TaskUrgency.OVERDUE || task.safePriority() == TaskPriority.HIGH -> AlertCoral
                                urgencyInfo.urgency == TaskUrgency.DUE_SOON -> FuelAmber
                                else -> CobaltPrimary
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(26.dp)
                                        .background(stripeColor, RoundedCornerShape(2.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        urgencyInfo.badgeText,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (urgencyInfo.urgency == TaskUrgency.OVERDUE) AlertCoral else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onToggleTask(task, true, selected.odometerKm) }) {
                                    Icon(
                                        Icons.Rounded.CheckCircleOutline,
                                        contentDescription = "Выполнить",
                                        tint = stripeColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Drivvo History Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterType == null,
                    onClick = { filterType = null },
                    label = { Text("Все (${data.vehicleRecords.size})") }
                )
                RecordType.entries.forEach { rt ->
                    val count = data.vehicleRecords.count { it.safeRecordType() == rt }
                    FilterChip(
                        selected = filterType == rt,
                        onClick = { filterType = if (filterType == rt) null else rt },
                        label = { Text("${rt.labelRu} ($count)") }
                    )
                }
            }
        }

        if (filteredRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Записей пока нет", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Добавьте первую заправку или сервис через кнопку + ниже",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onQuickAddRecord(RecordType.REFUEL) }) {
                                Text("Заправка")
                            }
                            Button(onClick = { onQuickAddRecord(RecordType.SERVICE) }) {
                                Text("Сервис / ТО")
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredRecords, key = { it.id }) { record ->
                val type = record.safeRecordType()
                val badgeColor = when (type) {
                    RecordType.REFUEL -> FuelAmber
                    RecordType.SERVICE -> ServiceEmerald
                    RecordType.EXPENSE -> ExpenseIndigo
                }
                val icon = when (type) {
                    RecordType.REFUEL -> Icons.Rounded.LocalGasStation
                    RecordType.SERVICE -> Icons.Rounded.Build
                    RecordType.EXPENSE -> Icons.Rounded.ReceiptLong
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditRecord(record) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(badgeColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = type.labelRu, tint = badgeColor, modifier = Modifier.size(22.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val sub = buildString {
                                append(dateFormat.format(Date(record.eventTimestampMs)))
                                append(" • ")
                                append("${numFormat.format(record.odometerKm)} км")
                                if (record.liters > 0) {
                                    append(" • ")
                                    append("${String.format(Locale.US, "%.1f", record.liters)} л")
                                }
                            }
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(Locale.US, "%.0f", record.amountCost),
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = record.categoryTag,
                                style = MaterialTheme.typography.labelMedium,
                                color = badgeColor
                            )
                        }

                        IconButton(onClick = { onDeleteRecord(record.id) }) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
