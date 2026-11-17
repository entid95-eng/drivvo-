package com.example.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.IconStyle
import com.example.model.Vehicle
import com.example.model.VehicleCategory
import com.example.model.VehicleClass
import com.example.ui.silhouettes.VehicleSilhouette
import com.example.ui.silhouettes.parseHexColor

val ACCENT_COLOR_PRESETS = listOf(
    "#1D63DC",
    "#10B981",
    "#F59E0B",
    "#EF4444",
    "#6366F1",
    "#0EA5E9",
    "#EC4899",
    "#475569"
)

@Composable
fun VehicleEditorDialog(
    initialVehicle: Vehicle?,
    onDismiss: () -> Unit,
    onSave: (Vehicle) -> Unit
) {
    var category by remember { mutableStateOf(initialVehicle?.safeCategory() ?: VehicleCategory.CAR) }
    var vehicleClass by remember { mutableStateOf(initialVehicle?.safeVehicleClass() ?: VehicleClass.SEDAN) }
    var iconStyle by remember { mutableStateOf(initialVehicle?.safeIconStyle() ?: IconStyle.TWO_TONE) }
    var accentHex by remember { mutableStateOf(initialVehicle?.accentColorHex ?: "#1D63DC") }
    var name by remember { mutableStateOf(initialVehicle?.name ?: "") }
    var brand by remember { mutableStateOf(initialVehicle?.brand ?: "") }
    var model by remember { mutableStateOf(initialVehicle?.model ?: "") }
    var yearStr by remember { mutableStateOf((initialVehicle?.year ?: 2022L).toString()) }
    var odometerStr by remember { mutableStateOf((initialVehicle?.odometerKm ?: 0L).toString()) }
    var fuelType by remember { mutableStateOf(initialVehicle?.fuelType ?: "АИ-95") }
    var tankStr by remember { mutableStateOf((initialVehicle?.tankCapacityLiters ?: 50.0).toInt().toString()) }

    val classesForCategory = remember(category) { VehicleClass.byCategory(category) }
    val accentColor = remember(accentHex) { parseHexColor(accentHex) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialVehicle == null) "Новый транспорт" else "Редактировать транспорт",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Закрыть")
                    }
                }

                // Category switcher: Авто / Мото
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VehicleCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                val available = VehicleClass.byCategory(cat)
                                if (vehicleClass !in available) {
                                    vehicleClass = available.first()
                                }
                            },
                            label = { Text(cat.labelRu, fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("category_chip_${cat.name}")
                        )
                    }
                }

                // Class selector with silhouettes
                Text("Класс и силуэт", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    classesForCategory.forEach { cls ->
                        val selected = vehicleClass == cls
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) accentColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .width(126.dp)
                                .clickable { vehicleClass = cls }
                                .testTag("class_option_${cls.name}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                VehicleSilhouette(
                                    vehicleClass = cls,
                                    iconStyle = iconStyle,
                                    accentColor = accentColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = cls.labelRu,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 3 Icon Style variants comparison
                Text("Вариант исполнения иконки (3 стиля)", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconStyle.entries.forEach { styleVariant ->
                        val selected = iconStyle == styleVariant
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { iconStyle = styleVariant }
                                .testTag("icon_style_${styleVariant.name}")
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                VehicleSilhouette(
                                    vehicleClass = vehicleClass,
                                    iconStyle = styleVariant,
                                    accentColor = accentColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = styleVariant.labelRu,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Accent color swatches
                Text("Цвет акцента", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ACCENT_COLOR_PRESETS.forEach { hex ->
                        val c = parseHexColor(hex)
                        val isSelected = accentHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { accentHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название (например, Tiguan или Yamaha MT-07)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vehicle_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Марка") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Модель") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = odometerStr,
                        onValueChange = { odometerStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Пробег (км)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = yearStr,
                        onValueChange = { yearStr = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Год") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fuelType,
                        onValueChange = { fuelType = it },
                        label = { Text("Топливо") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tankStr,
                        onValueChange = { tankStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Бак (л)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        val finalName = name.trim().ifBlank {
                            listOf(brand.trim(), model.trim()).filter { it.isNotBlank() }.joinToString(" ")
                                .ifBlank { vehicleClass.labelRu }
                        }
                        val veh = (initialVehicle ?: Vehicle()).copy(
                            name = finalName,
                            brand = brand.trim(),
                            model = model.trim(),
                            year = (yearStr.toLongOrNull() ?: 2022L).coerceIn(1950L, 2100L),
                            category = category.name,
                            vehicleClass = vehicleClass.name,
                            iconStyle = iconStyle.name,
                            accentColorHex = accentHex,
                            odometerKm = (odometerStr.toLongOrNull() ?: 0L).coerceIn(0L, 5_000_000L),
                            fuelType = fuelType.trim().ifBlank { "АИ-95" },
                            tankCapacityLiters = (tankStr.toDoubleOrNull() ?: 50.0).coerceIn(0.0, 500.0)
                        )
                        onSave(veh)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_vehicle_button")
                ) {
                    Text(if (initialVehicle == null) "Добавить в гараж" else "Сохранить изменения")
                }
            }
        }
    }
}
