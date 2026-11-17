package com.example.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.WidgetConfigPrefs
import com.example.data.WidgetSyncStore
import com.example.data.WidgetThemeMode
import com.example.ui.theme.DriveDeckTheme
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(Activity.RESULT_CANCELED)

        setContent {
            DriveDeckTheme {
                val scope = rememberCoroutineScope()
                var themeMode by remember { mutableStateOf(WidgetThemeMode.SYSTEM) }
                var opacity by remember { mutableIntStateOf(95) }
                var compact by remember { mutableStateOf(false) }
                var filterVehicleId by remember { mutableStateOf("ALL") }
                var vehicles by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

                LaunchedEffect(Unit) {
                    val snapshot = WidgetSyncStore.getSnapshot(this@WidgetConfigActivity)
                    themeMode = snapshot.config.themeMode
                    opacity = snapshot.config.opacityPercent
                    compact = snapshot.config.compactMode
                    filterVehicleId = snapshot.config.filterVehicleId
                    vehicles = snapshot.vehicles
                }

                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Настройка виджета DriveDeck") })
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Тема виджета", style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    WidgetThemeMode.entries.forEach { mode ->
                                        FilterChip(
                                            selected = themeMode == mode,
                                            onClick = { themeMode = mode },
                                            label = { Text(mode.labelRu) }
                                        )
                                    }
                                }
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Прозрачность фона: $opacity%", style = MaterialTheme.typography.titleMedium)
                                Slider(
                                    value = opacity.toFloat(),
                                    onValueChange = { opacity = it.toInt() },
                                    valueRange = 45f..100f
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Компактный режим", style = MaterialTheme.typography.bodyLarge)
                                    Switch(checked = compact, onCheckedChange = { compact = it })
                                }
                            }
                        }

                        if (vehicles.isNotEmpty()) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Фильтр транспорта", style = MaterialTheme.typography.titleMedium)
                                    FilterChip(
                                        selected = filterVehicleId == "ALL",
                                        onClick = { filterVehicleId = "ALL" },
                                        label = { Text("Весь гараж") }
                                    )
                                    vehicles.forEach { (id, name) ->
                                        FilterChip(
                                            selected = filterVehicleId == id,
                                            onClick = { filterVehicleId = id },
                                            label = { Text(name) }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    WidgetSyncStore.saveWidgetConfig(
                                        this@WidgetConfigActivity,
                                        WidgetConfigPrefs(
                                            themeMode = themeMode,
                                            opacityPercent = opacity,
                                            compactMode = compact,
                                            filterVehicleId = filterVehicleId
                                        )
                                    )
                                    val resultValue = Intent().apply {
                                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                    }
                                    setResult(Activity.RESULT_OK, resultValue)
                                    finish()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_widget_config_button")
                        ) {
                            Text("Сохранить виджет")
                        }
                    }
                }
            }
        }
    }
}
