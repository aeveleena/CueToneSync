package com.cuetone.cuetonesync.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuetone.cuetonesync.R
import com.cuetone.cuetonesync.ble.CueToneBleAdvertiser
import com.cuetone.cuetonesync.ble.CueToneClapperLogClient
import com.cuetone.cuetonesync.ble.CueToneTransmission
import com.cuetone.cuetonesync.ble.ClapperLogRecord
import com.cuetone.cuetonesync.ble.ClapperLogExportResult
import com.cuetone.cuetonesync.ble.PacketFactory
import com.cuetone.cuetonesync.data.DevicesRepository
import com.cuetone.cuetonesync.data.PacketLogRepository
import com.cuetone.cuetonesync.data.TimecodeRepository
import com.cuetone.cuetonesync.data.local.CueToneDatabase
import com.cuetone.cuetonesync.data.local.entity.LogTableEntity
import com.cuetone.cuetonesync.data.local.entity.ProjectEntity
import com.cuetone.cuetonesync.data.repository.ClapperLogImportRepository
import com.cuetone.cuetonesync.model.PacketLogEntry
import com.cuetone.cuetonesync.permissions.BlePermissionManager
import com.cuetone.cuetonesync.util.FRAME_DURATION_MS
import com.cuetone.cuetonesync.util.formatTimecode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.cuetone.cuetonesync.ui.theme.*
import com.cuetone.cuetonesync.ui.components.BackButton
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ClapperDetailsScreen(
    deviceId: Int,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSlateTableClick: () -> Unit,
    onImportedTableClick: (Long) -> Unit
) {
    val device = DevicesRepository.getDeviceById(deviceId) ?: return

    val context = androidx.compose.ui.platform.LocalContext.current
    val bleAdvertiser = remember { CueToneBleAdvertiser(context) }
    val logClient = remember { CueToneClapperLogClient(context) }
    val database = remember { CueToneDatabase.getDatabase(context) }
    val importRepository = remember { ClapperLogImportRepository(database) }
    val scope = rememberCoroutineScope()
    val projects by importRepository.observeProjects().collectAsState(initial = emptyList())
    val tables by importRepository.observeTables().collectAsState(initial = emptyList())

    var bleStatus by remember { mutableStateOf("") }
    var pendingPacket by remember { mutableStateOf<CueToneTransmission?>(null) }
    var pendingLogExport by remember { mutableStateOf(false) }
    var isExportingLog by remember { mutableStateOf(false) }
    var pendingLogRecords by remember { mutableStateOf<List<ClapperLogRecord>?>(null) }
    var activeImportCamera by remember { mutableStateOf<Int?>(null) }
    var showImportDestination by remember { mutableStateOf(false) }
    var showImportModeSheet by remember { mutableStateOf(false) }
    var savedDestinations by remember {
        mutableStateOf<Map<Int, LogTableEntity>>(emptyMap())
    }
    var isImportingLog by remember { mutableStateOf(false) }

    fun beginLogImport(records: List<ClapperLogRecord>) {
        pendingLogRecords = records
        activeImportCamera = records.minOfOrNull { it.cameraIndex } ?: 0
        showImportDestination = false
        showImportModeSheet = false
        savedDestinations = emptyMap()
        if (records.isNotEmpty()) {
            scope.launch {
                savedDestinations = importRepository.getCameraDestinations(
                    deviceId = device.id,
                    cameraIndices = records.map { it.cameraIndex }
                )
                showImportModeSheet = true
            }
        }
    }

    fun importLogIntoSavedDestinations() {
        val records = pendingLogRecords.orEmpty()
        val recordsByCamera = records.groupBy { it.cameraIndex }.toSortedMap()
        if (recordsByCamera.isEmpty()) return
        if (recordsByCamera.keys.any { it !in savedDestinations }) {
            showImportModeSheet = false
            showImportDestination = true
            return
        }

        showImportModeSheet = false
        isImportingLog = true
        scope.launch {
            try {
                var imported = 0
                recordsByCamera.forEach { (cameraIndex, cameraRecords) ->
                    val table = savedDestinations.getValue(cameraIndex)
                    imported += importRepository.importRecords(
                        tableId = table.id,
                        deviceId = device.id,
                        clapperSerialNumber = device.serialNumber,
                        records = cameraRecords
                    ).imported
                }
                val firstTableId = savedDestinations.getValue(recordsByCamera.keys.first()).id
                pendingLogRecords = null
                activeImportCamera = null
                bleStatus = if (imported == 0) {
                    "Новых записей нет"
                } else {
                    "Сохранено $imported новых записей"
                }
                onImportedTableClick(firstTableId)
            } catch (exception: Exception) {
                bleStatus = exception.message ?: "Не удалось сохранить журнал"
            } finally {
                isImportingLog = false
            }
        }
    }

    fun finishCameraImport(
        cameraIndex: Int,
        tableId: Long,
        message: String
    ) {
        val remaining = pendingLogRecords.orEmpty()
            .filterNot { it.cameraIndex == cameraIndex }
        bleStatus = message

        if (remaining.isEmpty()) {
            pendingLogRecords = null
            activeImportCamera = null
            showImportDestination = false
            onImportedTableClick(tableId)
        } else {
            pendingLogRecords = remaining
            activeImportCamera = remaining.minOfOrNull { it.cameraIndex }
            showImportDestination = true
        }
    }

    fun importLogInto(tableId: Long) {
        val cameraIndex = activeImportCamera ?: return
        val records = pendingLogRecords.orEmpty().filter { it.cameraIndex == cameraIndex }
        if (records.isEmpty()) return
        isImportingLog = true
        scope.launch {
            try {
                val result = importRepository.importRecords(
                    tableId = tableId,
                    deviceId = device.id,
                    clapperSerialNumber = device.serialNumber,
                    records = records
                )
                val message = if (result.imported == 0) {
                    "Камера ${cameraLetter(cameraIndex)}: новых записей нет"
                } else {
                    "Камера ${cameraLetter(cameraIndex)}: сохранено ${result.imported} записей"
                }
                finishCameraImport(cameraIndex, tableId, message)
            } catch (exception: Exception) {
                bleStatus = exception.message ?: "Не удалось сохранить журнал"
            } finally {
                isImportingLog = false
            }
        }
    }

    fun createTableAndImport(projectId: Long, name: String) {
        val cameraIndex = activeImportCamera ?: return
        val records = pendingLogRecords.orEmpty().filter { it.cameraIndex == cameraIndex }
        if (records.isEmpty()) return
        isImportingLog = true
        scope.launch {
            try {
                val tableId = importRepository.createTable(
                    projectId = projectId,
                    name = name,
                    clapperSerialNumber = device.serialNumber
                )
                val result = importRepository.importRecords(
                    tableId = tableId,
                    deviceId = device.id,
                    clapperSerialNumber = device.serialNumber,
                    records = records
                )
                finishCameraImport(
                    cameraIndex = cameraIndex,
                    tableId = tableId,
                    message = "Камера ${cameraLetter(cameraIndex)}: создана таблица «$name», " +
                        "сохранено ${result.imported} записей"
                )
            } catch (exception: Exception) {
                bleStatus = exception.message ?: "Не удалось создать таблицу"
            } finally {
                isImportingLog = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            bleAdvertiser.stopAdvertising()
            logClient.shutdown()
        }
    }

    fun sendAndLogPacket(
        packet: CueToneTransmission,
        actionName: String
    ) {
        bleAdvertiser.sendPacket(packet) { status ->
            val rawPacket = packet.toHexString()

            bleStatus = status

            val logEntry = PacketLogEntry(
                timestamp = System.currentTimeMillis(),
                direction = "OUT",
                deviceName = device.name,
                packetType = "Individual V2 / $actionName / ${packet.logicalPayload.size} bytes / ${packet.fragmentCount} fragments",
                status = status,
                rawData = rawPacket
            )

            PacketLogRepository.addLog(logEntry)
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions.values.all { it }

            if (granted) {
                if (pendingLogExport) {
                    pendingLogExport = false
                    startClapperLogExport(
                        deviceId = device.id,
                        deviceName = device.name,
                        advertiser = bleAdvertiser,
                        logClient = logClient,
                        onStatus = { bleStatus = it },
                        onExportingChanged = { isExportingLog = it },
                        onRecordsReady = ::beginLogImport
                    )
                } else {
                    pendingPacket?.let { packet ->
                        sendAndLogPacket(
                            packet = packet,
                            actionName = "Clapper State"
                        )
                    }
                }
                pendingPacket = null
            } else {
                pendingLogExport = false
                isExportingLog = false
                bleStatus = "BLE-разрешение отклонено"
            }
        }

    fun sendClapperState(
        packet: CueToneTransmission,
        actionName: String
    ) {
        if (BlePermissionManager.hasBleAdvertisePermission(context)) {
            sendAndLogPacket(
                packet = packet,
                actionName = actionName
            )
        } else {
            pendingPacket = packet
            permissionLauncher.launch(
                BlePermissionManager.requiredPermissions()
            )
        }
    }

    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(FRAME_DURATION_MS)
            tick++
        }
    }

    val currentFrame = tick.let {
        TimecodeRepository.getDeviceFrame(device)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BackButton(
                        onClick = onBackClick,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )

                    Text(
                        text = device.name,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Top
                ) {
                    IconButton(
                        modifier = Modifier
                            .size(32.dp)
                            .offset(y = (-6).dp),
                        onClick = onSettingsClick
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = "Настройки",
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DeviceInfoCard("Battery", "${device.batteryLevel}%")
                DeviceInfoCard("FPS", device.fps.title)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DeviceInfoCard("User Bit", device.userBit)
                DeviceInfoCard("Connection", "BLE")
            }

            Spacer(modifier = Modifier.height(90.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatTimecode(currentFrame, device.fps.frameRate),
                    color = TextPrimary,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(110.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val packet = PacketFactory.buildDeviceStatePacket(
                            device = device,
                            currentFrame = TimecodeRepository.getDeviceFrame(device)
                        )

                        sendClapperState(
                            packet = packet,
                            actionName = "Sync"
                        )
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Sync")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val updatedDevice = device.copy(
                            frameOffset = TimecodeRepository.offsetForTargetFrame(
                                targetFrame = 0,
                                frameRate = device.fps.frameRate
                            )
                        )

                        DevicesRepository.updateDevice(updatedDevice)

                        val packet = PacketFactory.buildDeviceStatePacket(
                            device = updatedDevice,
                            currentFrame = TimecodeRepository.getDeviceFrame(updatedDevice)
                        )

                        sendClapperState(
                            packet = packet,
                            actionName = "Reset TC"
                        )
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceElevated,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Reset TC")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSlateTableClick,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Открыть таблицу дублей")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExportingLog && pendingLogRecords == null,
                onClick = {
                    val hasPermissions =
                        BlePermissionManager.hasBleAdvertisePermission(context) &&
                            BlePermissionManager.hasBleScanPermission(context)
                    if (hasPermissions) {
                        startClapperLogExport(
                            deviceId = device.id,
                            deviceName = device.name,
                            advertiser = bleAdvertiser,
                            logClient = logClient,
                            onStatus = { bleStatus = it },
                            onExportingChanged = { isExportingLog = it },
                            onRecordsReady = ::beginLogImport
                        )
                    } else {
                        pendingLogExport = true
                        permissionLauncher.launch(
                            (
                                BlePermissionManager.requiredPermissions() +
                                    BlePermissionManager.requiredScanPermissions()
                                ).distinct().toTypedArray()
                        )
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceElevated,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    when {
                        isExportingLog -> "Получаем журнал..."
                        pendingLogRecords != null -> "Сначала сохраните полученный журнал"
                        else -> "Считать журнал хлопушки"
                    }
                )
            }

            pendingLogRecords?.let { records ->
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isImportingLog,
                    onClick = { showImportModeSheet = true },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Сохранить полученный журнал (${records.size})")
                }
            }

            if (bleStatus.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = bleStatus,
                    color = TextSecondary
                )
            }
        }
    }

    if (showImportDestination && pendingLogRecords != null && activeImportCamera != null) {
        val cameraIndex = activeImportCamera ?: 0
        val cameraRecords = pendingLogRecords.orEmpty().filter { it.cameraIndex == cameraIndex }
        ClapperLogDestinationDialog(
            cameraIndex = cameraIndex,
            recordCount = cameraRecords.size,
            projects = projects,
            tables = tables,
            defaultTableName = "${device.name} Камера ${cameraLetter(cameraIndex)} " +
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            isSaving = isImportingLog,
            onDismiss = { showImportDestination = false },
            onTableSelected = ::importLogInto,
            onCreateTable = ::createTableAndImport
        )
    }

    if (showImportModeSheet && pendingLogRecords != null) {
        val cameraIndices = pendingLogRecords.orEmpty()
            .map { it.cameraIndex }
            .distinct()
            .sorted()
        ClapperLogImportModeSheet(
            cameraIndices = cameraIndices,
            destinations = savedDestinations,
            isSaving = isImportingLog,
            onDismiss = { showImportModeSheet = false },
            onUseSavedDestinations = ::importLogIntoSavedDestinations,
            onChooseDestinations = {
                showImportModeSheet = false
                activeImportCamera = cameraIndices.firstOrNull()
                showImportDestination = activeImportCamera != null
            }
        )
    }
}

