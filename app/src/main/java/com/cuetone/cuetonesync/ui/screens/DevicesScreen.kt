package com.cuetone.cuetonesync.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuetone.cuetonesync.data.MockDevices
import com.cuetone.cuetonesync.model.TimecodeDevice
import com.cuetone.cuetonesync.ui.components.*
import com.cuetone.cuetonesync.util.FRAME_DURATION_MS
import com.cuetone.cuetonesync.util.formatTimecode
import kotlinx.coroutines.delay
import com.cuetone.cuetonesync.data.DevicesRepository
import com.cuetone.cuetonesync.data.TimecodeRepository
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.cuetone.cuetonesync.ble.CueToneBleAdvertiser
import com.cuetone.cuetonesync.ble.PacketFactory
import com.cuetone.cuetonesync.data.PacketLogRepository
import com.cuetone.cuetonesync.model.DeviceRole
import com.cuetone.cuetonesync.model.PacketLogEntry
import com.cuetone.cuetonesync.permissions.BlePermissionManager

@Composable
fun DevicesScreen(
    onDeviceClick: (Int) -> Unit,
    onPacketLogClick: () -> Unit
) {
    val devices = DevicesRepository.devices

    val context = LocalContext.current
    val bleAdvertiser = remember { CueToneBleAdvertiser(context) }

    var pendingStatePackets by remember {
        mutableStateOf<List<Pair<TimecodeDevice, ByteArray>>>(emptyList())
    }

    fun buildStatePacket(
        device: TimecodeDevice
    ): Pair<TimecodeDevice, ByteArray> {
        val packet = PacketFactory.buildDeviceStatePacket(
            device = device,
            currentFrame = TimecodeRepository.getDeviceFrame(device)
        )

        return device to packet
    }

    fun sendAndLogStatePackets(
        items: List<Pair<TimecodeDevice, ByteArray>>
    ) {
        if (items.isEmpty()) return

        val packets = items.map { it.second }

        bleAdvertiser.sendPacketsSequentially(packets) { index, packet, status ->
            val targetDevice = items[index].first

            val rawPacket = packet.joinToString(" ") {
                "%02X".format(it)
            }

            val packetName = if (targetDevice.type == "Clapper") {
                "Packet #2 / Clapper Master role"
            } else {
                "Packet #1 / Generator Master role"
            }

            val logEntry = PacketLogEntry(
                timestamp = System.currentTimeMillis(),
                direction = "OUT",
                deviceName = targetDevice.name,
                packetType = "$packetName / ${packet.size} bytes",
                status = "$status · ожидает подтверждения",
                rawData = rawPacket
            )

            PacketLogRepository.addLog(logEntry)

            PacketLogRepository.simulateConfirmation(
                logId = logEntry.id,
                status = status
            )
        }
    }

    var pendingSyncPacket by remember {
        mutableStateOf<ByteArray?>(null)
    }

    fun sendAndLogSyncPacket(
        packet: ByteArray
    ) {
        bleAdvertiser.sendPacket(packet) { status ->
            val rawPacket = packet.joinToString(" ") {
                "%02X".format(it)
            }

            val logEntry = PacketLogEntry(
                timestamp = System.currentTimeMillis(),
                direction = "OUT",
                deviceName = "Channel Sync",
                packetType = "Packet #3 / Sync / ${packet.size} bytes",
                status = "$status · ожидает подтверждения",
                rawData = rawPacket
            )

            PacketLogRepository.addLog(logEntry)

            PacketLogRepository.simulateConfirmation(
                logId = logEntry.id,
                status = status
            )
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

                pendingSyncPacket?.let { packet ->
                    sendAndLogSyncPacket(packet)
                }
                pendingSyncPacket = null
            }
        }

    var tick by remember { mutableIntStateOf(0) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showMasterDialog by remember { mutableStateOf(false) }
    var showManualSyncDialog by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<TimecodeDevice?>(null) }

    var selectedHeaderChannel by remember {
        mutableStateOf(devices.firstOrNull()?.channel)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(FRAME_DURATION_MS)
            tick++
        }
    }

    val hasDevices = devices.isNotEmpty()

    LaunchedEffect(devices.size) {
        if (selectedHeaderChannel == null && devices.isNotEmpty()) {
            selectedHeaderChannel = devices.first().channel
        }

        if (
            selectedHeaderChannel != null &&
            devices.none { it.channel == selectedHeaderChannel }
        ) {
            selectedHeaderChannel = devices.firstOrNull()?.channel
        }
    }

    val selectedChannelDevices = devices.filter {
        it.channel == selectedHeaderChannel
    }

    val headerDevice =
        selectedChannelDevices.firstOrNull { it.role == DeviceRole.MASTER }
            ?: selectedChannelDevices.firstOrNull()
            ?: devices.firstOrNull()

    val headerFrame = tick.let {
        headerDevice?.let { device ->
            TimecodeRepository.getDeviceFrame(device)
        } ?: 0
    }

    val headerTimecode = formatTimecode(
        headerFrame,
        headerDevice?.fps?.frameRate ?: 25
    )

    val masterFrame = TimecodeRepository.getMasterFrame()

    val headerFps = headerDevice?.fps?.title

    val availableToAdd = MockDevices.availableDevices.filter { availableDevice ->
        devices.none { addedDevice -> addedDevice.id == availableDevice.id }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF3C3F41)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            if (hasDevices) {
                TimecodeHeader(
                    mainTimecode = headerTimecode,
                    fps = headerFps,
                    selectedChannel = selectedHeaderChannel,
                    availableChannels = devices.map { it.channel }.distinct(),
                    onChannelSelected = { channel ->
                        selectedHeaderChannel = channel
                    },
                    onSyncAllClick = {
                        showMasterDialog = true
                    },
                    onManualSyncClick = {
                        showManualSyncDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = "All Devices",
                color = Color(0xFF6F95B9),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(devices) { device ->
                    DeviceCard(
                        device = device,
                        currentFrame = tick.let {
                            TimecodeRepository.getDeviceFrame(device)
                        },
                        onClick = {
                            onDeviceClick(device.id)
                        },
                        onRenameClick = {
                            selectedDevice = device
                        },
                        onDeleteClick = {
                            val deletedWasMaster = device.role == com.cuetone.cuetonesync.model.DeviceRole.MASTER

                            devices.remove(device)

                            if (deletedWasMaster && devices.isNotEmpty()) {
                                val newMaster = devices.first()

                                devices.replaceAll {
                                    if (it.id == newMaster.id) {
                                        it.copy(role = com.cuetone.cuetonesync.model.DeviceRole.MASTER)
                                    } else {
                                        it.copy(role = com.cuetone.cuetonesync.model.DeviceRole.SLAVE)
                                    }
                                }
                            }
                        },
                        onMakeMasterClick = {
                            val oldMaster = devices.firstOrNull {
                                it.channel == device.channel &&
                                        it.role == DeviceRole.MASTER &&
                                        it.id != device.id
                            }

                            val updatedDevices = devices.map { currentDevice ->
                                if (currentDevice.channel == device.channel) {
                                    if (currentDevice.id == device.id) {
                                        currentDevice.copy(role = DeviceRole.MASTER)
                                    } else {
                                        currentDevice.copy(role = DeviceRole.SLAVE)
                                    }
                                } else {
                                    currentDevice
                                }
                            }

                            devices.clear()
                            devices.addAll(updatedDevices)

                            val newMaster = updatedDevices.firstOrNull {
                                it.id == device.id
                            }

                            val updatedOldMaster = oldMaster?.let { previousMaster ->
                                updatedDevices.firstOrNull {
                                    it.id == previousMaster.id
                                }
                            }

                            val packetsToSend = mutableListOf<Pair<TimecodeDevice, ByteArray>>()

                            if (newMaster != null) {
                                packetsToSend.add(
                                    buildStatePacket(newMaster)
                                )
                            }

                            if (updatedOldMaster != null) {
                                packetsToSend.add(
                                    buildStatePacket(updatedOldMaster)
                                )
                            }

                            if (packetsToSend.isNotEmpty()) {
                                if (BlePermissionManager.hasBleAdvertisePermission(context)) {
                                    sendAndLogStatePackets(packetsToSend)
                                } else {
                                    pendingStatePackets = packetsToSend
                                    permissionLauncher.launch(
                                        BlePermissionManager.requiredPermissions()
                                    )
                                }
                            }
                        }
                    )
                }

                item {
                    AddDeviceCard(
                        onClick = {
                            showAddDialog = true
                        }
                    )
                }
            }
            BottomLogBar(
                onPacketLogClick = onPacketLogClick
            )
        }
    }

    if (showAddDialog) {
        AddDeviceDialog(
            availableDevices = availableToAdd,
            onDismiss = {
                showAddDialog = false
            },
            onDeviceSelected = { selected ->
                devices.add(selected.copy(role = com.cuetone.cuetonesync.model.DeviceRole.SLAVE))
                showAddDialog = false
            }
        )
    }

    if (showMasterDialog) {
        MasterSelectDialog(
            devices = devices,
            onDismiss = {
                showMasterDialog = false
            },
            onMasterSelected = { selectedChannel, selectedMaster ->
                val masterCurrentFrame =
                    TimecodeRepository.getDeviceFrame(selectedMaster)

                val updatedDevices = devices.map { device ->
                    if (device.channel == selectedChannel) {
                        device.copy(
                            frameOffset = masterCurrentFrame - TimecodeRepository.getMasterFrame(),
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
                    targetFrame = masterCurrentFrame,
                    fps = updatedMaster.fps,
                    userBit = updatedMaster.userBit
                )

                if (BlePermissionManager.hasBleAdvertisePermission(context)) {
                    sendAndLogSyncPacket(packet)
                } else {
                    pendingSyncPacket = packet
                    permissionLauncher.launch(
                        BlePermissionManager.requiredPermissions()
                    )
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
                val baseFrame = TimecodeRepository.getMasterFrame()

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
                    sendAndLogSyncPacket(packet)
                } else {
                    pendingSyncPacket = packet
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
fun BottomLogBar(
    onPacketLogClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(end = 16.dp, bottom = 8.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        IconButton(
            onClick = onPacketLogClick
        ) {
            Text(
                text = "☰",
                color = Color.LightGray,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}