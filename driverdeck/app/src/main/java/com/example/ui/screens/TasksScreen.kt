package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ReminderTask
import com.example.model.TaskPriority
import com.example.model.TaskUrgency
import com.example.model.Vehicle
import com.example.ui.GarageDashboardData
import com.example.ui.silhouettes.VehicleSilhouette
import com.example.ui.silhouettes.parseHexColor
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.FuelAmber
import com.example.ui.theme.ServiceEmerald

@Composable
fun TasksScreen(
    data: GarageDashboardData,
    onAddTask: () -> Unit,
    onEditTask: (ReminderTask) -> Unit,
    onToggleTask: (ReminderTask, Boolean, Long) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    var filterVehicleId by remember { mutableStateOf("ALL") }
    var showCompleted by remember { mutableStateOf(false) }

    val vehicleMap = remember(data.vehicles) { data.vehicles.associateBy { it.id } }

    val filtered = remember(data.reminders, filterVehicleId) {
        if (filterVehicleId == "ALL") data.reminders
        else data.reminders.filter { it.vehicleId == filterVehicleId }
    }

    val activeWithUrgency = remember(filtered, vehicleMap) {
        filtered.filter { !it.completed }
            .map { t ->
                val v = vehicleMap[t.vehicleId]
                Triple(t, v, t.computeUrgency(v?.odometerKm ?: 0L))
            }
            .sortedBy { it.third.sortScore }
    }

    val overdue = activeWithUrgency.filter { it.third.urgency == TaskUrgency.OVERDUE }
    val dueSoon = activeWithUrgency.filter { it.third.urgency == TaskUrgency.DUE_SOON }
    val upcoming = activeWithUrgency.filter { it.third.urgency == TaskUrgency.UPCOMING }
    val completedList = remember(filtered) { filtered.filter { it.completed } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Задачи в стиле Todoist", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Синхронизируются с виджетом на главном экране",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddTask,
                    enabled = data.vehicles.isNotEmpty(),
                    modifier = Modifier.testTag("tasks_add_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Задача")
                }
            }
        }

        // Vehicle filter chips
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
                    label = { Text("Все (${data.reminders.count { !it.completed }})") }
                )
                data.vehicles.forEach { v ->
                    val cnt = data.reminders.count { !it.completed && it.vehicleId == v.id }
                    FilterChip(
                        selected = filterVehicleId == v.id,
                        onClick = { filterVehicleId = v.id },
                        label = { Text("${v.displayTitle()} ($cnt)") }
                    )
                }
            }
        }

        if (activeWithUrgency.isEmpty() && (!showCompleted || completedList.isEmpty())) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Список ближайших задач пуст", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Добавьте замену масла, продление страховки или проверку цепи — задачи автоматически появятся в виджете.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (overdue.isNotEmpty()) {
            item { SectionTitle("Просрочено (${overdue.size})", AlertCoral) }
            items(overdue, key = { it.first.id }) { (task, veh, info) ->
                TodoistTaskRow(
                    task = task,
                    vehicle = veh,
                    badge = info.badgeText,
                    urgency = info.urgency,
                    onClick = { onEditTask(task) },
                    onToggle = { onToggleTask(task, true, veh?.odometerKm ?: 0L) },
                    onDelete = { onDeleteTask(task.id) }
                )
            }
        }

        if (dueSoon.isNotEmpty()) {
            item { SectionTitle("Скоро и срочно (${dueSoon.size})", FuelAmber) }
            items(dueSoon, key = { it.first.id }) { (task, veh, info) ->
                TodoistTaskRow(
                    task = task,
                    vehicle = veh,
                    badge = info.badgeText,
                    urgency = info.urgency,
                    onClick = { onEditTask(task) },
                    onToggle = { onToggleTask(task, true, veh?.odometerKm ?: 0L) },
                    onDelete = { onDeleteTask(task.id) }
                )
            }
        }

        if (upcoming.isNotEmpty()) {
            item { SectionTitle("Плановые (${upcoming.size})", CobaltPrimary) }
            items(upcoming, key = { it.first.id }) { (task, veh, info) ->
                TodoistTaskRow(
                    task = task,
                    vehicle = veh,
                    badge = info.badgeText,
                    urgency = info.urgency,
                    onClick = { onEditTask(task) },
                    onToggle = { onToggleTask(task, true, veh?.odometerKm ?: 0L) },
                    onDelete = { onDeleteTask(task.id) }
                )
            }
        }

        if (completedList.isNotEmpty()) {
            item {
                FilterChip(
                    selected = showCompleted,
                    onClick = { showCompleted = !showCompleted },
                    label = { Text("Выполненные (${completedList.size})") }
                )
            }
            if (showCompleted) {
                items(completedList, key = { it.id }) { task ->
                    val veh = vehicleMap[task.vehicleId]
                    TodoistTaskRow(
                        task = task,
                        vehicle = veh,
                        badge = "Выполнено",
                        urgency = TaskUrgency.COMPLETED,
                        onClick = { onEditTask(task) },
                        onToggle = { onToggleTask(task, false, veh?.odometerKm ?: 0L) },
                        onDelete = { onDeleteTask(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(title, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TodoistTaskRow(
    task: ReminderTask,
    vehicle: Vehicle?,
    badge: String,
    urgency: TaskUrgency,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val ringColor = when {
        task.completed -> ServiceEmerald
        urgency == TaskUrgency.OVERDUE || task.safePriority() == TaskPriority.HIGH -> AlertCoral
        urgency == TaskUrgency.DUE_SOON || task.safePriority() == TaskPriority.MEDIUM -> FuelAmber
        else -> CobaltPrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Todoist-style circular priority checkbox with 48dp touch target
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggle)
                    .testTag("task_checkbox_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (task.completed) ServiceEmerald else ringColor.copy(alpha = 0.12f))
                        .border(2.dp, ringColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.completed) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (vehicle != null) {
                        VehicleSilhouette(
                            vehicleClass = vehicle.safeVehicleClass(),
                            iconStyle = vehicle.safeIconStyle(),
                            accentColor = parseHexColor(vehicle.accentColorHex),
                            showGroundLine = false,
                            modifier = Modifier.width(34.dp)
                        )
                        Text(
                            text = vehicle.displayTitle(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ringColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelMedium,
                            color = ringColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (task.repeatIntervalKm > 0 || task.repeatIntervalDays > 0) {
                        Icon(
                            Icons.Rounded.Repeat,
                            contentDescription = "Повторяемая задача",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
