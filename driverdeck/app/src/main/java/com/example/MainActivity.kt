package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Garage
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.GarageRepository
import com.example.data.WidgetSyncStore
import com.example.model.LogRecord
import com.example.model.RecordType
import com.example.model.ReminderTask
import com.example.model.Vehicle
import com.example.ui.GarageDashboardData
import com.example.ui.GarageViewModel
import com.example.ui.UiState
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GarageAndPcScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.sheets.OdometerQuickDialog
import com.example.ui.sheets.RecordEditorDialog
import com.example.ui.sheets.ReminderEditorDialog
import com.example.ui.sheets.VehicleEditorDialog
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.DriveDeckTheme
import com.example.ui.theme.ExpenseIndigo
import com.example.ui.theme.FuelAmber
import com.example.ui.theme.ServiceEmerald
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class DriveTab(
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Журнал", Icons.Rounded.DirectionsCar, "nav_dashboard"),
    TASKS("Задачи", Icons.Rounded.Checklist, "nav_tasks"),
    ANALYTICS("Отчёты", Icons.Rounded.BarChart, "nav_analytics"),
    GARAGE_PC("Гараж и ПК", Icons.Rounded.Garage, "nav_garage_pc")
}

class MainActivity : ComponentActivity() {
    private var widgetActionState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        widgetActionState = intent?.getStringExtra("widget_action")

