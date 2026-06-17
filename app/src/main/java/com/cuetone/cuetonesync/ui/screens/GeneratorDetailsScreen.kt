package com.cuetone.cuetonesync.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuetone.cuetonesync.ble.CueToneBleAdvertiser
import com.cuetone.cuetonesync.data.DevicesRepository
import com.cuetone.cuetonesync.data.PacketLogRepository
import com.cuetone.cuetonesync.data.TimecodeRepository
import com.cuetone.cuetonesync.model.PacketLogEntry
import com.cuetone.cuetonesync.permissions.BlePermissionManager
import com.cuetone.cuetonesync.util.FRAME_DURATION_MS
import com.cuetone.cuetonesync.util.formatTimecode
import kotlinx.coroutines.delay
import com.cuetone.cuetonesync.ble.PacketFactory
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import com.cuetone.cuetonesync.R

@Composable
fun GeneratorDetailsScreen(
    deviceId: Int,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val device = DevicesRepository.getDeviceById(deviceId)

    if (device == null) {
        return
    }

    val context = LocalContext.current
    val bleAdvertiser = remember { CueToneBleAdvertiser(context) }

    var bleStatus by remember { mutableStateOf("") }
    var pendingPacket by remember { mutableStateOf<ByteArray?>(null) }

    fun sendAndLogPacket(packet: ByteArray) {
        bleAdvertiser.sendPacket(packet) { status ->
            val rawPacket = packet.joinToString(" ") {
                "%02X".format(it)
            }

            bleStatus = status

            val logEntry = PacketLogEntry(
                timestamp = System.currentTimeMillis(),
                direction = "OUT",
                deviceName = device.name,
                packetType = "Packet #1 / Generator State / ${packet.size} bytes",
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
                pendingPacket?.let { packet ->
                    sendAndLogPacket(packet)
                }
                pendingPacket = null
            } else {
                bleStatus = "BLE-разрешение отклонено"
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
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            Column {
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
                        text = device.name,
                        color = Color.White,
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
                            .size(32.dp).offset(y = (-6).dp),
                        onClick = onSettingsClick
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = "Настройки",
                            tint = Color(0xFF6F95B9),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DeviceInfoCard(
                    title = "Battery",
                    value = "${device.batteryLevel}%"
                )

                DeviceInfoCard(
                    title = "FPS",
                    value = device.fps.title
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DeviceInfoCard(
                    title = "User Bit",
                    value = device.userBit
                )

                DeviceInfoCard(
                    title = "Connection",
                    value = "BLE"
                )
            }

            Spacer(modifier = Modifier.height(90.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatTimecode(currentFrame, device.fps.frameRate),
                    color = Color.White,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(130.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val packet = PacketFactory.buildGeneratorPacket(
                            device = device,
                            currentFrame = TimecodeRepository.getDeviceFrame(device)
                        )

                        if (BlePermissionManager.hasBleAdvertisePermission(context)) {
                            sendAndLogPacket(packet)
                        } else {
                            pendingPacket = packet
                            permissionLauncher.launch(
                                BlePermissionManager.requiredPermissions()
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Sync")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val updatedDevice = device.copy(
                            frameOffset = TimecodeRepository.offsetForTargetFrame(0)
                        )

                        DevicesRepository.updateDevice(updatedDevice)

                        val packet = PacketFactory.buildGeneratorPacket(
                            device = updatedDevice,
                            currentFrame = TimecodeRepository.getDeviceFrame(updatedDevice)
                        )

                        if (BlePermissionManager.hasBleAdvertisePermission(context)) {
                            sendAndLogPacket(packet)
                        } else {
                            pendingPacket = packet
                            permissionLauncher.launch(
                                BlePermissionManager.requiredPermissions()
                            )
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Reset TC")
                }
            }

            if (bleStatus.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = bleStatus,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun DeviceInfoCard(
    title: String,
    value: String
) {
    Card(
        modifier = Modifier.width(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF242424)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}