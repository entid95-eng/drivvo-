package com.example.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.R
import com.example.data.GarageRepository
import com.example.data.WidgetSnapshot
import com.example.data.WidgetSyncStore
import com.example.data.WidgetTaskItem
import com.example.data.WidgetThemeMode
import com.example.model.TaskPriority
import com.example.model.TaskUrgency
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

val KEY_TASK_ID = ActionParameters.Key<String>("task_id")

class ToggleTaskWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[KEY_TASK_ID] ?: return
        WidgetSyncStore.markTaskCompletedLocally(context, taskId)
        val uid = Firebase.auth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            GarageRepository(context).toggleReminderCompletedById(uid, taskId, completed = true)
        }
    }
}

class CycleFilterWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        WidgetSyncStore.cycleFilterVehicle(context)
    }
}

class DriveDeckWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initialSnapshot = WidgetSyncStore.getSnapshot(context)
        provideContent {
            val snapshot by WidgetSyncStore.observeSnapshot(context)
                .collectAsState(initial = initialSnapshot)
            WidgetBody(snapshot = snapshot)
        }
    }

    @Composable
    private fun WidgetBody(snapshot: WidgetSnapshot) {
        val context = LocalContext.current
        val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val isDark = when (snapshot.config.themeMode) {
            WidgetThemeMode.SYSTEM -> isSystemDark
            WidgetThemeMode.DARK -> true
            WidgetThemeMode.LIGHT -> false
        }

        val alpha = (snapshot.config.opacityPercent.coerceIn(40, 100) / 100f)
        val bgBase = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
        val bgColor = bgBase.copy(alpha = alpha)
        val cardBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.92f) else Color(0xFFF4F6FA)
        val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
        val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
        val accentBlue = Color(0xFF1D63DC)

        val filteredTasks = if (snapshot.config.filterVehicleId == "ALL") {
            snapshot.tasks
        } else {
            snapshot.tasks.filter { it.vehicleId == snapshot.config.filterVehicleId }
        }

        val filterLabel = if (snapshot.config.filterVehicleId == "ALL") {
            "Весь гараж"
        } else {
            snapshot.vehicles.firstOrNull { it.first == snapshot.config.filterVehicleId }?.second ?: "Авто"
        }

        val addIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("widget_action", "add_task")
        }
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("widget_action", "open_tasks")
        }

        val containerModifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .let { mod ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    mod.cornerRadius(android.R.dimen.system_app_widget_background_radius)
                } else {
                    mod.cornerRadius(18.dp)
                }
            }
            .padding(if (snapshot.config.compactMode) 10.dp else 14.dp)

        Column(modifier = containerModifier) {
            // Todoist-style Widget Top Bar
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity(openAppIntent))
                ) {
                    Text(
                        text = "Задачи ТО",
                        style = TextStyle(
                            color = ColorProvider(textPrimary),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "$filterLabel • ${filteredTasks.size}",
                        style = TextStyle(
                            color = ColorProvider(accentBlue),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = GlanceModifier.clickable(actionRunCallback<CycleFilterWidgetAction>())
                    )
                }

                Box(
                    modifier = GlanceModifier
                        .size(32.dp)
                        .background(accentBlue)
                        .cornerRadius(10.dp)
                        .clickable(actionStartActivity(addIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_plus),
                        contentDescription = "Добавить задачу",
                        modifier = GlanceModifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(if (snapshot.config.compactMode) 6.dp else 10.dp))

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(cardBg)
                        .cornerRadius(12.dp)
                        .clickable(actionStartActivity(openAppIntent))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Все задачи выполнены",
                            style = TextStyle(
                                color = ColorProvider(textPrimary),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "Нажмите + чтобы добавить напоминание",
                            style = TextStyle(
                                color = ColorProvider(textSecondary),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(
                        items = filteredTasks.take(12),
                        itemId = { it.id.hashCode().toLong() }
                    ) { item ->
                        Column {
                            WidgetTaskRow(
                                item = item,
                                cardBg = cardBg,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                compact = snapshot.config.compactMode,
                                openAppIntent = openAppIntent
                            )
                            Spacer(modifier = GlanceModifier.height(if (snapshot.config.compactMode) 4.dp else 6.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun WidgetTaskRow(
        item: WidgetTaskItem,
        cardBg: Color,
        textPrimary: Color,
        textSecondary: Color,
        compact: Boolean,
        openAppIntent: Intent
    ) {
        val priorityColor = when {
            item.urgency == TaskUrgency.OVERDUE || item.priority == TaskPriority.HIGH -> Color(0xFFEF4444)
            item.urgency == TaskUrgency.DUE_SOON || item.priority == TaskPriority.MEDIUM -> Color(0xFFF59E0B)
            else -> Color(0xFF1D63DC)
        }
        val vehAccent = runCatching { Color(android.graphics.Color.parseColor(item.accentHex)) }
            .getOrElse { Color(0xFF1D63DC) }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(cardBg)
                .cornerRadius(12.dp)
                .padding(horizontal = 8.dp, vertical = if (compact) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Todoist-style urgency stripe
            Box(
                modifier = GlanceModifier
                    .width(3.dp)
                    .height(if (compact) 24.dp else 28.dp)
                    .background(priorityColor)
                    .cornerRadius(2.dp)
            ) {}

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Vehicle class silhouette icon
            Image(
                provider = ImageProvider(item.vehicleClass.widgetDrawableRes),
                contentDescription = item.vehicleClass.labelRu,
                colorFilter = ColorFilter.tint(ColorProvider(vehAccent)),
                modifier = GlanceModifier.size(width = 30.dp, height = 16.dp)
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity(openAppIntent))
            ) {
                Text(
                    text = item.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(textPrimary),
                        fontSize = if (compact) 12.sp else 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "${item.vehicleName} • ${item.badgeText}",
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(
                            if (item.urgency == TaskUrgency.OVERDUE) Color(0xFFEF4444) else textSecondary
                        ),
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            // Interactive Todoist circular checkbox
            Box(
                modifier = GlanceModifier
                    .size(28.dp)
                    .clickable(
                        actionRunCallback<ToggleTaskWidgetAction>(
                            actionParametersOf(KEY_TASK_ID to item.id)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(
                        if (item.completed) R.drawable.ic_widget_checked else R.drawable.ic_widget_circle
                    ),
                    contentDescription = "Отметить выполненной",
                    colorFilter = if (!item.completed) ColorFilter.tint(ColorProvider(priorityColor)) else null,
                    modifier = GlanceModifier.size(20.dp)
                )
            }
        }
    }
}
