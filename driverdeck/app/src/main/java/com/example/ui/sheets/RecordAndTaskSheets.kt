package com.example.ui.sheets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.LogRecord
import com.example.model.RecordType
import com.example.model.ReminderTask
import com.example.model.ReminderTriggerType
import com.example.model.TaskPriority
import com.example.model.Vehicle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OdometerQuickDialog(
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var odoStr by remember { mutableStateOf(vehicle.odometerKm.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Обновить пробег") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(vehicle.displayTitle(), style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = odoStr,
                    onValueChange = { odoStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Текущий пробег (км)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val valKm = odoStr.toLongOrNull() ?: vehicle.odometerKm
                onConfirm(valKm.coerceIn(0L, 5_000_000L))
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun RecordEditorDialog(
    vehicles: List<Vehicle>,
    defaultVehicleId: String,
    initialRecord: LogRecord?,
    defaultType: RecordType,
    onDismiss: () -> Unit,
    onSave: (LogRecord, Long?) -> Unit
) {
    var selectedVehicleId by remember {
        mutableStateOf(initialRecord?.vehicleId?.ifBlank { defaultVehicleId } ?: defaultVehicleId)
    }
    val selectedVehicle = vehicles.firstOrNull { it.id == selectedVehicleId } ?: vehicles.firstOrNull()
    var recordType by remember { mutableStateOf(initialRecord?.safeRecordType() ?: defaultType) }
    var title by remember { mutableStateOf(initialRecord?.title ?: "") }
    var categoryTag by remember {
        mutableStateOf(
            initialRecord?.categoryTag ?: when (recordType) {
                RecordType.REFUEL -> selectedVehicle?.fuelType ?: "АИ-95"
                RecordType.SERVICE -> "Замена масла"
                RecordType.EXPENSE -> "Страховка"
            }
        )
    }
    var costStr by remember {
        mutableStateOf(if (initialRecord != null && initialRecord.amountCost > 0) initialRecord.amountCost.toString() else "")
    }
    var odoStr by remember {
        mutableStateOf((initialRecord?.odometerKm ?: selectedVehicle?.odometerKm ?: 0L).toString())
    }
    var litersStr by remember {
        mutableStateOf(if (initialRecord != null && initialRecord.liters > 0) initialRecord.liters.toString() else "")
    }
    var pricePerLiterStr by remember {
        mutableStateOf(if (initialRecord != null && initialRecord.pricePerLiter > 0) initialRecord.pricePerLiter.toString() else "")
    }
    var fullTank by remember { mutableStateOf(initialRecord?.fullTank ?: true) }
    var notes by remember { mutableStateOf(initialRecord?.notes ?: "") }

    val presets = when (recordType) {
        RecordType.REFUEL -> listOf("АИ-95", "АИ-92", "АИ-100", "ДТ", "Газ")
        RecordType.SERVICE -> listOf("Замена масла", "Фильтры", "Тормоза", "Цепь и звёзды", "Шиномонтаж", "Плановое ТО")
        RecordType.EXPENSE -> listOf("ОСАГО / КАСКО", "Мойка", "Парковка", "Штраф", "Налог", "Тюнинг")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialRecord == null) "Новая запись" else "Редактировать запись",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Закрыть")
                    }
                }

                // Vehicle picker
                if (vehicles.size > 1) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        vehicles.forEach { v ->
                            FilterChip(
                                selected = selectedVehicle?.id == v.id,
                                onClick = {
                                    selectedVehicleId = v.id
                                    if (initialRecord == null) odoStr = v.odometerKm.toString()
                                },
                                label = { Text(v.displayTitle()) }
                            )
                        }
                    }
                }

                // RecordType chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecordType.entries.forEach { rt ->
                        FilterChip(
                            selected = recordType == rt,
                            onClick = {
                                recordType = rt
                                categoryTag = when (rt) {
                                    RecordType.REFUEL -> selectedVehicle?.fuelType ?: "АИ-95"
                                    RecordType.SERVICE -> "Замена масла"
                                    RecordType.EXPENSE -> "Мойка"
                                }
                            },
                            label = { Text(rt.labelRu) }
                        )
                    }
                }

                // Category presets
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { tag ->
                        FilterChip(
                            selected = categoryTag == tag,
                            onClick = {
                                categoryTag = tag
                                if (title.isBlank()) title = tag
                            },
                            label = { Text(tag) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название (например, Лукойл АИ-95 или СТО)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("record_title_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = odoStr,
                        onValueChange = { odoStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Пробег (км)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it.replace(',', '.') },
                        label = { Text("Сумма") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (recordType == RecordType.REFUEL) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = litersStr,
                            onValueChange = { v ->
                                val clean = v.replace(',', '.')
                                litersStr = clean
                                val l = clean.toDoubleOrNull()
                                val ppl = pricePerLiterStr.toDoubleOrNull()
                                val c = costStr.toDoubleOrNull()
                                if (l != null && ppl != null && ppl > 0 && costStr.isBlank()) {
                                    costStr = String.format(Locale.US, "%.2f", l * ppl)
                                } else if (l != null && l > 0 && c != null && c > 0) {
                                    pricePerLiterStr = String.format(Locale.US, "%.2f", c / l)
                                }
                            },
                            label = { Text("Литры") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = pricePerLiterStr,
                            onValueChange = { v ->
                                val clean = v.replace(',', '.')
                                pricePerLiterStr = clean
                                val ppl = clean.toDoubleOrNull()
                                val l = litersStr.toDoubleOrNull()
                                if (ppl != null && l != null && l > 0) {
                                    costStr = String.format(Locale.US, "%.2f", ppl * l)
                                }
                            },
                            label = { Text("Цена за л") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Полный бак (для расчёта расхода)", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = fullTank, onCheckedChange = { fullTank = it })
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Комментарий") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val vId = selectedVehicle?.id ?: return@Button
                        val liters = if (recordType == RecordType.REFUEL) (litersStr.toDoubleOrNull() ?: 0.0) else 0.0
                        val cost = costStr.toDoubleOrNull() ?: 0.0
                        val ppl = if (recordType == RecordType.REFUEL) {
                            pricePerLiterStr.toDoubleOrNull()
                                ?: if (liters > 0 && cost > 0) cost / liters else 0.0
                        } else 0.0
                        val rec = (initialRecord ?: LogRecord()).copy(
                            vehicleId = vId,
                            recordType = recordType.name,
                            title = title.trim().ifBlank { categoryTag.ifBlank { recordType.labelRu } },
                            categoryTag = categoryTag.trim().ifBlank { recordType.labelRu },
                            amountCost = cost.coerceAtLeast(0.0),
                            odometerKm = (odoStr.toLongOrNull() ?: selectedVehicle.odometerKm).coerceIn(0L, 5_000_000L),
                            liters = liters.coerceIn(0.0, 1000.0),
                            pricePerLiter = ppl.coerceIn(0.0, 10000.0),
                            fullTank = if (recordType == RecordType.REFUEL) fullTank else false,
                            notes = notes.trim()
                        )
                        onSave(rec, selectedVehicle.odometerKm)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_record_button")
                ) {
                    Text("Сохранить запись")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditorDialog(
    vehicles: List<Vehicle>,
    defaultVehicleId: String,
    initialTask: ReminderTask?,
    onDismiss: () -> Unit,
    onSave: (ReminderTask) -> Unit
) {
    var selectedVehicleId by remember {
        mutableStateOf(initialTask?.vehicleId?.ifBlank { defaultVehicleId } ?: defaultVehicleId)
    }
    val selectedVehicle = vehicles.firstOrNull { it.id == selectedVehicleId } ?: vehicles.firstOrNull()
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var notes by remember { mutableStateOf(initialTask?.notes ?: "") }
    var triggerType by remember { mutableStateOf(initialTask?.safeTriggerType() ?: ReminderTriggerType.BOTH) }
    var dueOdoStr by remember {
        val suggested = initialTask?.dueOdometerKm?.takeIf { it > 0 }
            ?: ((selectedVehicle?.odometerKm ?: 0L) + 10000L)
        mutableStateOf(suggested.toString())
    }
    var dueDateMs by remember {
        mutableLongStateOf(
            initialTask?.dueDateEpochMs?.takeIf { it > 0L }
                ?: (System.currentTimeMillis() + 30L * 86_400_000L)
        )
    }
    var repeatDaysStr by remember {
        mutableStateOf(if (initialTask != null && initialTask.repeatIntervalDays > 0) initialTask.repeatIntervalDays.toString() else "")
    }
    var repeatKmStr by remember {
        mutableStateOf(if (initialTask != null && initialTask.repeatIntervalKm > 0) initialTask.repeatIntervalKm.toString() else "")
    }
    var priority by remember { mutableStateOf(initialTask?.safePriority() ?: TaskPriority.MEDIUM) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale("ru")) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dueDateMs = it }
                    showDatePicker = false
                }) { Text("Выбрать") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialTask == null) "Новая задача / ТО" else "Редактировать задачу",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Закрыть")
                    }
                }

                if (vehicles.size > 1) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        vehicles.forEach { v ->
                            FilterChip(
                                selected = selectedVehicle?.id == v.id,
                                onClick = { selectedVehicleId = v.id },
                                label = { Text(v.displayTitle()) }
                            )
                        }
                    }
                }

                // Quick task templates
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Замена масла", "ОСАГО", "Техосмотр", "Смазка цепи", "Фильтры", "Резина").forEach { tpl ->
                        FilterChip(
                            selected = title == tpl,
                            onClick = { title = tpl },
                            label = { Text(tpl) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Задача") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                Text("Напоминать", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReminderTriggerType.entries.forEach { tt ->
                        FilterChip(
                            selected = triggerType == tt,
                            onClick = { triggerType = tt },
                            label = { Text(tt.labelRu) }
                        )
                    }
                }

                if (triggerType != ReminderTriggerType.BY_DATE) {
                    OutlinedTextField(
                        value = dueOdoStr,
                        onValueChange = { dueOdoStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Целевой пробег (км)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (triggerType != ReminderTriggerType.BY_MILEAGE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Дата: ${dateFormatter.format(Date(dueDateMs))}")
                        }
                        TextButton(onClick = { dueDateMs = System.currentTimeMillis() + 7L * 86_400_000L }) { Text("+7 дн") }
                        TextButton(onClick = { dueDateMs = System.currentTimeMillis() + 30L * 86_400_000L }) { Text("+30 дн") }
                    }
                }

                Text("Приоритет (цвет в виджете Todoist)", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { pr ->
                        FilterChip(
                            selected = priority == pr,
                            onClick = { priority = pr },
                            label = { Text(pr.labelRu) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = repeatKmStr,
                        onValueChange = { repeatKmStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Повтор (км)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = repeatDaysStr,
                        onValueChange = { repeatDaysStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Повтор (дней)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Детали (вязкость масла, артикул и т.д.)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        val vId = selectedVehicle?.id ?: return@Button
                        val task = (initialTask ?: ReminderTask()).copy(
                            vehicleId = vId,
                            title = title.trim().ifBlank { "Задача по ТО" },
                            notes = notes.trim(),
                            triggerType = triggerType.name,
                            dueDateEpochMs = if (triggerType == ReminderTriggerType.BY_MILEAGE) 0L else dueDateMs,
                            dueOdometerKm = if (triggerType == ReminderTriggerType.BY_DATE) 0L else (dueOdoStr.toLongOrNull() ?: 0L),
                            repeatIntervalDays = repeatDaysStr.toLongOrNull() ?: 0L,
                            repeatIntervalKm = repeatKmStr.toLongOrNull() ?: 0L,
                            priority = priority.name
                        )
                        onSave(task)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_task_button")
                ) {
                    Text("Сохранить задачу")
                }
            }
        }
    }
}
