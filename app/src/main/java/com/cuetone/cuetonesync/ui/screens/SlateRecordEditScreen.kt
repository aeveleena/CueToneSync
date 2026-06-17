package com.cuetone.cuetonesync.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.cuetone.cuetonesync.ble.PacketFactory
import com.cuetone.cuetonesync.data.PacketLogRepository
import com.cuetone.cuetonesync.data.SlateRepository
import com.cuetone.cuetonesync.model.PacketLogEntry
import com.cuetone.cuetonesync.permissions.BlePermissionManager
import androidx.compose.material3.CheckboxDefaults

@Composable
fun SlateRecordEditScreen(
    recordId: Int,
    onBackClick: () -> Unit
) {
    // Получаем строку таблицы по идентификатору
    // Если запись не найдена, экран не отображается
    val record = SlateRepository.getRecordById(recordId) ?: return

    val context = LocalContext.current
    val bleAdvertiser = remember { CueToneBleAdvertiser(context) }

    // pendingPacket используется, если перед отправкой нужно запросить BLE-разрешение
    var pendingPacket by remember { mutableStateOf<ByteArray?>(null) }
    var bleStatus by remember { mutableStateOf("") }

    // Отправляет сформированный пакет №4 и добавляет запись в журнал пакетов
    fun sendAndLogPacket(packet: ByteArray) {
        bleAdvertiser.sendPacket(packet) { status ->
            val rawPacket = packet.joinToString(" ") {
                "%02X".format(it)
            }

            bleStatus = status

            val logEntry = PacketLogEntry(
                timestamp = System.currentTimeMillis(),
                direction = "OUT",
                deviceName = "Clapper Slate Table",
                packetType = "Packet #4 / Slate Record / ${packet.size} bytes",
                status = "$status · ожидает подтверждения",
                rawData = rawPacket
            )

            PacketLogRepository.addLog(logEntry)

            // В демонстрационном режиме имитируется подтверждение обработки пакета
            PacketLogRepository.simulateConfirmation(
                logId = logEntry.id,
                status = status
            )
        }
    }

    // Запрос разрешений BLE. После получения разрешения отправляется отложенный пакет
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

    // Редактируемые поля строки таблицы
    var episode by remember { mutableStateOf(record.episode) }
    var scene by remember { mutableStateOf(record.scene) }
    var shot by remember { mutableStateOf(record.shot) }
    var take by remember { mutableStateOf(record.take) }
    var note by remember { mutableStateOf(record.note) }
    var isGood by remember { mutableStateOf(record.isGood) }

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
            // Верхняя панель экрана: возврат назад и номер дубля.
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
                    text = "Дубль №${record.number}",
                    color = Color.White,
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
                // Таймкод и GPS-данные отображаются справочно
                InfoText("Таймкод", record.timecode)

                GpsInfoRow(
                    latitude = record.latitude.toString(),
                    longitude = record.longitude.toString(),
                    gpsTime = record.gpsTime
                )

                // Поля съёмочной информации, которые пользователь может изменить
                SlateTextField(
                    label = "Эпизод",
                    value = episode,
                    onValueChange = { episode = it }
                )

                SlateTextField(
                    label = "Сцена",
                    value = scene,
                    onValueChange = { scene = it }
                )

                SlateTextField(
                    label = "Кадр",
                    value = shot,
                    onValueChange = { shot = it }
                )

                SlateTextField(
                    label = "Дубль",
                    value = take,
                    onValueChange = { take = it }
                )

                // Комментарий хранится только в приложении и не передаётся хлопушке
                SlateTextField(
                    label = "Комментарий",
                    value = note,
                    onValueChange = { note = it }
                )

                // Метка удачного дубля передаётся в пакет №4
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isGood,
                        onCheckedChange = { isGood = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF6F95B9),
                            uncheckedColor = Color.Gray,
                            checkmarkColor = Color.White
                        )
                    )

                    Text(
                        text = "Удачный дубль",
                        color = Color.White
                    )
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // Создаём обновлённую строку таблицы
                        val updatedRecord = record.copy(
                            episode = episode,
                            scene = scene,
                            shot = shot,
                            take = take,
                            note = note,
                            isGood = isGood
                        )

                        // Сохраняем изменения в локальном репозитории
                        SlateRepository.updateRecord(updatedRecord)

                        // Формируем пакет №4 для передачи изменённой строки хлопушке
                        val packet =
                            PacketFactory.buildSlateRecordPacket(updatedRecord)

                        // Если разрешение есть — отправляем сразу,
                        // иначе сохраняем пакет и запрашиваем разрешение
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
                        containerColor = Color(0xFF6F95B9),
                        contentColor = Color.White
                    )
                ) {
                    Text("Сохранить")
                }

                if (bleStatus.isNotBlank()) {
                    Text(
                        text = bleStatus,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

// Компонент для отображения неизменяемого информационного поля
@Composable
fun InfoText(
    title: String,
    value: String
) {
    Column {
        Text(
            text = title,
            color = Color.Gray,
            fontSize = 12.sp
        )

        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

// Унифицированное поле ввода для редактируемых данных строки таблицы
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
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF6F95B9),
            unfocusedBorderColor = Color.Gray,
            focusedLabelColor = Color(0xFF6F95B9),
            unfocusedLabelColor = Color.Gray,
            cursorColor = Color.White
        )
    )
}

// Отображает широту, долготу и GPS-время в одной строке
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
        GpsInfoCell(
            title = "Широта",
            value = latitude,
            modifier = Modifier.weight(1f)
        )

        GpsInfoCell(
            title = "Долгота",
            value = longitude,
            modifier = Modifier.weight(1f)
        )

        GpsInfoCell(
            title = "GPS-время",
            value = gpsTime,
            modifier = Modifier.weight(1f)
        )
    }
}

// Ячейка справочной GPS-информации
@Composable
fun GpsInfoCell(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = title,
            color = Color.Gray,
            fontSize = 12.sp,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}