private fun startClapperLogExport(
    deviceId: Int,
    deviceName: String,
    advertiser: CueToneBleAdvertiser,
    logClient: CueToneClapperLogClient,
    onStatus: (String) -> Unit,
    onExportingChanged: (Boolean) -> Unit,
    onRecordsReady: (List<ClapperLogRecord>) -> Unit
) {
    onExportingChanged(true)
    onStatus("Подтвердите EXPORT на хлопушке")
    val request = PacketFactory.buildClapperLogRequest(deviceId)
    advertiser.sendPacket(request, durationMs = 10_000L) { requestStatus ->
        PacketLogRepository.addLog(
            PacketLogEntry(
                timestamp = System.currentTimeMillis(),
                direction = "OUT",
                deviceName = deviceName,
                packetType = "Clapper log request 0xC3",
                status = requestStatus,
                rawData = request.toHexString()
            )
        )
        if (!requestStatus.startsWith("Legacy BLE: отправлено")) {
            onExportingChanged(false)
            onStatus(requestStatus)
            return@sendPacket
        }

        logClient.start(
            deviceId = deviceId,
            onStatus = onStatus,
            onResult = { result ->
                onExportingChanged(false)
                when (result) {
                    is ClapperLogExportResult.Success -> {
                        val status = "Журнал получен: ${result.records.size} записей"
                        onStatus(status)
                        onRecordsReady(result.records)
                        PacketLogRepository.addLog(
                            PacketLogEntry(
                                timestamp = System.currentTimeMillis(),
                                direction = "IN",
                                deviceName = deviceName,
                                packetType = "Clapper LOG1 export",
                                status = status,
                                rawData = result.records.joinToString(" | ") { record ->
                                    "seq=${record.sequence},tc=%02d:%02d:%02d:%02d".format(
                                        record.timecodeHours,
                                        record.timecodeMinutes,
                                        record.timecodeSeconds,
                                        record.timecodeFrames
                                    )
                                }
                            )
                        )
                    }
                    is ClapperLogExportResult.Failure -> onStatus(result.message)
                }
            }
        )
    }
}

