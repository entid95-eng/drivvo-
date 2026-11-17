package com.example.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlin.math.roundToLong

const val CURRENT_SCHEMA_VERSION = 1L

enum class VehicleCategory(val labelRu: String) {
    CAR("Авто"),
    MOTO("Мото")
}

enum class VehicleClass(
    val category: VehicleCategory,
    val labelRu: String,
    val descriptionRu: String,
    @DrawableRes val widgetDrawableRes: Int
) {
    SEDAN(VehicleCategory.CAR, "Седан", "Классический трёхобъёмный кузов", R.drawable.ic_veh_sedan),
    CROSSOVER(VehicleCategory.CAR, "Кроссовер", "Повышенный клиренс и рейлинги", R.drawable.ic_veh_crossover),
    WAGON(VehicleCategory.CAR, "Универсал", "Длинная крыша и большой багажник", R.drawable.ic_veh_wagon),
    NAKED(VehicleCategory.MOTO, "Нейкед", "Открытая рама и прямой руль", R.drawable.ic_veh_naked),
    SPORT(VehicleCategory.MOTO, "Спорт", "Аэродинамический обтекатель и клипоны", R.drawable.ic_veh_sport),
    TOUR_ENDURO(VehicleCategory.MOTO, "Турэндуро", "Высокая посадка, клюв и защита рук", R.drawable.ic_veh_tour_enduro),
    SCOOTER(VehicleCategory.MOTO, "Скутер", "Городской с ровным поликом", R.drawable.ic_veh_scooter),
    TOURING(VehicleCategory.MOTO, "Туристический", "Высокий ветровик и боковые кофры", R.drawable.ic_veh_touring);

    companion object {
        fun fromRaw(raw: String?): VehicleClass =
            entries.firstOrNull { it.name == raw } ?: SEDAN

        fun byCategory(category: VehicleCategory): List<VehicleClass> =
            entries.filter { it.category == category }
    }
}

enum class IconStyle(val labelRu: String, val subtitleRu: String) {
    SOLID("Монолит", "Плотный контрастный силуэт"),
    OUTLINE("Чертёж", "Технический контурный профиль"),
    TWO_TONE("Аэро", "Двухцветная окраска и детали");

    companion object {
        fun fromRaw(raw: String?): IconStyle =
            entries.firstOrNull { it.name == raw } ?: TWO_TONE
    }
}

enum class RecordType(val labelRu: String) {
    REFUEL("Заправка"),
    SERVICE("Сервис / ТО"),
    EXPENSE("Расход");

    companion object {
        fun fromRaw(raw: String?): RecordType =
            entries.firstOrNull { it.name == raw } ?: EXPENSE
    }
}

enum class ReminderTriggerType(val labelRu: String) {
    BY_DATE("По дате"),
    BY_MILEAGE("По пробегу"),
    BOTH("Дата или пробег");

    companion object {
        fun fromRaw(raw: String?): ReminderTriggerType =
            entries.firstOrNull { it.name == raw } ?: BOTH
    }
}

enum class TaskPriority(val labelRu: String) {
    HIGH("Срочно"),
    MEDIUM("Скоро"),
    LOW("Планово");

    companion object {
        fun fromRaw(raw: String?): TaskPriority =
            entries.firstOrNull { it.name == raw } ?: MEDIUM
    }
}

enum class TaskUrgency {
    OVERDUE,
    DUE_SOON,
    UPCOMING,
    COMPLETED
}

data class TaskUrgencyInfo(
    val urgency: TaskUrgency,
    val badgeText: String,
    val sortScore: Long
)

