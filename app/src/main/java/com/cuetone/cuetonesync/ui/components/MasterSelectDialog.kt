package com.cuetone.cuetonesync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuetone.cuetonesync.model.DeviceChannel
import com.cuetone.cuetonesync.model.DeviceRole
import com.cuetone.cuetonesync.model.TimecodeDevice

@Composable
fun MasterSelectDialog(
    devices: List<TimecodeDevice>,
    onDismiss: () -> Unit,
    onMasterSelected: (DeviceChannel, TimecodeDevice) -> Unit
) {
    var selectedChannel by remember {
        mutableStateOf(devices.firstOrNull()?.channel ?: DeviceChannel.A)
    }

    val channelDevices = devices.filter { it.channel == selectedChannel }

    var selectedMasterId by remember(selectedChannel, devices.size) {
        mutableStateOf(
            channelDevices.firstOrNull { it.role == DeviceRole.MASTER }?.id
                ?: channelDevices.firstOrNull()?.id
        )
    }

    val selectedMaster = channelDevices.firstOrNull { it.id == selectedMasterId }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF242424),
        title = {
            Text(
                text = "Синхронизация канала",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Канал",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                MasterSoftDropdown(
                    selectedText = "CH ${selectedChannel.name}",
                    options = DeviceChannel.entries.map { "CH ${it.name}" },
                    onOptionSelected = { selected ->
                        selectedChannel = DeviceChannel.valueOf(
                            selected.removePrefix("CH ")
                        )
                    }
                )

                Text(
                    text = "Устройство-мастер",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                if (channelDevices.isEmpty()) {
                    Text(
                        text = "В выбранном канале нет устройств",
                        color = Color.LightGray
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        channelDevices.forEach { device ->
                            val selected = selectedMasterId == device.id

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (selected) Color(0xFF6F95B9) else Color(0xFF3A3A3A),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .then(
                                        if (selected) {
                                            Modifier.border(
                                                width = 1.dp,
                                                color = Color.White,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        selectedMasterId = device.id
                                    }
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "${device.name} · ${device.type}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedMaster != null,
                onClick = {
                    selectedMaster?.let {
                        onMasterSelected(selectedChannel, it)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterSoftDropdown(
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
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF6F95B9),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF6F95B9),
                unfocusedLabelColor = Color.Gray,
                cursorColor = Color.White
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