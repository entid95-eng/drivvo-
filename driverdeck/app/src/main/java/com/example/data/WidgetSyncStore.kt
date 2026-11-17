package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ReminderTask
import com.example.model.TaskPriority
import com.example.model.TaskUrgency
import com.example.model.Vehicle
import com.example.model.VehicleClass
import com.example.widget.DriveDeckWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.driveDeckWidgetStore: DataStore<Preferences> by preferencesDataStore(name = "drivedeck_widget_cache")

enum class WidgetThemeMode(val labelRu: String) {
    SYSTEM("Системная"),
    LIGHT("Светлая"),
    DARK("Тёмная");

    companion object {
        fun fromRaw(raw: String?): WidgetThemeMode = entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

data class WidgetConfigPrefs(
    val themeMode: WidgetThemeMode = WidgetThemeMode.SYSTEM,
    val opacityPercent: Int = 95,
    val compactMode: Boolean = false,
    val filterVehicleId: String = "ALL"
)

data class WidgetTaskItem(
    val id: String,
    val vehicleId: String,
    val vehicleName: String,
    val vehicleClass: VehicleClass,
    val accentHex: String,
    val title: String,
    val badgeText: String,
    val urgency: TaskUrgency,
    val priority: TaskPriority,
    val completed: Boolean
)

data class WidgetSnapshot(
    val tasks: List<WidgetTaskItem>,
    val vehicles: List<Pair<String, String>>,
    val config: WidgetConfigPrefs
)

object WidgetSyncStore {
    private val KEY_TASKS_JSON = stringPreferencesKey("widget_tasks_json")
    private val KEY_VEHICLES_JSON = stringPreferencesKey("widget_vehicles_json")
    private val KEY_THEME_MODE = stringPreferencesKey("widget_theme_mode")
    private val KEY_OPACITY = intPreferencesKey("widget_opacity_percent")
    private val KEY_COMPACT = booleanPreferencesKey("widget_compact_mode")
    private val KEY_FILTER_VEHICLE = stringPreferencesKey("widget_filter_vehicle_id")
    private val KEY_REVISION = intPreferencesKey("widget_revision")

    fun observeSnapshot(context: Context): Flow<WidgetSnapshot> =
        context.applicationContext.driveDeckWidgetStore.data.map { prefs ->
            parseSnapshot(prefs)
        }

    suspend fun getSnapshot(context: Context): WidgetSnapshot {
        val prefs = context.applicationContext.driveDeckWidgetStore.data.first()
        return parseSnapshot(prefs)
    }

    suspend fun syncFromCloud(
        context: Context,
        vehicles: List<Vehicle>,
        reminders: List<ReminderTask>
    ) {
        val vehicleMap = vehicles.associateBy { it.id }
        val now = System.currentTimeMillis()

        val sortedActive = reminders
            .filter { !it.completed }
            .map { task ->
                val vehicle = vehicleMap[task.vehicleId]
                val odo = vehicle?.odometerKm ?: 0L
                val info = task.computeUrgency(odo, now)
                Triple(task, vehicle, info)
            }
            .sortedWith(
                compareBy<Triple<ReminderTask, Vehicle?, com.example.model.TaskUrgencyInfo>> {
                    when (it.third.urgency) {
                        TaskUrgency.OVERDUE -> 0
                        TaskUrgency.DUE_SOON -> 1
                        TaskUrgency.UPCOMING -> 2
                        TaskUrgency.COMPLETED -> 3
                    }
                }.thenBy { it.third.sortScore }
            )

        val tasksArr = JSONArray()
        sortedActive.forEach { (task, vehicle, info) ->
            tasksArr.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("vehicleId", task.vehicleId)
                    put("vehicleName", vehicle?.displayTitle() ?: "Авто")
                    put("vehicleClass", (vehicle?.safeVehicleClass() ?: VehicleClass.SEDAN).name)
                    put("accentHex", vehicle?.accentColorHex ?: "#1D63DC")
                    put("title", task.title)
                    put("badgeText", info.badgeText)
                    put("urgency", info.urgency.name)
                    put("priority", task.safePriority().name)
                    put("completed", task.completed)
                }
            )
        }

        val vehArr = JSONArray()
        vehicles.forEach { v ->
            vehArr.put(
                JSONObject().apply {
                    put("id", v.id)
                    put("name", v.displayTitle())
                }
            )
        }

        val appCtx = context.applicationContext
        appCtx.driveDeckWidgetStore.edit { prefs ->
            prefs[KEY_TASKS_JSON] = tasksArr.toString()
            prefs[KEY_VEHICLES_JSON] = vehArr.toString()
            prefs[KEY_REVISION] = (prefs[KEY_REVISION] ?: 0) + 1
        }
        runCatching { DriveDeckWidget().updateAll(appCtx) }
    }

