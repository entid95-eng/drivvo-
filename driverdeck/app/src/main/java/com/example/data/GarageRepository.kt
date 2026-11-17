package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.CURRENT_SCHEMA_VERSION
import com.example.model.IconStyle
import com.example.model.LogRecord
import com.example.model.RecordType
import com.example.model.ReminderTask
import com.example.model.ReminderTriggerType
import com.example.model.TaskPriority
import com.example.model.Vehicle
import com.example.model.VehicleCategory
import com.example.model.VehicleClass
import com.example.model.normalizeHex
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GarageRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Необходимо войти через Google.")
        if (expectedUserId != null) {
            check(uid == expectedUserId) { "Пользователь изменился; запись отклонена." }
        }
        return uid
    }

    private fun vehiclesCollection(uid: String) =
        db.collection("users").document(uid).collection("vehicles")

    private fun recordsCollection(uid: String) =
        db.collection("users").document(uid).collection("records")

    private fun remindersCollection(uid: String) =
        db.collection("users").document(uid).collection("reminders")

    fun observeVehicles(userId: String): Flow<List<Vehicle>> = flow {
        requireUserId(userId)
        val path = "users/$userId/vehicles"
        emitAll(
            vehiclesCollection(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Vehicle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeRecords(userId: String): Flow<List<LogRecord>> = flow {
        requireUserId(userId)
        val path = "users/$userId/records"
        emitAll(
            recordsCollection(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LogRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.eventTimestampMs }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeReminders(userId: String): Flow<List<ReminderTask>> = flow {
        requireUserId(userId)
        val path = "users/$userId/reminders"
        emitAll(
            remindersCollection(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ReminderTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun fetchSnapshotOnce(userId: String): Result<Triple<List<Vehicle>, List<LogRecord>, List<ReminderTask>>> {
        return try {
            val uid = requireUserId(userId)
            val vSnap = vehiclesCollection(uid).get().await()
            val rSnap = recordsCollection(uid).get().await()
            val tSnap = remindersCollection(uid).get().await()
            val vehicles = vSnap.toObjects(Vehicle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val records = rSnap.toObjects(LogRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.eventTimestampMs }
            val reminders = tSnap.toObjects(ReminderTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(Triple(vehicles, records, reminders))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users/$userId")
            Result.failure(e)
        }
    }

    suspend fun saveVehicle(userId: String, vehicle: Vehicle): Result<String> {
        val isNew = vehicle.id.isBlank()
        val docRef = if (isNew) vehiclesCollection(userId).document() else vehiclesCollection(userId).document(vehicle.id)
        return try {
            val uid = requireUserId(userId)
            require(vehicle.displayTitle().isNotBlank()) { "Укажите название или модель транспорта" }
            if (isNew) {
                docRef.set(vehicle.toCreateMap(uid)).await()
            } else {
                docRef.update(vehicle.toUpdateMap()).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateVehicleOdometer(userId: String, vehicleId: String, odometerKm: Long): Result<Unit> {
        val docRef = vehiclesCollection(userId).document(vehicleId)
        return try {
            requireUserId(userId)
            require(vehicleId.isNotBlank())
            docRef.update(
                mapOf(
                    "odometerKm" to odometerKm.coerceIn(0L, 5_000_000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateVehicleStyle(
        userId: String,
        vehicleId: String,
        iconStyle: IconStyle,
        accentColorHex: String
    ): Result<Unit> {
        val docRef = vehiclesCollection(userId).document(vehicleId)
        return try {
            requireUserId(userId)
            require(vehicleId.isNotBlank())
            docRef.update(
                mapOf(
                    "iconStyle" to iconStyle.name,
                    "accentColorHex" to normalizeHex(accentColorHex),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteVehicle(userId: String, vehicleId: String): Result<Unit> {
        val docRef = vehiclesCollection(userId).document(vehicleId)
        return try {
            val uid = requireUserId(userId)
            require(vehicleId.isNotBlank())
            val linkedRecords = recordsCollection(uid).whereEqualTo("vehicleId", vehicleId).get().await()
            val linkedReminders = remindersCollection(uid).whereEqualTo("vehicleId", vehicleId).get().await()
            val batch = db.batch()
            linkedRecords.documents.forEach { batch.delete(it.reference) }
            linkedReminders.documents.forEach { batch.delete(it.reference) }
            batch.delete(docRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveRecord(
        userId: String,
        record: LogRecord,
        currentVehicleOdometerKm: Long? = null
    ): Result<String> {
        val isNew = record.id.isBlank()
        val docRef = if (isNew) recordsCollection(userId).document() else recordsCollection(userId).document(record.id)
        return try {
            val uid = requireUserId(userId)
            require(record.vehicleId.isNotBlank()) { "Выберите транспорт" }
            if (isNew) {
                docRef.set(record.toCreateMap(uid)).await()
            } else {
                docRef.update(record.toUpdateMap()).await()
            }
            if (currentVehicleOdometerKm != null && record.odometerKm > currentVehicleOdometerKm) {
                updateVehicleOdometer(uid, record.vehicleId, record.odometerKm)
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteRecord(userId: String, recordId: String): Result<Unit> {
        val docRef = recordsCollection(userId).document(recordId)
        return try {
            requireUserId(userId)
            require(recordId.isNotBlank())
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveReminder(userId: String, reminder: ReminderTask): Result<String> {
        val isNew = reminder.id.isBlank()
        val docRef = if (isNew) remindersCollection(userId).document() else remindersCollection(userId).document(reminder.id)
        return try {
            val uid = requireUserId(userId)
            require(reminder.vehicleId.isNotBlank()) { "Выберите транспорт" }
            if (isNew) {
                docRef.set(reminder.toCreateMap(uid)).await()
            } else {
                docRef.update(reminder.toUpdateMap()).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleReminderCompleted(
        userId: String,
        reminder: ReminderTask,
        completed: Boolean,
        vehicleOdometerKm: Long
    ): Result<Unit> {
        val docRef = remindersCollection(userId).document(reminder.id)
        return try {
            val uid = requireUserId(userId)
            require(reminder.id.isNotBlank())
            val nowMs = System.currentTimeMillis()
            docRef.update(
                mapOf(
                    "completed" to completed,
                    "completedAtEpochMs" to if (completed) nowMs else 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // If recurring task is marked completed, schedule next occurrence automatically
            if (completed && (reminder.repeatIntervalDays > 0L || reminder.repeatIntervalKm > 0L)) {
                val nextDate = if (reminder.repeatIntervalDays > 0L) {
                    nowMs + reminder.repeatIntervalDays * 86_400_000L
                } else 0L
                val nextOdo = if (reminder.repeatIntervalKm > 0L) {
                    (maxOf(vehicleOdometerKm, reminder.dueOdometerKm) + reminder.repeatIntervalKm)
                        .coerceAtMost(5_000_000L)
                } else 0L
                val nextTrigger = when {
                    nextDate > 0L && nextOdo > 0L -> ReminderTriggerType.BOTH
                    nextOdo > 0L -> ReminderTriggerType.BY_MILEAGE
                    else -> ReminderTriggerType.BY_DATE
                }
                val nextTask = reminder.copy(
                    id = "",
                    triggerType = nextTrigger.name,
                    dueDateEpochMs = nextDate,
                    dueOdometerKm = nextOdo,
                    completed = false,
                    completedAtEpochMs = 0L
                )
                remindersCollection(uid).document().set(nextTask.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleReminderCompletedById(
        userId: String,
        reminderId: String,
        completed: Boolean
    ): Result<Unit> {
        val docRef = remindersCollection(userId).document(reminderId)
        return try {
            val uid = requireUserId(userId)
            val snap = docRef.get().await()
            val task = snap.toObject(ReminderTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Задача не найдена"))
            val vehSnap = vehiclesCollection(uid).document(task.vehicleId).get().await()
            val odo = vehSnap.getLong("odometerKm") ?: 0L
            toggleReminderCompleted(uid, task, completed, odo)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(userId: String, reminderId: String): Result<Unit> {
        val docRef = remindersCollection(userId).document(reminderId)
        return try {
            requireUserId(userId)
            require(reminderId.isNotBlank())
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun exportBackupJson(userId: String): Result<String> {
        return fetchSnapshotOnce(userId).map { (vehicles, records, reminders) ->
            val root = JSONObject().apply {
                put("app", "DriveDeck")
                put("schemaVersion", CURRENT_SCHEMA_VERSION)
                put("exportedAtMs", System.currentTimeMillis())
                put("vehicles", JSONArray().apply {
                    vehicles.forEach { v ->
                        put(JSONObject().apply {
                            put("id", v.id)
                            put("name", v.name)
                            put("brand", v.brand)
                            put("model", v.model)
                            put("year", v.year)
                            put("category", v.category)
                            put("vehicleClass", v.vehicleClass)
                            put("iconStyle", v.iconStyle)
                            put("accentColorHex", v.accentColorHex)
                            put("odometerKm", v.odometerKm)
                            put("fuelType", v.fuelType)
                            put("tankCapacityLiters", v.tankCapacityLiters)
                        })
                    }
                })
                put("records", JSONArray().apply {
                    records.forEach { r ->
                        put(JSONObject().apply {
                            put("id", r.id)
                            put("vehicleId", r.vehicleId)
                            put("recordType", r.recordType)
                            put("title", r.title)
                            put("categoryTag", r.categoryTag)
                            put("amountCost", r.amountCost)
                            put("odometerKm", r.odometerKm)
                            put("liters", r.liters)
                            put("pricePerLiter", r.pricePerLiter)
                            put("fullTank", r.fullTank)
                            put("notes", r.notes)
                            put("eventTimestampMs", r.eventTimestampMs)
                        })
                    }
                })
                put("reminders", JSONArray().apply {
                    reminders.forEach { t ->
                        put(JSONObject().apply {
                            put("id", t.id)
                            put("vehicleId", t.vehicleId)
                            put("title", t.title)
                            put("notes", t.notes)
                            put("triggerType", t.triggerType)
                            put("dueDateEpochMs", t.dueDateEpochMs)
                            put("dueOdometerKm", t.dueOdometerKm)
                            put("repeatIntervalDays", t.repeatIntervalDays)
                            put("repeatIntervalKm", t.repeatIntervalKm)
                            put("priority", t.priority)
                            put("completed", t.completed)
                            put("completedAtEpochMs", t.completedAtEpochMs)
                        })
                    }
                })
            }
            root.toString(2)
        }
    }

    suspend fun exportCsv(userId: String): Result<String> {
        return fetchSnapshotOnce(userId).map { (vehicles, records, _) ->
            val vMap = vehicles.associateBy { it.id }
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            buildString {
                appendLine("Дата;Транспорт;Класс;Тип;Название;Категория;Пробег_км;Литры;Цена_за_л;Сумма;Полный_бак;Заметка")
                records.forEach { r ->
                    val v = vMap[r.vehicleId]
                    val dateStr = sdf.format(Date(r.eventTimestampMs))
                    val row = listOf(
                        dateStr,
                        v?.displayTitle() ?: r.vehicleId,
                        v?.safeVehicleClass()?.labelRu ?: "",
                        r.safeRecordType().labelRu,
                        r.title,
                        r.categoryTag,
                        r.odometerKm.toString(),
                        String.format(Locale.US, "%.2f", r.liters),
                        String.format(Locale.US, "%.2f", r.pricePerLiter),
                        String.format(Locale.US, "%.2f", r.amountCost),
                        if (r.fullTank) "Да" else "Нет",
                        r.notes.replace(";", ",").replace("\n", " ")
                    ).joinToString(";")
                    appendLine(row)
                }
            }
        }
    }

    suspend fun importBackupJson(userId: String, rawJson: String): Result<Int> {
        return try {
            val uid = requireUserId(userId)
            val root = JSONObject(rawJson)
            val vehArr = root.optJSONArray("vehicles") ?: JSONArray()
            val recArr = root.optJSONArray("records") ?: JSONArray()
            val remArr = root.optJSONArray("reminders") ?: JSONArray()

            val oldToNewVehicleIds = mutableMapOf<String, String>()
            var count = 0

            for (i in 0 until vehArr.length()) {
                val o = vehArr.getJSONObject(i)
                val oldId = o.optString("id")
                val vehicle = Vehicle(
                    userId = uid,
                    schemaVersion = CURRENT_SCHEMA_VERSION,
                    name = o.optString("name", "Транспорт"),
                    brand = o.optString("brand"),
                    model = o.optString("model"),
                    year = o.optLong("year", 2022L),
                    category = VehicleCategory.entries.firstOrNull { it.name == o.optString("category") }?.name
                        ?: VehicleClass.fromRaw(o.optString("vehicleClass")).category.name,
                    vehicleClass = VehicleClass.fromRaw(o.optString("vehicleClass")).name,
                    iconStyle = IconStyle.fromRaw(o.optString("iconStyle")).name,
                    accentColorHex = normalizeHex(o.optString("accentColorHex", "#1D63DC")),
                    odometerKm = o.optLong("odometerKm", 0L),
                    fuelType = o.optString("fuelType", "АИ-95"),
                    tankCapacityLiters = o.optDouble("tankCapacityLiters", 50.0)
                )
                val docRef = vehiclesCollection(uid).document()
                docRef.set(vehicle.toCreateMap(uid)).await()
                if (oldId.isNotBlank()) oldToNewVehicleIds[oldId] = docRef.id
                count++
            }

            for (i in 0 until recArr.length()) {
                val o = recArr.getJSONObject(i)
                val mappedVehId = oldToNewVehicleIds[o.optString("vehicleId")] ?: continue
                val record = LogRecord(
                    userId = uid,
                    schemaVersion = CURRENT_SCHEMA_VERSION,
                    vehicleId = mappedVehId,
                    recordType = RecordType.fromRaw(o.optString("recordType")).name,
                    title = o.optString("title", "Запись"),
                    categoryTag = o.optString("categoryTag", "Прочее"),
                    amountCost = o.optDouble("amountCost", 0.0),
                    odometerKm = o.optLong("odometerKm", 0L),
                    liters = o.optDouble("liters", 0.0),
                    pricePerLiter = o.optDouble("pricePerLiter", 0.0),
                    fullTank = o.optBoolean("fullTank", true),
                    notes = o.optString("notes"),
                    eventTimestampMs = o.optLong("eventTimestampMs", System.currentTimeMillis())
                )
                recordsCollection(uid).document().set(record.toCreateMap(uid)).await()
                count++
            }

            for (i in 0 until remArr.length()) {
                val o = remArr.getJSONObject(i)
                val mappedVehId = oldToNewVehicleIds[o.optString("vehicleId")] ?: continue
                val reminder = ReminderTask(
                    userId = uid,
                    schemaVersion = CURRENT_SCHEMA_VERSION,
                    vehicleId = mappedVehId,
                    title = o.optString("title", "Задача"),
                    notes = o.optString("notes"),
                    triggerType = ReminderTriggerType.fromRaw(o.optString("triggerType")).name,
                    dueDateEpochMs = o.optLong("dueDateEpochMs", 0L),
                    dueOdometerKm = o.optLong("dueOdometerKm", 0L),
                    repeatIntervalDays = o.optLong("repeatIntervalDays", 0L),
                    repeatIntervalKm = o.optLong("repeatIntervalKm", 0L),
                    priority = TaskPriority.fromRaw(o.optString("priority")).name,
                    completed = o.optBoolean("completed", false),
                    completedAtEpochMs = o.optLong("completedAtEpochMs", 0L)
                )
                remindersCollection(uid).document().set(reminder.toCreateMap(uid)).await()
                count++
            }

            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("GarageRepo", "Backup import failed", e)
            Result.failure(e)
        }
    }
}
