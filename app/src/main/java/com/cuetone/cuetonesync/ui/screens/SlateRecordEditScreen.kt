package com.cuetone.cuetonesync.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuetone.cuetonesync.ble.CueToneBleAdvertiser
import com.cuetone.cuetonesync.ble.CueToneTransmission
import com.cuetone.cuetonesync.ble.PacketFactory
import com.cuetone.cuetonesync.data.DevicesRepository
import com.cuetone.cuetonesync.data.PacketLogRepository
import com.cuetone.cuetonesync.data.local.CueToneDatabase
import com.cuetone.cuetonesync.data.repository.SlateRepository
import com.cuetone.cuetonesync.model.PacketLogEntry
import com.cuetone.cuetonesync.permissions.BlePermissionManager
import com.cuetone.cuetonesync.ui.theme.AppBackground
import com.cuetone.cuetonesync.ui.theme.Border
import com.cuetone.cuetonesync.ui.theme.Primary
import com.cuetone.cuetonesync.ui.theme.SurfaceElevated
import com.cuetone.cuetonesync.ui.theme.TextPrimary
import com.cuetone.cuetonesync.ui.theme.TextSecondary
import com.cuetone.cuetonesync.ui.components.BackButton
import kotlinx.coroutines.launch

@Composable
fun SlateRecordEditScreen(
    recordId: Long,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { CueToneDatabase.getDatabase(context) }
    val slateRepository = remember { SlateRepository(database.slateRecordDao()) }
    val record by slateRepository.observeRecord(recordId).collectAsState(initial = null)
    val currentRecord = record ?: return
    val table by database.logTableDao()
        .observeTable(currentRecord.tableId)
        .collectAsState(initial = null)
    val targetClapper = DevicesRepository.getClapperBySerialOrFirst(
        table?.clapperSerialNumber
    )
    val scope = rememberCoroutineScope()
    val bleAdvertiser = remember { CueToneBleAdvertiser(context) }

    var pendingPacket by remember { mutableStateOf<CueToneTransmission?>(null) }
    var bleStatus by remember { mutableStateOf("") }

    fun sendAndLogPacket(packet: CueToneTransmission) {
        bleAdvertiser.sendPacket(packet) { status ->
            val rawPacket = packet.toHexString()

            bleStatus = status

            val logEntry = PacketLogEntry(
                timestamp = System.currentTimeMillis(),
                direction = "OUT",
                deviceName = "Clapper Slate Table",
                packetType = "Packet #4 / Slate Record / ${packet.logicalPayload.size} bytes / ${packet.fragmentCount} fragments",
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
            if (permissions.values.all { it }) {
                pendingPacket?.let { packet -> sendAndLogPacket(packet) }
                pendingPacket = null
            } else {
                bleStatus = "BLE-разрешение отклонено"
            }
        }

    var episode by remember(currentRecord.id) { mutableStateOf(currentRecord.episode) }
    var scene by remember(currentRecord.id) { mutableStateOf(currentRecord.scene) }
    var shot by remember(currentRecord.id) { mutableStateOf(currentRecord.shot) }
    var take by remember(currentRecord.id) { mutableStateOf(currentRecord.take) }
    var note by remember(currentRecord.id) { mutableStateOf(currentRecord.note) }
    var isGood by remember(currentRecord.id) { mutableStateOf(currentRecord.isGood) }

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
                BackButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                Text(
                    text = "Дубль №${currentRecord.number}",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoText("Таймкод", currentRecord.timecode)

                GpsInfoRow(
                    latitude = currentRecord.latitude.toString(),
                    longitude = currentRecord.longitude.toString(),
                    gpsTime = currentRecord.gpsTime
                )

                SlateTextField("Эпизод", episode) { episode = it }
                SlateTextField("Сцена", scene) { scene = it }
                SlateTextField("Кадр", shot) { shot = it }
                SlateTextField("Дубль", take) { take = it }
                SlateTextField("Комментарий", note) { note = it }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isGood,
                        onCheckedChange = { isGood = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Primary,
                            uncheckedColor = Border,
                            checkmarkColor = TextPrimary
                        )
                    )

                    Text(
                        text = "Удачный дубль",
                        color = TextPrimary
                    )
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val updatedRecord = currentRecord.copy(
                            episode = episode,
                            scene = scene,
                            shot = shot,
                            take = take,
                            note = note,
                            isGood = isGood
                        )

                        scope.launch {
                            slateRepository.updateRecord(updatedRecord)
                        }

                        val clapper = targetClapper
                        if (clapper == null) {
                            bleStatus = "К таблице не привязана хлопушка"
                            return@Button
                        }

                        val packet = PacketFactory.buildSlateRecordPacket(
                            record = updatedRecord,
                            clapperDeviceId = clapper.id
                        )

                        if (BlePermissionManager.hasBleAdvertisePermission(context)) {
                            sendAndLogPacket(packet)
                        } else {
                            pendingPacket = packet
                            permissionLauncher.launch(
                                BlePermissionManager.requiredPermissions()
                            )
                        }

                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = TextPrimary
                    )
                ) {
                    Text("Сохранить")
                }

                if (bleStatus.isNotBlank()) {
                    Text(
                        text = bleStatus,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun InfoText(
    title: String,
    value: String
) {
    Column {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 12.sp
        )

        Text(
            text = value,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SlateTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = SurfaceElevated,
            unfocusedContainerColor = SurfaceElevated,
            focusedBorderColor = Primary,
            unfocusedBorderColor = Border,
            focusedLabelColor = Primary,
            unfocusedLabelColor = TextSecondary,
            cursorColor = Primary
        )
    )
}

@Composable
fun GpsInfoRow(
    latitude: String,
    longitude: String,
    gpsTime: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GpsInfoCell("Широта", latitude, Modifier.weight(1f))
        GpsInfoCell("Долгота", longitude, Modifier.weight(1f))
        GpsInfoCell("GPS-время", gpsTime, Modifier.weight(1f))
    }
}

@Composable
fun GpsInfoCell(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 12.sp,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}
