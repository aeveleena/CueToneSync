package com.cuetone.cuetonesync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuetone.cuetonesync.data.DevicesRepository
import com.cuetone.cuetonesync.model.DeviceRole
import com.cuetone.cuetonesync.model.FpsOption
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.cuetone.cuetonesync.model.DeviceChannel
import com.cuetone.cuetonesync.model.AutoLockOption
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.cuetone.cuetonesync.ble.CueToneBleAdvertiser
import com.cuetone.cuetonesync.ble.CueToneTransmission
import com.cuetone.cuetonesync.ble.CueToneLegacyProtocol
import com.cuetone.cuetonesync.ble.PacketFactory
import com.cuetone.cuetonesync.data.PacketLogRepository
import com.cuetone.cuetonesync.model.PacketLogEntry
import com.cuetone.cuetonesync.permissions.BlePermissionManager
import com.cuetone.cuetonesync.data.TimecodeRepository
import com.cuetone.cuetonesync.ui.theme.*

@Composable
fun DeviceSettingsScreen(
    deviceId: Int,
    onBackClick: () -> Unit
) {
    val device = DevicesRepository.getDeviceById(deviceId) ?: return

    val context = LocalContext.current
    val bleAdvertiser = remember { CueToneBleAdvertiser(context) }

    var bleStatus by remember { mutableStateOf("") }
    var pendingPackets by remember { mutableStateOf<List<CueToneTransmission>>(emptyList()) }

    fun sendAndLogPackets(packets: List<CueToneTransmission>) {
        bleAdvertiser.sendPacketsSequentially(packets) { _, packet, status ->
            val rawPacket = packet.toHexString()

            val packetName = when {
                packet.packetType == CueToneLegacyProtocol.PACKET_DEVICE_NAME ->
                    "Generator Name"
                device.type == "Clapper" ->
                    "Packet #2 / Clapper Settings"
                else ->
                    "Packet #1 / Generator Settings"
            }

            bleStatus = status

            val logEntry = PacketLogEntry(
                timestamp = System.currentTimeMillis(),
                direction = "OUT",
                deviceName = device.name,
                packetType = "$packetName / target 0x%04X / ${packet.logicalPayload.size} bytes / ${packet.fragmentCount} fragments".format(packet.target),
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
                if (pendingPackets.isNotEmpty()) {
                    sendAndLogPackets(pendingPackets)
                }
                pendingPackets = emptyList()
            } else {
                bleStatus = "BLE-разрешение отклонено"
            }
        }

    val todayUserBit = remember {
        val today = LocalDate.now()
        today.format(DateTimeFormatter.ofPattern("dd:MM:yy:00"))
    }

    var deviceName by remember {
        mutableStateOf(device.name)
    }

    var userBit by remember {
        mutableStateOf(device.userBit.ifBlank { todayUserBit })
    }

    var selectedFps by remember {
        mutableStateOf(device.fps)
    }

    var outputVolume by remember {
        mutableStateOf(device.outputVolume)
    }

    var displayBrightness by remember {
        mutableIntStateOf(device.displayBrightness)
    }

    var isMaster by remember {
        mutableStateOf(device.role == DeviceRole.MASTER)
    }

    var selectedChannel by remember {
        mutableStateOf(device.channel)
    }

    var selectedAutoLock by remember {
        mutableStateOf(device.autoLock)
    }

    var gpsEnabled by remember {
        mutableStateOf(device.gpsEnabled)
    }

    var gpsSyncEnabled by remember {
        mutableStateOf(device.gpsSyncEnabled)
    }

    val fpsOptions = listOf(
        FpsOption.FPS_23_98,
        FpsOption.FPS_24,
        FpsOption.FPS_25,
        FpsOption.FPS_29_97,
        FpsOption.FPS_29_97_DF,
        FpsOption.FPS_30,
        FpsOption.FPS_47_95,
        FpsOption.FPS_48,
        FpsOption.FPS_50,
        FpsOption.FPS_59_94,
        FpsOption.FPS_59_94_DF,
        FpsOption.FPS_60
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text("<-")
                }

                Text(
                    text = "Настройки",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SettingsSectionTitle("Имя устройства")

                OutlinedTextField(
                    value = deviceName,
                    onValueChange = {
                        deviceName = if (device.type == "Generator") {
                            utf8Prefix(it, 6)
                        } else {
                            it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = if (device.type == "Generator") {
                        { Text("До 6 байт") }
                    } else {
                        null
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Border,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = Primary,
                        focusedContainerColor = SurfaceElevated,
                        unfocusedContainerColor = SurfaceElevated,
                        focusedSupportingTextColor = TextSecondary,
                        unfocusedSupportingTextColor = TextSecondary
                    )
                )

                SettingsSectionTitle("User Bit")

                OutlinedTextField(
                    value = userBit,
                    onValueChange = { userBit = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Border,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = Primary,
                        focusedContainerColor = SurfaceElevated,
                        unfocusedContainerColor = SurfaceElevated
                    )
                )

                SettingsSectionTitle("FPS")

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    fpsOptions.chunked(3).forEach { rowItems ->

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            rowItems.forEach { fps ->

                                val selected =
                                    fps == selectedFps

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (selected)
                                                Selected
                                            else
                                                AppSurface,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedFps = fps
                                        }
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {

                                    Text(
                                        text = fps.title,
                                        color =
                                            if (selected)
                                                TextPrimary
                                            else
                                                TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                SettingsSectionTitle("Output Volume")

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    OutputButton(
                        title = "LTC",
                        selected = outputVolume == "LTC"
                    ) {
                        outputVolume = "LTC"
                    }

                    OutputButton(
                        title = "Audio-TC",
                        selected = outputVolume == "Audio-TC"
                    ) {
                        outputVolume = "Audio-TC"
                    }
                }

                if (device.type == "Clapper") {
                    SettingsSectionTitle("GPS")

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutputButton(
                            title = "GPS OFF",
                            selected = !gpsEnabled
                        ) {
                            gpsEnabled = false
                        }

                        OutputButton(
                            title = "GPS ON",
                            selected = gpsEnabled
                        ) {
                            gpsEnabled = true
                        }
                    }

                    SettingsSectionTitle("GPS Sync")

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutputButton(
                            title = "SYNC OFF",
                            selected = !gpsSyncEnabled
                        ) {
                            gpsSyncEnabled = false
                        }

                        OutputButton(
                            title = "SYNC ON",
                            selected = gpsSyncEnabled
                        ) {
                            gpsSyncEnabled = true
                        }
                    }
                }

                if (device.type != "Clapper") {
                    SettingsSectionTitle("Автоблокировка")

                    SoftDropdown(
                        selectedText = selectedAutoLock.title,
                        options = AutoLockOption.entries.map { it.title },
                        onOptionSelected = { selectedTitle ->
                            selectedAutoLock =
                                AutoLockOption.entries.first { it.title == selectedTitle }
                        }
                    )
                }

                SettingsSectionTitle("Яркость дисплея")

                BrightnessGrid(
                    selectedBrightness = displayBrightness,
                    onBrightnessSelected = { displayBrightness = it }
                )

                if (device.type != "Clapper") {

                    SettingsSectionTitle("Роль устройства")

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        OutputButton(
                            title = "MASTER",
                            selected = isMaster
                        ) {
                            isMaster = true
                        }

                        OutputButton(
                            title = "SLAVE",
                            selected = !isMaster
                        ) {
                            isMaster = false
                        }
                    }
                }

                SettingsSectionTitle("Канал связи")

                SoftDropdown(
                    selectedText = "CH ${selectedChannel.name}",
                    options = DeviceChannel.entries.map { "CH ${it.name}" },
                    onOptionSelected = { selected ->
                        selectedChannel = DeviceChannel.valueOf(
                            selected.removePrefix("CH ")
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val requestedName = deviceName.trim()
                        val generatorNameChanged =
                            device.type == "Generator" &&
                                requestedName.isNotEmpty() &&
                                requestedName != device.name
                        val updatedDevice = device.copy(
                            name = if (device.type == "Generator") device.name else requestedName,
                            userBit = userBit,
                            fps = selectedFps,
                            outputVolume = outputVolume,
                            displayBrightness = displayBrightness,
                            channel = selectedChannel,
                            autoLock = selectedAutoLock,
                            gpsEnabled = gpsEnabled,
                            gpsSyncEnabled = gpsSyncEnabled,
                            role =
                                if (isMaster)
                                    DeviceRole.MASTER
                                else
                                    DeviceRole.SLAVE
                        )

                        DevicesRepository.updateDeviceWithMasterRule(updatedDevice)

                        val packets = buildList {
                            add(
                                PacketFactory.buildDeviceStatePacket(
                                    device = updatedDevice,
                                    currentFrame = TimecodeRepository.getDeviceFrame(updatedDevice)
                                )
                            )
                            if (generatorNameChanged) {
                                add(
                                    PacketFactory.buildGeneratorNamePacket(
                                        deviceId = updatedDevice.id,
                                        name = requestedName
                                    )
                                )
                            }
                        }

                        if (BlePermissionManager.hasBleAdvertisePermission(context)) {
                            sendAndLogPackets(packets)
                        } else {
                            pendingPackets = packets
                            permissionLauncher.launch(
                                BlePermissionManager.requiredPermissions()
                            )
                        }

                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}

private fun utf8Prefix(value: String, maxByteLength: Int): String {
    val result = StringBuilder()
    var byteCount = 0
    value.forEach { character ->
        val characterSize = character.toString().encodeToByteArray().size
        if (byteCount + characterSize > maxByteLength) return result.toString()
        result.append(character)
        byteCount += characterSize
    }
    return result.toString()
}

@Composable
fun SettingsSectionTitle(
    title: String
) {
    Text(
        text = title,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    )
}

@Composable
fun OutputButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (selected)
                    Selected
                else
                    AppSurface,
                RoundedCornerShape(12.dp)
            )
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 18.dp,
                vertical = 12.dp
            ),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = title,
            color =
                if (selected)
                    TextPrimary
                else
                    TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BrightnessGrid(
    selectedBrightness: Int,
    onBrightnessSelected: (Int) -> Unit
) {
    val brightnessOptions = listOf(
        10, 20, 30, 40, 50,
        60, 70, 80, 90, 100
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        brightnessOptions.chunked(5).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { brightness ->
                    val selected = brightness == selectedBrightness

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) Selected else AppSurface,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onBrightnessSelected(brightness)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$brightness%",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoftDropdown(
    selectedText: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        Box(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .background(
                    SurfaceElevated,
                    RoundedCornerShape(14.dp)
                )
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = selectedText,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = SurfaceElevated
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = TextPrimary
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