@Keep
@IgnoreExtraProperties
data class Vehicle(
    @DocumentId val id: String = "",
    val userId: String = "",
    val schemaVersion: Long = CURRENT_SCHEMA_VERSION,
    val name: String = "",
    val brand: String = "",
    val model: String = "",
    val year: Long = 2022L,
    val category: String = VehicleCategory.CAR.name,
    val vehicleClass: String = VehicleClass.SEDAN.name,
    val iconStyle: String = IconStyle.TWO_TONE.name,
    val accentColorHex: String = "#1D63DC",
    val odometerKm: Long = 0L,
    val fuelType: String = "АИ-95",
    val tankCapacityLiters: Double = 55.0,
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun safeCategory(): VehicleCategory =
        VehicleCategory.entries.firstOrNull { it.name == category } ?: safeVehicleClass().category

    fun safeVehicleClass(): VehicleClass = VehicleClass.fromRaw(vehicleClass)

    fun safeIconStyle(): IconStyle = IconStyle.fromRaw(iconStyle)

    fun displayTitle(): String =
        name.ifBlank { listOf(brand, model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Транспорт" } }

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "schemaVersion" to CURRENT_SCHEMA_VERSION,
        "name" to displayTitle().take(60),
        "brand" to brand.trim().take(40),
        "model" to model.trim().take(40),
        "year" to year.coerceIn(1950L, 2100L),
        "category" to safeCategory().name,
        "vehicleClass" to safeVehicleClass().name,
        "iconStyle" to safeIconStyle().name,
        "accentColorHex" to normalizeHex(accentColorHex),
        "odometerKm" to odometerKm.coerceIn(0L, 5_000_000L),
        "fuelType" to fuelType.trim().ifBlank { "АИ-95" }.take(24),
        "tankCapacityLiters" to tankCapacityLiters.coerceIn(0.0, 500.0),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "schemaVersion" to CURRENT_SCHEMA_VERSION,
        "name" to displayTitle().take(60),
        "brand" to brand.trim().take(40),
        "model" to model.trim().take(40),
        "year" to year.coerceIn(1950L, 2100L),
        "category" to safeCategory().name,
        "vehicleClass" to safeVehicleClass().name,
        "iconStyle" to safeIconStyle().name,
        "accentColorHex" to normalizeHex(accentColorHex),
        "odometerKm" to odometerKm.coerceIn(0L, 5_000_000L),
        "fuelType" to fuelType.trim().ifBlank { "АИ-95" }.take(24),
        "tankCapacityLiters" to tankCapacityLiters.coerceIn(0.0, 500.0),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class LogRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val schemaVersion: Long = CURRENT_SCHEMA_VERSION,
    val vehicleId: String = "",
    val recordType: String = RecordType.REFUEL.name,
    val title: String = "",
    val categoryTag: String = "АИ-95",
    val amountCost: Double = 0.0,
    val odometerKm: Long = 0L,
    val liters: Double = 0.0,
    val pricePerLiter: Double = 0.0,
    val fullTank: Boolean = true,
    val notes: String = "",
    val eventTimestampMs: Long = System.currentTimeMillis(),
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun safeRecordType(): RecordType = RecordType.fromRaw(recordType)

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "schemaVersion" to CURRENT_SCHEMA_VERSION,
        "vehicleId" to vehicleId.trim(),
        "recordType" to safeRecordType().name,
        "title" to title.trim().ifBlank { safeRecordType().labelRu }.take(80),
        "categoryTag" to categoryTag.trim().ifBlank { safeRecordType().labelRu }.take(40),
        "amountCost" to amountCost.coerceIn(0.0, 10_000_000.0),
        "odometerKm" to odometerKm.coerceIn(0L, 5_000_000L),
        "liters" to liters.coerceIn(0.0, 1000.0),
        "pricePerLiter" to pricePerLiter.coerceIn(0.0, 10_000.0),
        "fullTank" to fullTank,
        "notes" to notes.trim().take(500),
        "eventTimestampMs" to eventTimestampMs.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "schemaVersion" to CURRENT_SCHEMA_VERSION,
        "vehicleId" to vehicleId.trim(),
        "recordType" to safeRecordType().name,
        "title" to title.trim().ifBlank { safeRecordType().labelRu }.take(80),
        "categoryTag" to categoryTag.trim().ifBlank { safeRecordType().labelRu }.take(40),
        "amountCost" to amountCost.coerceIn(0.0, 10_000_000.0),
        "odometerKm" to odometerKm.coerceIn(0L, 5_000_000L),
        "liters" to liters.coerceIn(0.0, 1000.0),
        "pricePerLiter" to pricePerLiter.coerceIn(0.0, 10_000.0),
        "fullTank" to fullTank,
        "notes" to notes.trim().take(500),
        "eventTimestampMs" to eventTimestampMs.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ReminderTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val schemaVersion: Long = CURRENT_SCHEMA_VERSION,
    val vehicleId: String = "",
    val title: String = "",
    val notes: String = "",
    val triggerType: String = ReminderTriggerType.BOTH.name,
    val dueDateEpochMs: Long = 0L,
    val dueOdometerKm: Long = 0L,
    val repeatIntervalDays: Long = 0L,
    val repeatIntervalKm: Long = 0L,
    val priority: String = TaskPriority.MEDIUM.name,
    val completed: Boolean = false,
    val completedAtEpochMs: Long = 0L,
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun safeTriggerType(): ReminderTriggerType = ReminderTriggerType.fromRaw(triggerType)

    fun safePriority(): TaskPriority = TaskPriority.fromRaw(priority)

    fun computeUrgency(vehicleOdometerKm: Long, nowMs: Long = System.currentTimeMillis()): TaskUrgencyInfo {
        if (completed) {
            return TaskUrgencyInfo(TaskUrgency.COMPLETED, "Выполнено", Long.MAX_VALUE)
        }
        val trigger = safeTriggerType()
        val kmDiff = if (trigger != ReminderTriggerType.BY_DATE && dueOdometerKm > 0L) {
            dueOdometerKm - vehicleOdometerKm
        } else null

        val dayMillis = 86_400_000L
        val daysDiff = if (trigger != ReminderTriggerType.BY_MILEAGE && dueDateEpochMs > 0L) {
            ((dueDateEpochMs - nowMs).toDouble() / dayMillis).roundToLong()
        } else null

        val isOverdueKm = kmDiff != null && kmDiff <= 0L
        val isOverdueDate = daysDiff != null && daysDiff < 0L
        val isSoonKm = kmDiff != null && kmDiff in 1L..800L
        val isSoonDate = daysDiff != null && daysDiff in 0L..7L

        val urgency = when {
            isOverdueKm || isOverdueDate -> TaskUrgency.OVERDUE
            isSoonKm || isSoonDate || safePriority() == TaskPriority.HIGH -> TaskUrgency.DUE_SOON
            else -> TaskUrgency.UPCOMING
        }

        val parts = mutableListOf<String>()
        if (kmDiff != null) {
            parts += if (kmDiff <= 0L) "просрочено ${-kmDiff} км" else "через $kmDiff км"
        }
        if (daysDiff != null) {
            parts += when {
                daysDiff < 0L -> "просрочено ${-daysDiff} дн."
                daysDiff == 0L -> "сегодня"
                else -> "через $daysDiff дн."
            }
        }
        val badge = parts.joinToString(" • ").ifBlank { safePriority().labelRu }

        val minDaysWeight = daysDiff?.let { it * 1000L } ?: 1_000_000L
        val minKmWeight = kmDiff ?: 1_000_000L
        val score = minOf(minDaysWeight, minKmWeight)
        return TaskUrgencyInfo(urgency, badge, score)
    }

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "schemaVersion" to CURRENT_SCHEMA_VERSION,
        "vehicleId" to vehicleId.trim(),
        "title" to title.trim().ifBlank { "Задача по ТО" }.take(80),
        "notes" to notes.trim().take(500),
        "triggerType" to safeTriggerType().name,
        "dueDateEpochMs" to dueDateEpochMs.coerceAtLeast(0L),
        "dueOdometerKm" to dueOdometerKm.coerceIn(0L, 5_000_000L),
        "repeatIntervalDays" to repeatIntervalDays.coerceIn(0L, 3650L),
        "repeatIntervalKm" to repeatIntervalKm.coerceIn(0L, 500_000L),
        "priority" to safePriority().name,
        "completed" to completed,
        "completedAtEpochMs" to completedAtEpochMs.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "schemaVersion" to CURRENT_SCHEMA_VERSION,
        "vehicleId" to vehicleId.trim(),
        "title" to title.trim().ifBlank { "Задача по ТО" }.take(80),
        "notes" to notes.trim().take(500),
        "triggerType" to safeTriggerType().name,
        "dueDateEpochMs" to dueDateEpochMs.coerceAtLeast(0L),
        "dueOdometerKm" to dueOdometerKm.coerceIn(0L, 5_000_000L),
        "repeatIntervalDays" to repeatIntervalDays.coerceIn(0L, 3650L),
        "repeatIntervalKm" to repeatIntervalKm.coerceIn(0L, 500_000L),
        "priority" to safePriority().name,
        "completed" to completed,
        "completedAtEpochMs" to completedAtEpochMs.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

data class VehicleStats(
    val avgLitersPer100Km: Double? = null,
    val totalSpent: Double = 0.0,
    val fuelSpent: Double = 0.0,
    val serviceSpent: Double = 0.0,
    val expenseSpent: Double = 0.0,
    val costPerKm: Double? = null,
    val monthlyTotals: List<Pair<String, Double>> = emptyList()
)

fun normalizeHex(input: String): String {
    val clean = input.trim()
    val valid = Regex("^#[0-9a-fA-F]{6,8}$")
    return if (valid.matches(clean)) clean.uppercase() else "#1D63DC"
}

fun calculateVehicleStats(records: List<LogRecord>): VehicleStats {
    val fuelRecords = records.filter { it.safeRecordType() == RecordType.REFUEL }
    val serviceRecords = records.filter { it.safeRecordType() == RecordType.SERVICE }
    val expenseRecords = records.filter { it.safeRecordType() == RecordType.EXPENSE }

    val fuelSpent = fuelRecords.sumOf { it.amountCost }
    val serviceSpent = serviceRecords.sumOf { it.amountCost }
    val expenseSpent = expenseRecords.sumOf { it.amountCost }
    val totalSpent = fuelSpent + serviceSpent + expenseSpent

    // Fuel consumption between consecutive full-tank refuels ordered by odometer
    val sortedFuel = fuelRecords.filter { it.odometerKm > 0 && it.liters > 0 }.sortedBy { it.odometerKm }
    var avgConsumption: Double? = null
    if (sortedFuel.size >= 2) {
        val fullIndices = sortedFuel.mapIndexedNotNull { idx, r -> if (r.fullTank) idx else null }
        if (fullIndices.size >= 2) {
            val firstIdx = fullIndices.first()
            val lastIdx = fullIndices.last()
            val dist = sortedFuel[lastIdx].odometerKm - sortedFuel[firstIdx].odometerKm
            val consumedLiters = sortedFuel.subList(firstIdx + 1, lastIdx + 1).sumOf { it.liters }
            if (dist > 0 && consumedLiters > 0) {
                avgConsumption = (consumedLiters / dist.toDouble()) * 100.0
            }
        }
    }

    val odos = records.mapNotNull { if (it.odometerKm > 0) it.odometerKm else null }
    val costPerKm = if (odos.size >= 2) {
        val span = (odos.max() - odos.min())
        if (span > 0) totalSpent / span.toDouble() else null
    } else null

    return VehicleStats(
        avgLitersPer100Km = avgConsumption,
        totalSpent = totalSpent,
        fuelSpent = fuelSpent,
        serviceSpent = serviceSpent,
        expenseSpent = expenseSpent,
        costPerKm = costPerKm
    )
}
