package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GarageRepository
import com.example.model.IconStyle
import com.example.model.LogRecord
import com.example.model.ReminderTask
import com.example.model.Vehicle
import com.example.model.VehicleStats
import com.example.model.calculateVehicleStats
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class GarageDashboardData(
    val vehicles: List<Vehicle>,
    val selectedVehicle: Vehicle?,
    val records: List<LogRecord>,
    val vehicleRecords: List<LogRecord>,
    val reminders: List<ReminderTask>,
    val vehicleStats: VehicleStats
)

class GarageViewModel(
    private val repository: GarageRepository,
    private val currentUserId: String,
    private val syncWidget: suspend (List<Vehicle>, List<ReminderTask>) -> Unit
) : ViewModel() {

    private val _selectedVehicleId = MutableStateFlow<String?>(null)
    val selectedVehicleId: StateFlow<String?> = _selectedVehicleId.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val uiState: StateFlow<UiState<GarageDashboardData>> = combine(
        repository.observeVehicles(currentUserId),
        repository.observeRecords(currentUserId),
        repository.observeReminders(currentUserId),
        _selectedVehicleId
    ) { vehicles, records, reminders, chosenId ->
        val selected = vehicles.firstOrNull { it.id == chosenId } ?: vehicles.firstOrNull()
        val vehicleRecords = if (selected != null) {
            records.filter { it.vehicleId == selected.id }
        } else {
            records
        }
        val stats = calculateVehicleStats(vehicleRecords)
        GarageDashboardData(
            vehicles = vehicles,
            selectedVehicle = selected,
            records = records,
            vehicleRecords = vehicleRecords,
            reminders = reminders,
            vehicleStats = stats
        )
    }
        .onEach { data ->
            syncWidget(data.vehicles, data.reminders)
        }
        .map<GarageDashboardData, UiState<GarageDashboardData>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Error observing garage data", root)
            emit(UiState.Error(root.localizedMessage ?: "Ошибка синхронизации с облаком"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    fun selectVehicle(vehicleId: String) {
        _selectedVehicleId.value = vehicleId
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun saveVehicle(vehicle: Vehicle, onDone: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.saveVehicle(currentUserId, vehicle)
            _isSaving.value = false
            res.onSuccess { id ->
                _selectedVehicleId.value = id
                _snackbarMessage.value = if (vehicle.id.isBlank()) "Транспорт добавлен" else "Профиль обновлён"
                onDone(id)
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Не удалось сохранить транспорт"
            }
        }
    }

    fun updateVehicleOdometer(vehicleId: String, odometerKm: Long) {
        viewModelScope.launch {
            repository.updateVehicleOdometer(currentUserId, vehicleId, odometerKm)
                .onSuccess { _snackbarMessage.value = "Пробег обновлён: $odometerKm км" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Ошибка обновления пробега" }
        }
    }

    fun updateVehicleStyle(vehicleId: String, style: IconStyle, accentHex: String) {
        viewModelScope.launch {
            repository.updateVehicleStyle(currentUserId, vehicleId, style, accentHex)
                .onSuccess { _snackbarMessage.value = "Стиль силуэта обновлён" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Ошибка сохранения стиля" }
        }
    }

    fun deleteVehicle(vehicleId: String) {
        viewModelScope.launch {
            repository.deleteVehicle(currentUserId, vehicleId)
                .onSuccess {
                    if (_selectedVehicleId.value == vehicleId) _selectedVehicleId.value = null
                    _snackbarMessage.value = "Транспорт удалён"
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Не удалось удалить" }
        }
    }

    fun saveRecord(record: LogRecord, currentVehicleOdometer: Long?, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.saveRecord(currentUserId, record, currentVehicleOdometer)
            _isSaving.value = false
            res.onSuccess {
                _snackbarMessage.value = "${record.safeRecordType().labelRu} сохранена"
                onDone()
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Ошибка сохранения записи"
            }
        }
    }

    fun deleteRecord(recordId: String) {
        viewModelScope.launch {
            repository.deleteRecord(currentUserId, recordId)
                .onSuccess { _snackbarMessage.value = "Запись удалена" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Ошибка удаления" }
        }
    }

    fun saveReminder(task: ReminderTask, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.saveReminder(currentUserId, task)
            _isSaving.value = false
            res.onSuccess {
                _snackbarMessage.value = "Задача сохранена"
                onDone()
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Ошибка сохранения задачи"
            }
        }
    }

    fun toggleReminder(task: ReminderTask, completed: Boolean, vehicleOdometer: Long) {
        viewModelScope.launch {
            repository.toggleReminderCompleted(currentUserId, task, completed, vehicleOdometer)
                .onSuccess {
                    _snackbarMessage.value = if (completed) {
                        if (task.repeatIntervalDays > 0 || task.repeatIntervalKm > 0) {
                            "Выполнено • создана следующая повторяемая задача"
                        } else "Задача выполнена"
                    } else "Задача возвращена в активные"
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Ошибка обновления задачи" }
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            repository.deleteReminder(currentUserId, reminderId)
                .onSuccess { _snackbarMessage.value = "Задача удалена" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Ошибка удаления" }
        }
    }

    suspend fun exportBackupJson(): Result<String> = repository.exportBackupJson(currentUserId)

    suspend fun exportCsv(): Result<String> = repository.exportCsv(currentUserId)

    fun importBackupJson(json: String) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.importBackupJson(currentUserId, json)
            _isSaving.value = false
            res.onSuccess { count ->
                _snackbarMessage.value = "Импортировано объектов: $count"
            }.onFailure {
                _snackbarMessage.value = "Ошибка импорта: ${it.localizedMessage ?: "неверный формат JSON"}"
            }
        }
    }

    private companion object {
        const val TAG = "GarageViewModel"
    }
}
