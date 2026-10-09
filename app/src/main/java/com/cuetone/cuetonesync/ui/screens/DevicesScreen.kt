package com.cuetone.cuetonesync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuetone.cuetonesync.model.TimecodeDevice
import com.cuetone.cuetonesync.ui.components.*
import com.cuetone.cuetonesync.util.FRAME_DURATION_MS
import kotlinx.coroutines.delay
import com.cuetone.cuetonesync.data.DevicesRepository
import com.cuetone.cuetonesync.data.TimecodeRepository
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.cuetone.cuetonesync.ble.CueToneBleAdvertiser
import com.cuetone.cuetonesync.ble.CueToneBleScanner
import com.cuetone.cuetonesync.ble.CueToneTransmission
import com.cuetone.cuetonesync.ble.ClapperLogBeacon
import com.cuetone.cuetonesync.ble.PacketFactory
import com.cuetone.cuetonesync.data.PacketLogRepository
import com.cuetone.cuetonesync.model.DeviceRole
import com.cuetone.cuetonesync.model.DeviceChannel
import com.cuetone.cuetonesync.model.PacketLogEntry
import com.cuetone.cuetonesync.permissions.BlePermissionManager
import com.cuetone.cuetonesync.ui.theme.*
import com.cuetone.cuetonesync.util.splitFrames