        setContent {
            DriveDeckTheme {
                var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { auth ->
                        currentUser = auth.currentUser
                    }
                    Firebase.auth.addAuthStateListener(listener)
                    onDispose {
                        Firebase.auth.removeAuthStateListener(listener)
                    }
                }

                val user = currentUser
                if (user == null) {
                    AuthScreen(onAuthSuccess = {
                        currentUser = Firebase.auth.currentUser
                    })
                } else {
                    key(user.uid) {
                        DriveDeckAuthenticatedApp(
                            uid = user.uid,
                            widgetAction = widgetActionState,
                            onWidgetActionConsumed = { widgetActionState = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        widgetActionState = intent.getStringExtra("widget_action")
    }
}

@Composable
private fun DriveDeckAuthenticatedApp(
    uid: String,
    widgetAction: String?,
    onWidgetActionConsumed: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val dbId = remember { appContext.getString(R.string.firestore_database_id) }

    val viewModel: GarageViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val db = FirebaseFirestore.getInstance(dbId)
                val repo = GarageRepository(db, Firebase.auth)
                GarageViewModel(
                    repository = repo,
                    currentUserId = uid,
                    syncWidget = { vehicles, reminders ->
                        WidgetSyncStore.syncFromCloud(appContext, vehicles, reminders)
                    }
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeTab by remember { mutableStateOf(DriveTab.DASHBOARD) }
    var fabExpanded by remember { mutableStateOf(false) }

    // Active Modal Dialog States
    var editingVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var showVehicleDialog by remember { mutableStateOf(false) }

    var editingRecord by remember { mutableStateOf<LogRecord?>(null) }
    var defaultRecordType by remember { mutableStateOf(RecordType.REFUEL) }
    var showRecordDialog by remember { mutableStateOf(false) }

    var editingReminder by remember { mutableStateOf<ReminderTask?>(null) }
    var showReminderDialog by remember { mutableStateOf(false) }

    var quickOdometerVehicle by remember { mutableStateOf<Vehicle?>(null) }

    LaunchedEffect(snackbarMsg) {
        val msg = snackbarMsg
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    // Handle widget shortcut actions ("add_task" or "open_tasks")
    LaunchedEffect(widgetAction, uiState) {
        if (widgetAction == null) return@LaunchedEffect
        val successData = (uiState as? UiState.Success)?.data ?: return@LaunchedEffect
        when (widgetAction) {
            "add_task" -> {
                activeTab = DriveTab.TASKS
                if (successData.vehicles.isNotEmpty()) {
                    editingReminder = null
                    showReminderDialog = true
                } else {
                    editingVehicle = null
                    showVehicleDialog = true
                    viewModel.showMessage("Сначала добавьте транспорт для задачи")
                }
                onWidgetActionConsumed()
            }
            "open_tasks" -> {
                activeTab = DriveTab.TASKS
                onWidgetActionConsumed()
            }
            else -> onWidgetActionConsumed()
        }
    }

    val openRecordEditor: (RecordType, LogRecord?, GarageDashboardData) -> Unit = { type, rec, data ->
        if (data.vehicles.isEmpty()) {
            editingVehicle = null
            showVehicleDialog = true
            viewModel.showMessage("Сначала добавьте авто или мотоцикл")
        } else {
            defaultRecordType = type
            editingRecord = rec
            showRecordDialog = true
        }
    }

    val openReminderEditor: (ReminderTask?, GarageDashboardData) -> Unit = { task, data ->
        if (data.vehicles.isEmpty()) {
            editingVehicle = null
            showVehicleDialog = true
            viewModel.showMessage("Сначала добавьте транспорт для задачи")
        } else {
            editingReminder = task
            showReminderDialog = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        DriveTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = activeTab == tab,
                                onClick = {
                                    activeTab = tab
                                    fabExpanded = false
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                val successData = (uiState as? UiState.Success)?.data
                if (successData != null && (activeTab == DriveTab.DASHBOARD || activeTab == DriveTab.TASKS)) {
                    DrivvoSpeedDialFab(
                        expanded = fabExpanded,
                        onToggle = { fabExpanded = !fabExpanded },
                        onRefuel = {
                            fabExpanded = false
                            openRecordEditor(RecordType.REFUEL, null, successData)
                        },
                        onService = {
                            fabExpanded = false
                            openRecordEditor(RecordType.SERVICE, null, successData)
                        },
                        onExpense = {
                            fabExpanded = false
                            openRecordEditor(RecordType.EXPENSE, null, successData)
                        },
                        onTask = {
                            fabExpanded = false
                            openReminderEditor(null, successData)
                        },
                        onVehicle = {
                            fabExpanded = false
                            editingVehicle = null
                            showVehicleDialog = true
                        }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(modifier = Modifier.weight(1f))
                        DriveTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = activeTab == tab,
                                onClick = {
                                    activeTab = tab
                                    fabExpanded = false
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (val state = uiState) {
                        UiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        "Синхронизация гаража...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        is UiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }

                        is UiState.Success -> {
                            val data = state.data
                            when (activeTab) {
                                DriveTab.DASHBOARD -> DashboardScreen(
                                    data = data,
                                    onSelectVehicle = { viewModel.selectVehicle(it) },
                                    onAddVehicle = {
                                        editingVehicle = null
                                        showVehicleDialog = true
                                    },
                                    onEditVehicle = { veh ->
                                        editingVehicle = veh
                                        showVehicleDialog = true
                                    },
                                    onQuickOdometer = { veh -> quickOdometerVehicle = veh },
                                    onEditRecord = { rec ->
                                        openRecordEditor(rec.safeRecordType(), rec, data)
                                    },
                                    onDeleteRecord = { id -> viewModel.deleteRecord(id) },
                                    onToggleTask = { task, done, odo ->
                                        viewModel.toggleReminder(task, done, odo)
                                    },
                                    onOpenTasksTab = { activeTab = DriveTab.TASKS },
                                    onQuickAddRecord = { rt -> openRecordEditor(rt, null, data) }
                                )

                                DriveTab.TASKS -> TasksScreen(
                                    data = data,
                                    onAddTask = { openReminderEditor(null, data) },
                                    onEditTask = { task -> openReminderEditor(task, data) },
                                    onToggleTask = { task, done, odo ->
                                        viewModel.toggleReminder(task, done, odo)
                                    },
                                    onDeleteTask = { id -> viewModel.deleteReminder(id) }
                                )

                                DriveTab.ANALYTICS -> AnalyticsScreen(data = data)

                                DriveTab.GARAGE_PC -> GarageAndPcScreen(
                                    data = data,
                                    onAddVehicle = {
                                        editingVehicle = null
                                        showVehicleDialog = true
                                    },
                                    onEditVehicle = { veh ->
                                        editingVehicle = veh
                                        showVehicleDialog = true
                                    },
                                    onDeleteVehicle = { id -> viewModel.deleteVehicle(id) },
                                    onUpdateVehicleStyle = { id, style, hex ->
                                        viewModel.updateVehicleStyle(id, style, hex)
                                    },
                                    onExportJson = { viewModel.exportBackupJson() },
                                    onExportCsv = { viewModel.exportCsv() },
                                    onImportJson = { raw -> viewModel.importBackupJson(raw) },
                                    onShowMessage = { msg -> viewModel.showMessage(msg) }
                                )
                            }

                            // Modal Editors
                            if (showVehicleDialog) {
                                VehicleEditorDialog(
                                    initialVehicle = editingVehicle,
                                    onDismiss = { showVehicleDialog = false },
                                    onSave = { veh ->
                                        viewModel.saveVehicle(veh) {
                                            showVehicleDialog = false
                                        }
                                    }
                                )
                            }

                            if (showRecordDialog) {
                                RecordEditorDialog(
                                    vehicles = data.vehicles,
                                    defaultVehicleId = data.selectedVehicle?.id ?: data.vehicles.firstOrNull()?.id.orEmpty(),
                                    initialRecord = editingRecord,
                                    defaultType = defaultRecordType,
                                    onDismiss = { showRecordDialog = false },
                                    onSave = { rec, currentOdo ->
                                        viewModel.saveRecord(rec, currentOdo) {
                                            showRecordDialog = false
                                        }
                                    }
                                )
                            }

                            if (showReminderDialog) {
                                ReminderEditorDialog(
                                    vehicles = data.vehicles,
                                    defaultVehicleId = data.selectedVehicle?.id ?: data.vehicles.firstOrNull()?.id.orEmpty(),
                                    initialTask = editingReminder,
                                    onDismiss = { showReminderDialog = false },
                                    onSave = { task ->
                                        viewModel.saveReminder(task) {
                                            showReminderDialog = false
                                        }
                                    }
                                )
                            }

                            quickOdometerVehicle?.let { veh ->
                                OdometerQuickDialog(
                                    vehicle = veh,
                                    onDismiss = { quickOdometerVehicle = null },
                                    onConfirm = { newOdo ->
                                        viewModel.updateVehicleOdometer(veh.id, newOdo)
                                        quickOdometerVehicle = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrivvoSpeedDialFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    onRefuel: () -> Unit,
    onService: () -> Unit,
    onExpense: () -> Unit,
    onTask: () -> Unit,
    onVehicle: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpeedDialItem("Заправка", Icons.Rounded.LocalGasStation, FuelAmber, onRefuel, "fab_refuel")
                SpeedDialItem("Сервис / ТО", Icons.Rounded.Build, ServiceEmerald, onService, "fab_service")
                SpeedDialItem("Расход", Icons.Rounded.ReceiptLong, ExpenseIndigo, onExpense, "fab_expense")
                SpeedDialItem("Задача ТО", Icons.Rounded.TaskAlt, CobaltPrimary, onTask, "fab_task")
                SpeedDialItem("Транспорт", Icons.Rounded.DirectionsCar, MaterialTheme.colorScheme.secondary, onVehicle, "fab_vehicle")
            }
        }

        FloatingActionButton(
            onClick = onToggle,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag("main_speed_dial_fab")
        ) {
            Icon(
                imageVector = if (expanded) Icons.Rounded.Close else Icons.Rounded.Add,
                contentDescription = if (expanded) "Закрыть меню" else "Быстрое добавление"
            )
        }
    }
}

@Composable
private fun SpeedDialItem(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 2.dp
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = tint,
            contentColor = Color.White,
            modifier = Modifier
                .size(48.dp)
                .testTag(tag)
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}
