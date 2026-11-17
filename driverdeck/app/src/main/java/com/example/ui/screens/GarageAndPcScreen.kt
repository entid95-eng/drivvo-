package com.example.ui.screens

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.model.CURRENT_SCHEMA_VERSION
import com.example.model.IconStyle
import com.example.model.Vehicle
import com.example.model.VehicleClass
import com.example.pc.PcSyncService
import com.example.pc.PcWebServerManager
import com.example.ui.GarageDashboardData
import com.example.ui.auth.signOutUser
import com.example.ui.silhouettes.VehicleSilhouette
import com.example.ui.silhouettes.parseHexColor
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ServiceEmerald
import com.example.widget.DriveDeckWidgetReceiver
import com.example.widget.WidgetConfigActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun GarageAndPcScreen(
    data: GarageDashboardData,
    onAddVehicle: () -> Unit,
    onEditVehicle: (Vehicle) -> Unit,
    onDeleteVehicle: (String) -> Unit,
    onUpdateVehicleStyle: (String, IconStyle, String) -> Unit,
    onExportJson: suspend () -> Result<String>,
    onExportCsv: suspend () -> Result<String>,
    onImportJson: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val pcState by PcWebServerManager.state.collectAsStateWithLifecycle()

    var galleryStylePreview by remember { mutableStateOf(IconStyle.TWO_TONE) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        PcSyncService.start(context)
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                onExportJson().onSuccess { content ->
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                        onShowMessage("Резервная копия JSON сохранена")
                    }.onFailure { onShowMessage("Ошибка записи файла") }
                }.onFailure { onShowMessage(it.localizedMessage ?: "Ошибка экспорта") }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                onExportCsv().onSuccess { content ->
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                        onShowMessage("Таблица CSV сохранена для ПК/Excel")
                    }.onFailure { onShowMessage("Ошибка записи CSV") }
                }.onFailure { onShowMessage(it.localizedMessage ?: "Ошибка экспорта") }
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                if (text.isNotBlank()) onImportJson(text)
            }.onFailure { onShowMessage("Не удалось прочитать файл") }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Garage section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Гараж и ПК-доступ", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = onAddVehicle, modifier = Modifier.testTag("garage_add_button")) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Добавить")
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ваша техника (${data.vehicles.size})", style = MaterialTheme.typography.titleMedium)
                    if (data.vehicles.isEmpty()) {
                        Text(
                            "Добавьте авто или мотоцикл, чтобы привязать журнал и задачи.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        data.vehicles.forEach { veh ->
                            val color = parseHexColor(veh.accentColorHex)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    VehicleSilhouette(
                                        vehicleClass = veh.safeVehicleClass(),
                                        iconStyle = veh.safeIconStyle(),
                                        accentColor = color,
                                        modifier = Modifier.width(86.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(veh.displayTitle(), style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${veh.safeVehicleClass().labelRu} • ${veh.safeIconStyle().labelRu}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconStyle.entries.forEach { st ->
                                                FilterChip(
                                                    selected = veh.safeIconStyle() == st,
                                                    onClick = { onUpdateVehicleStyle(veh.id, st, veh.accentColorHex) },
                                                    label = { Text(st.labelRu, style = MaterialTheme.typography.labelMedium) }
                                                )
                                            }
                                        }
                                    }
                                    Column {
                                        IconButton(onClick = { onEditVehicle(veh) }) {
                                            Icon(Icons.Rounded.Edit, contentDescription = "Редактировать")
                                        }
                                        IconButton(onClick = { onDeleteVehicle(veh.id) }) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Удалить")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Gallery of all 8 vehicle classes x 3 icon variants
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Галерея силуэтов по классам (8 классов × 3 стиля)", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconStyle.entries.forEach { style ->
                            FilterChip(
                                selected = galleryStylePreview == style,
                                onClick = { galleryStylePreview = style },
                                label = { Text(style.labelRu) }
                            )
                        }
                    }
                    Text(
                        galleryStylePreview.subtitleRu,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VehicleClass.entries.forEach { cls ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.width(116.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    VehicleSilhouette(
                                        vehicleClass = cls,
                                        iconStyle = galleryStylePreview,
                                        accentColor = CobaltPrimary,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(cls.labelRu, style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        cls.category.labelRu,
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

        // PC Web Console via Wi-Fi
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Веб-панель для ПК (по Wi-Fi)", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Просмотр и редактирование с компьютера в браузере",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = pcState.running,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        PcSyncService.start(context)
                                    }
                                } else {
                                    PcSyncService.stop(context)
                                }
                            },
                            modifier = Modifier.testTag("pc_server_switch")
                        )
                    }

                    if (pcState.running) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ServiceEmerald.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Сервер активен — откройте на ПК:", style = MaterialTheme.typography.labelLarge, color = ServiceEmerald)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pcState.url,
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    IconButton(onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("DriveDeck URL", pcState.url))
                                        onShowMessage("Адрес скопирован")
                                    }) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Копировать URL")
                                    }
                                }
                                Text(
                                    text = "PIN-код для входа: ${pcState.pin}",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    } else if (pcState.error != null) {
                        Text(pcState.error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Home screen Todoist widget actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Виджет рабочего стола", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Список задач в стиле Todoist с чекбоксами и быстрым +",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val awm = context.getSystemService(AppWidgetManager::class.java)
                                    val provider = ComponentName(context, DriveDeckWidgetReceiver::class.java)
                                    if (awm.isRequestPinAppWidgetSupported) {
                                        awm.requestPinAppWidget(provider, null, null)
                                    } else {
                                        onShowMessage("Добавьте виджет DriveDeck через меню виджетов рабочего стола")
                                    }
                                } else {
                                    onShowMessage("Добавьте виджет DriveDeck через меню виджетов рабочего стола")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("На экран")
                        }
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(context, WidgetConfigActivity::class.java))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Оформление")
                        }
                    }
                }
            }
        }

        // Updates, Backup & Account
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Обновления и резервные копии", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Версия: ${BuildConfig.VERSION_NAME} (сборка ${BuildConfig.VERSION_CODE}) • Схема данных v$CURRENT_SCHEMA_VERSION.\nВсе новые доработки совместимы с вашими текущими данными в облаке.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { exportJsonLauncher.launch("drivedeck_backup.json") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Бэкап JSON")
                        }
                        OutlinedButton(
                            onClick = { importJsonLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Импорт")
                        }
                    }
                    OutlinedButton(
                        onClick = { exportCsvLauncher.launch("drivedeck_records.csv") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Экспорт таблицы в Excel / CSV для ПК")
                    }
                    Spacer(Modifier.height(4.dp))
                    val email = Firebase.auth.currentUser?.email ?: "Аккаунт Google"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { signOutUser(credentialManager, scope) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(email, style = MaterialTheme.typography.titleSmall)
                            Text("Выйти из аккаунта", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        }
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Выйти", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