@Composable
fun DevicesScreen(
    bleScanner: CueToneBleScanner,
    onDeviceClick: (Int) -> Unit,
    onPacketLogClick: () -> Unit,
    onProjectsClick: () -> Unit,
    onClapperBeacon: (ClapperLogBeacon, String) -> Unit
) {
    val devices = DevicesRepository.devices

    val context = LocalContext.current
    val bleAdvertiser = remember { CueToneBleAdvertiser(context) }
    var isScanning by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf("Нажмите «Искать снова»") }

    val startDeviceScan = { forceRestart: Boolean ->
        isScanning = true
        bleScanner.startScan(
            onDeviceFound = DevicesRepository::onDeviceDiscovered,
            onStatus = { status ->
                scanStatus = status
                isScanning = status == "Поиск устройств..." ||
                    status == "Слушаем эфир CUETONE" ||
                    status == "Перезапускаем BLE-сканирование..."
            },
            onClapperBeacon = beaconHandler@ { beacon ->
                if (!DevicesRepository.onClapperBeacon(beacon)) {
                    return@beaconHandler
                }

                val clapper = DevicesRepository.getDeviceById(beacon.deviceId)
                val timecode = if (clapper != null) {
                    val current = splitFrames(
                        TimecodeRepository.getDeviceFrame(clapper),
                        clapper.fps.frameRate
                    )
                    "%02d:%02d:%02d.%02d".format(
                        current.hours,
                        current.minutes,
                        beacon.timecodeSeconds,
                        beacon.timecodeFrames
                    )
                } else {
                    "00:00:%02d.%02d".format(
                        beacon.timecodeSeconds,
                        beacon.timecodeFrames
                    )
                }

                onClapperBeacon(beacon, timecode)
            },
            forceRestart = forceRestart
        )
    }

    val scanPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.values.all { it }) {
                startDeviceScan(false)
            } else {
                isScanning = false
                scanStatus = "Разрешение на поиск устройств отклонено"
            }
        }

    var pendingStatePackets by remember {
        mutableStateOf<List<Pair<TimecodeDevice, CueToneTransmission>>>(emptyList())
    }

    fun buildStatePacket(
        device: TimecodeDevice
    ): Pair<TimecodeDevice, CueToneTransmission> {
        val packet = PacketFactory.buildDeviceStatePacket(
            device = device,
            currentFrame = TimecodeRepository.getDeviceFrame(device)
        )

        return device to packet
    }

    fun sendAndLogStatePackets(
        items: List<Pair<TimecodeDevice, CueToneTransmission>>
    ) {
        if (items.isEmpty()) return

        val packets = items.map { it.second }

        bleAdvertiser.sendPacketsSequentially(packets) { index, packet, status ->
            val targetDevice = items[index].first

            val rawPacket = packet.toHexString()

            val packetName = if (targetDevice.type == "Clapper") {
                "Individual V2 / Clapper Master role"
            } else {
                "Packet #1 / Generator Master role"
            }

            val logEntry = PacketLogEntry(
                timestamp = System.currentTimeMillis(),
                direction = "OUT",
                deviceName = targetDevice.name,
                packetType = "$packetName / target 0x%04X / ${packet.logicalPayload.size} bytes / ${packet.fragmentCount} fragments".format(packet.target),
                status = status,
                rawData = rawPacket
            )

            PacketLogRepository.addLog(logEntry)
        }
    }

    var pendingSyncPackets by remember {
        mutableStateOf<List<Pair<DeviceChannel, CueToneTransmission>>>(emptyList())
    }

    fun sendAndLogSyncPackets(
        items: List<Pair<DeviceChannel, CueToneTransmission>>
    ) {
        if (items.isEmpty()) return

        bleAdvertiser.sendPacketsSequentially(items.map { it.second }) { index, packet, status ->
            val rawPacket = packet.toHexString()
            val channel = items[index].first

            val logEntry = PacketLogEntry(
                timestamp = System.currentTimeMillis(),
                direction = "OUT",
                deviceName = "Channel ${channel.name} Sync",
                packetType = "Packet #3 / Sync / target 0x%04X / ${packet.logicalPayload.size} bytes / ${packet.fragmentCount} fragments".format(packet.target),
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
                sendAndLogStatePackets(pendingStatePackets)
                pendingStatePackets = emptyList()

                sendAndLogSyncPackets(pendingSyncPackets)
                pendingSyncPackets = emptyList()
            }
        }

    var tick by remember { mutableIntStateOf(0) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showMasterDialog by remember { mutableStateOf(false) }
    var showManualSyncDialog by remember { mutableStateOf(false) }
    var selectedFilterChannel by remember { mutableStateOf<DeviceChannel?>(null) }
    val collapsedChannels = remember { mutableStateMapOf<DeviceChannel, Boolean>() }

    LaunchedEffect(Unit) {
        if (BlePermissionManager.hasBleScanPermission(context)) {
            startDeviceScan(false)
        }
        while (true) {
            delay(FRAME_DURATION_MS)
            tick++
        }
    }

    val hasDevices = devices.isNotEmpty()

    val availableChannels = devices.map { it.channel }.distinct().sortedBy { it.ordinal }
    val visibleChannels = selectedFilterChannel?.let { listOf(it) } ?: availableChannels

    LaunchedEffect(availableChannels) {
        if (selectedFilterChannel != null && selectedFilterChannel !in availableChannels) {
            selectedFilterChannel = null
        }
    }

    fun syncChannels(channels: List<DeviceChannel>) {
        if (channels.isEmpty()) return

        var updatedDevices = devices.toList()
        val packets = mutableListOf<Pair<DeviceChannel, CueToneTransmission>>()

        channels.forEach { channel ->
            val channelDevices = updatedDevices.filter { it.channel == channel }
            val master = channelDevices.firstOrNull { it.role == DeviceRole.MASTER }
                ?: channelDevices.firstOrNull()
                ?: return@forEach
            val targetFrame = TimecodeRepository.getDeviceFrame(master)
            val baseFrame = TimecodeRepository.getMasterFrame(master.fps.frameRate)

            updatedDevices = updatedDevices.map { device ->
                if (device.channel == channel) {
                    device.copy(
                        frameOffset = targetFrame - baseFrame,
                        fps = master.fps,
                        role = if (device.id == master.id) DeviceRole.MASTER else DeviceRole.SLAVE
                    )
                } else {
                    device
                }
            }

            val syncedChannelDevices = updatedDevices.filter { it.channel == channel }
            val syncedMaster = syncedChannelDevices.first { it.id == master.id }
            packets += channel to PacketFactory.buildSyncPacket(
                devicesInChannel = syncedChannelDevices,
                masterDevice = syncedMaster,
                targetFrame = targetFrame,
                fps = syncedMaster.fps,
                userBit = syncedMaster.userBit
            )
        }

        devices.clear()
        devices.addAll(updatedDevices)

        if (BlePermissionManager.hasBleAdvertisePermission(context)) {
            sendAndLogSyncPackets(packets)
        } else {
            pendingSyncPackets = packets
            permissionLauncher.launch(BlePermissionManager.requiredPermissions())
        }
    }

    val availableToAdd = DevicesRepository.discoveredDevices.filter { availableDevice ->
        devices.none { addedDevice -> addedDevice.id == availableDevice.id }
    }

    fun deleteDevice(device: TimecodeDevice) {
        val deletedWasMaster = device.role == DeviceRole.MASTER
        devices.remove(device)

        if (deletedWasMaster) {
            val newMaster = devices.firstOrNull { it.channel == device.channel } ?: return
            devices.replaceAll { current ->
                if (current.channel == device.channel) {
                    current.copy(
                        role = if (current.id == newMaster.id) DeviceRole.MASTER else DeviceRole.SLAVE
                    )
                } else {
                    current
                }
            }
        }
    }

    fun makeMaster(device: TimecodeDevice) {
        val oldMaster = devices.firstOrNull {
            it.channel == device.channel && it.role == DeviceRole.MASTER && it.id != device.id
        }
        val updatedDevices = devices.map { currentDevice ->
            if (currentDevice.channel == device.channel) {
                currentDevice.copy(
                    role = if (currentDevice.id == device.id) DeviceRole.MASTER else DeviceRole.SLAVE
                )
            } else {
                currentDevice
            }
        }

        devices.clear()
        devices.addAll(updatedDevices)

        val packetsToSend = listOfNotNull(
            updatedDevices.firstOrNull { it.id == device.id }?.let(::buildStatePacket),
            oldMaster?.let { previous ->
                updatedDevices.firstOrNull { it.id == previous.id }?.let(::buildStatePacket)
            }
        )

        if (packetsToSend.isNotEmpty()) {
            if (BlePermissionManager.hasBleAdvertisePermission(context)) {
                sendAndLogStatePackets(packetsToSend)
            } else {
                pendingStatePackets = packetsToSend
                permissionLauncher.launch(BlePermissionManager.requiredPermissions())
            }
        }
    }

    fun openAddDeviceDialog() {
        showAddDialog = true
        if (BlePermissionManager.hasBleScanPermission(context)) {
            startDeviceScan(false)
        } else {
            scanStatus = "Нужно разрешение на поиск BLE-устройств"
            scanPermissionLauncher.launch(BlePermissionManager.requiredScanPermissions())
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 20.dp)
            ) {
                if (hasDevices) {
                    DevicesToolbar(
                        selectedChannel = selectedFilterChannel,
                        availableChannels = availableChannels,
                        onChannelSelected = { selectedFilterChannel = it },
                        onSyncAllClick = { showMasterDialog = true },
                        onManualSyncClick = { showManualSyncDialog = true },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 112.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(visibleChannels, key = { it.name }) { channel ->
                        val channelDevices = devices.filter { it.channel == channel }
                        ChannelSection(
                            channel = channel,
                            devices = channelDevices,
                            expanded = collapsedChannels[channel] != true,
                            onToggle = {
                                collapsedChannels[channel] = collapsedChannels[channel] != true
                            },
                            onSync = { syncChannels(listOf(channel)) },
                            deviceContent = { device ->
                                DeviceCard(
                                    device = device,
                                    currentFrame = tick.let {
                                        TimecodeRepository.getDeviceFrame(device)
                                    },
                                    isOnline = DevicesRepository.isDeviceOnline(device),
                                    onClick = { onDeviceClick(device.id) },
                                    onDeleteClick = { deleteDevice(device) },
                                    onMakeMasterClick = { makeMaster(device) }
                                )
                            }
                        )
                    }
                }
            }

            BottomLogBar(
                onPacketLogClick = onPacketLogClick,
                onAddDeviceClick = ::openAddDeviceDialog,
                onProjectsClick = onProjectsClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    }

    if (showAddDialog) {
        AddDeviceDialog(
            availableDevices = availableToAdd,
            isScanning = isScanning,
            scanStatus = scanStatus,
            onDismiss = {
                showAddDialog = false
            },
            onScanAgain = {
                if (BlePermissionManager.hasBleScanPermission(context)) {
                    startDeviceScan(true)
                } else {
                    scanPermissionLauncher.launch(
                        BlePermissionManager.requiredScanPermissions()
                    )
                }
            },
            onDeviceSelected = { selected ->
                DevicesRepository.addDiscoveredDevice(selected)
                showAddDialog = false
            }
        )
    }

    if (showMasterDialog) {
        MasterSelectDialog(
            devices = devices,
            onDismiss = { showMasterDialog = false },
            onMasterSelected = { selectedChannel, selectedMaster ->
                val masterCurrentFrame = TimecodeRepository.getDeviceFrame(selectedMaster)
                val baseFrame = TimecodeRepository.getMasterFrame(selectedMaster.fps.frameRate)
                val updatedDevices = devices.map { device ->
                    device.copy(
                        channel = selectedChannel,
                        frameOffset = masterCurrentFrame - baseFrame,
                        fps = selectedMaster.fps,
                        role = if (device.id == selectedMaster.id) {
                            DeviceRole.MASTER
                        } else {
                            DeviceRole.SLAVE
                        }
                    )
                }

                devices.clear()
                devices.addAll(updatedDevices)

                val updatedMaster = updatedDevices.first {
                    it.id == selectedMaster.id
                }
                val packet = PacketFactory.buildSyncPacket(
                    devicesInChannel = updatedDevices,
                    masterDevice = updatedMaster,
                    targetFrame = masterCurrentFrame,
                    fps = updatedMaster.fps,
                    userBit = updatedMaster.userBit,
                    broadcast = true
                )

                if (BlePermissionManager.hasBleAdvertisePermission(context)) {
                    sendAndLogSyncPackets(listOf(selectedChannel to packet))
                } else {
                    pendingSyncPackets = listOf(selectedChannel to packet)
                    permissionLauncher.launch(BlePermissionManager.requiredPermissions())
                }

                showMasterDialog = false
            }
        )
    }

    if (showManualSyncDialog) {
        ManualSyncDialog(
            devices = devices,
            onDismiss = {
                showManualSyncDialog = false
            },
            onSync = { targetFrame, selectedFps, selectedChannel, selectedMaster, userBit ->
                val baseFrame = TimecodeRepository.getMasterFrame(selectedFps.frameRate)

                val updatedDevices = devices.map { device ->
                    if (device.channel == selectedChannel) {
                        device.copy(
                            frameOffset = targetFrame - baseFrame,
                            fps = selectedFps,
                            userBit = userBit,
                            role =
                                if (device.id == selectedMaster.id) {
                                    DeviceRole.MASTER
                                } else {
                                    DeviceRole.SLAVE
                                }
                        )
                    } else {
                        device
                    }
                }

                devices.clear()
                devices.addAll(updatedDevices)

                val updatedChannelDevices =
                    updatedDevices.filter { it.channel == selectedChannel }

                val updatedMaster =
                    updatedChannelDevices.first { it.id == selectedMaster.id }

                val packet = PacketFactory.buildSyncPacket(
                    devicesInChannel = updatedChannelDevices,
                    masterDevice = updatedMaster,
                    targetFrame = targetFrame,
                    fps = selectedFps,
                    userBit = userBit
                )

                if (BlePermissionManager.hasBleAdvertisePermission(context)) {
                    sendAndLogSyncPackets(listOf(selectedChannel to packet))
                } else {
                    pendingSyncPackets = listOf(selectedChannel to packet)
                    permissionLauncher.launch(
                        BlePermissionManager.requiredPermissions()
                    )
                }

                showManualSyncDialog = false
            }
        )
    }
}

@Composable
private fun ChannelSection(
    channel: DeviceChannel,
    devices: List<TimecodeDevice>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSync: () -> Unit,
    deviceContent: @Composable (TimecodeDevice) -> Unit
) {
    val channelFps = devices.firstOrNull()?.fps?.title ?: "--"

    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CH ${channel.name}",
                    color = Primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = channelFps,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    onClick = onSync,
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = ChannelSync
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sync",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Свернуть канал" else "Развернуть канал",
                        tint = TextSecondary
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    devices.chunked(2).forEach { rowDevices ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            rowDevices.forEach { device ->
                                Box(modifier = Modifier.weight(1f)) {
                                    deviceContent(device)
                                }
                            }

                            if (rowDevices.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomLogBar(
    onPacketLogClick: () -> Unit,
    onAddDeviceClick: () -> Unit,
    onProjectsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomActionTile(
            label = "Журнал BLE",
            onClick = onPacketLogClick
        ) {
            Icon(
                imageVector = Icons.Outlined.ReceiptLong,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(25.dp)
            )
        }

        BottomActionTile(
            label = "Добавить устройство",
            onClick = onAddDeviceClick
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        BottomActionTile(
            label = "Проекты",
            onClick = onProjectsClick
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(25.dp)
            )
        }
    }
}

@Composable
private fun RowScope.BottomActionTile(
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(78.dp),
        shape = RoundedCornerShape(12.dp),
        color = PrimarySoft.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.height(34.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Text(
                text = label,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