    suspend fun markTaskCompletedLocally(context: Context, taskId: String) {
        val appCtx = context.applicationContext
        appCtx.driveDeckWidgetStore.edit { prefs ->
            val currentJson = prefs[KEY_TASKS_JSON] ?: "[]"
            val arr = runCatching { JSONArray(currentJson) }.getOrElse { JSONArray() }
            val updated = JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.optString("id") != taskId) {
                    updated.put(obj)
                }
            }
            prefs[KEY_TASKS_JSON] = updated.toString()
            prefs[KEY_REVISION] = (prefs[KEY_REVISION] ?: 0) + 1
        }
        runCatching { DriveDeckWidget().updateAll(appCtx) }
    }

    suspend fun saveWidgetConfig(context: Context, config: WidgetConfigPrefs) {
        val appCtx = context.applicationContext
        appCtx.driveDeckWidgetStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = config.themeMode.name
            prefs[KEY_OPACITY] = config.opacityPercent.coerceIn(40, 100)
            prefs[KEY_COMPACT] = config.compactMode
            prefs[KEY_FILTER_VEHICLE] = config.filterVehicleId
            prefs[KEY_REVISION] = (prefs[KEY_REVISION] ?: 0) + 1
        }
        runCatching { DriveDeckWidget().updateAll(appCtx) }
    }

    suspend fun cycleFilterVehicle(context: Context) {
        val appCtx = context.applicationContext
        appCtx.driveDeckWidgetStore.edit { prefs ->
            val snapshot = parseSnapshot(prefs)
            val ids = listOf("ALL") + snapshot.vehicles.map { it.first }
            val currentIdx = ids.indexOf(snapshot.config.filterVehicleId)
            val nextId = if (ids.isEmpty()) "ALL" else ids[(currentIdx + 1) % ids.size]
            prefs[KEY_FILTER_VEHICLE] = nextId
            prefs[KEY_REVISION] = (prefs[KEY_REVISION] ?: 0) + 1
        }
        runCatching { DriveDeckWidget().updateAll(appCtx) }
    }

    private fun parseSnapshot(prefs: Preferences): WidgetSnapshot {
        val tasksArr = runCatching { JSONArray(prefs[KEY_TASKS_JSON] ?: "[]") }.getOrElse { JSONArray() }
        val tasks = buildList {
            for (i in 0 until tasksArr.length()) {
                val o = tasksArr.optJSONObject(i) ?: continue
                add(
                    WidgetTaskItem(
                        id = o.optString("id"),
                        vehicleId = o.optString("vehicleId"),
                        vehicleName = o.optString("vehicleName", "Транспорт"),
                        vehicleClass = VehicleClass.fromRaw(o.optString("vehicleClass")),
                        accentHex = o.optString("accentHex", "#1D63DC"),
                        title = o.optString("title"),
                        badgeText = o.optString("badgeText"),
                        urgency = TaskUrgency.entries.firstOrNull { it.name == o.optString("urgency") }
                            ?: TaskUrgency.UPCOMING,
                        priority = TaskPriority.fromRaw(o.optString("priority")),
                        completed = o.optBoolean("completed", false)
                    )
                )
            }
        }

        val vehArr = runCatching { JSONArray(prefs[KEY_VEHICLES_JSON] ?: "[]") }.getOrElse { JSONArray() }
        val vehicles = buildList {
            for (i in 0 until vehArr.length()) {
                val o = vehArr.optJSONObject(i) ?: continue
                add(o.optString("id") to o.optString("name"))
            }
        }

        val config = WidgetConfigPrefs(
            themeMode = WidgetThemeMode.fromRaw(prefs[KEY_THEME_MODE]),
            opacityPercent = prefs[KEY_OPACITY] ?: 95,
            compactMode = prefs[KEY_COMPACT] ?: false,
            filterVehicleId = prefs[KEY_FILTER_VEHICLE] ?: "ALL"
        )

        return WidgetSnapshot(tasks = tasks, vehicles = vehicles, config = config)
    }
}
