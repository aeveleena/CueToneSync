package com.cuetone.cuetonesync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuetone.cuetonesync.model.DeviceChannel
import com.cuetone.cuetonesync.model.FpsOption
import com.cuetone.cuetonesync.model.TimecodeDevice
import com.cuetone.cuetonesync.model.defaultUserBit

@Composable
fun ManualSyncDialog(
    devices: List<TimecodeDevice>,
    onDismiss: () -> Unit,
    onSync: (Int, FpsOption, DeviceChannel, TimecodeDevice, String) -> Unit
) {
    var hours by remember { mutableStateOf("00") }
    var minutes by remember { mutableStateOf("00") }
    var seconds by remember { mutableStateOf("00") }

    var userBit by remember {
        mutableStateOf(defaultUserBit())
    }

    var selectedFps by remember {
        mutableStateOf(FpsOption.FPS_25)
    }

    var selectedChannel by remember {
        mutableStateOf(devices.firstOrNull()?.channel ?: DeviceChannel.A)
    }

    val channelDevices = devices.filter { it.channel == selectedChannel }

    var selectedMasterId by remember(selectedChannel, devices.size) {
        mutableStateOf(channelDevices.firstOrNull()?.id)
    }

    val selectedMaster = channelDevices.firstOrNull { it.id == selectedMasterId }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF242424),
        title = {
            Text(
                text = "Ручная настройка таймкода",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DialogSectionTitle("Канал")

                ManualSoftDropdown(
                    selectedText = "CH ${selectedChannel.name}",
                    options = DeviceChannel.entries.map { "CH ${it.name}" },
                    onOptionSelected = { selected ->
                        selectedChannel = DeviceChannel.valueOf(
                            selected.removePrefix("CH ")
                        )
                    }
                )

                DialogSectionTitle("Устройство-мастер")

                ManualSoftDropdown(
                    selectedText = selectedMaster?.name ?: "Нет устройств в канале",
                    options = channelDevices.map { it.name },
                    onOptionSelected = { selectedName ->
                        selectedMasterId = channelDevices.firstOrNull {
                            it.name == selectedName
                        }?.id
                    }
                )

                DialogSectionTitle("Таймкод")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftTimecodeField("Часы", hours, { hours = it }, Modifier.weight(1f))
                    SoftTimecodeField("Мин", minutes, { minutes = it }, Modifier.weight(1f))
                    SoftTimecodeField("Сек", seconds, { seconds = it }, Modifier.weight(1f))
                }

                DialogSectionTitle("User Bit")

                OutlinedTextField(
                    value = userBit,
                    onValueChange = {
                        userBit = it
                            .filter { char -> char.isDigit() || char == ':' }
                            .take(11)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6F95B9),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color.White
                    )
                )

                DialogSectionTitle("FPS")

                FpsGrid(
                    selectedFps = selectedFps,
                    onFpsSelected = { selectedFps = it }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = selectedMaster != null,
                onClick = {
                    val h = hours.toIntOrNull() ?: 0
                    val m = minutes.toIntOrNull() ?: 0
                    val s = seconds.toIntOrNull() ?: 0

                    val totalFrames =
                        ((h * 60 + m) * 60 + s) * selectedFps.frameRate

                    selectedMaster?.let { master ->
                        onSync(
                            totalFrames,
                            selectedFps,
                            selectedChannel,
                            master,
                            userBit
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6F95B9),
                    contentColor = Color.White
                )
            ) {
                Text("Синхронизировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Отмена",
                    color = Color(0xFF6F95B9)
                    )
            }
        }
    )
}

@Composable
fun DialogSectionTitle(
    title: String
) {
    Text(
        text = title,
        color = Color.White,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun SoftTimecodeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            onValueChange(
                newValue
                    .filter { it.isDigit() }
                    .take(2)
            )
        },
        label = { Text(label) },
        modifier = modifier,
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

@Composable
fun FpsGrid(
    selectedFps: FpsOption,
    onFpsSelected: (FpsOption) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FpsOption.entries.chunked(3).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { fps ->
                    val selected = fps == selectedFps

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) Color(0xFF6F95B9) else Color(0xFF303030),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onFpsSelected(fps) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = fps.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSoftDropdown(
    selectedText: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF6F95B9),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF6F95B9),
                unfocusedLabelColor = Color.Gray
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFF242424)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = Color.White
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