@Composable
private fun ClapperLogDestinationDialog(
    cameraIndex: Int,
    recordCount: Int,
    projects: List<ProjectEntity>,
    tables: List<LogTableEntity>,
    defaultTableName: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onTableSelected: (Long) -> Unit,
    onCreateTable: (Long, String) -> Unit
) {
    var createMode by remember { mutableStateOf(false) }
    var tableName by remember(defaultTableName) { mutableStateOf(defaultTableName) }
    var selectedProjectId by remember(projects) {
        mutableStateOf(projects.firstOrNull()?.id)
    }
    var projectMenuOpen by remember { mutableStateOf(false) }
    val projectNames = remember(projects) { projects.associate { it.id to it.name } }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                if (createMode) {
                    "Новая таблица · Камера ${cameraLetter(cameraIndex)}"
                } else {
                    "Куда сохранять камеру ${cameraLetter(cameraIndex)}?"
                }
            )
        },
        text = {
            if (createMode) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Записей камеры: $recordCount", color = TextSecondary)

                    Box {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSaving && projects.isNotEmpty(),
                            onClick = { projectMenuOpen = true }
                        ) {
                            Text(
                                selectedProjectId?.let { projectNames[it] } ?: "Выберите проект"
                            )
                        }
                        DropdownMenu(
                            expanded = projectMenuOpen,
                            onDismissRequest = { projectMenuOpen = false }
                        ) {
                            projects.forEach { project ->
                                DropdownMenuItem(
                                    text = { Text(project.name) },
                                    onClick = {
                                        selectedProjectId = project.id
                                        projectMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = tableName,
                        enabled = !isSaving,
                        onValueChange = { tableName = it },
                        singleLine = true,
                        label = { Text("Название таблицы") }
                    )

                    if (projects.isEmpty()) {
                        Text(
                            "Сначала создайте проект на экране «Проекты»",
                            color = TextSecondary
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Получено записей камеры: $recordCount", color = TextSecondary)

                    if (tables.isEmpty()) {
                        Text("Таблиц дублей пока нет", color = TextSecondary)
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(tables, key = { it.id }) { table ->
                                ListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isSaving) {
                                            onTableSelected(table.id)
                                        },
                                    headlineContent = { Text(table.name) },
                                    supportingContent = {
                                        val projectName = projectNames[table.projectId] ?: "Проект"
                                        val clapperName = table.clapperSerialNumber
                                            ?.takeIf { it.isNotBlank() }
                                            ?: "без привязанной хлопушки"
                                        Text("$projectName · $clapperName")
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = SurfaceElevated
                                    )
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving,
                        onClick = { createMode = true }
                    ) {
                        Text("Создать новую таблицу")
                    }
                }
            }
        },
        confirmButton = {
            if (createMode) {
                TextButton(
                    enabled = !isSaving && tableName.isNotBlank() && selectedProjectId != null,
                    onClick = {
                        selectedProjectId?.let { projectId ->
                            onCreateTable(projectId, tableName.trim())
                        }
                    }
                ) {
                    Text(if (isSaving) "Сохраняем..." else "Создать и сохранить")
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    if (createMode) createMode = false else onDismiss()
                }
            ) {
                Text(if (createMode) "Назад" else "Позже")
            }
        }
    )
}

private fun cameraLetter(cameraIndex: Int): Char = 'A' + cameraIndex.coerceIn(0, 2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClapperLogImportModeSheet(
    cameraIndices: List<Int>,
    destinations: Map<Int, LogTableEntity>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onUseSavedDestinations: () -> Unit,
    onChooseDestinations: () -> Unit
) {
    val hasAllDestinations = cameraIndices.isNotEmpty() &&
        cameraIndices.all { it in destinations }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Куда сохранить журнал?",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (destinations.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    cameraIndices.forEach { cameraIndex ->
                        Text(
                            text = destinations[cameraIndex]?.let { table ->
                                "Камера ${cameraLetter(cameraIndex)} → ${table.name}"
                            } ?: "Камера ${cameraLetter(cameraIndex)} → таблица не выбрана",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Text(
                    text = "Для этих камер таблицы ещё не выбраны",
                    color = TextSecondary
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = hasAllDestinations && !isSaving,
                onClick = onUseSavedDestinations,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Сохранить туда же")
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                onClick = onChooseDestinations,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceElevated,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Выбрать другие таблицы")
            }

            TextButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                onClick = onDismiss
            ) {
                Text("Позже")
            }
        }
    }
}
