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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuetone.cuetonesync.model.DeviceChannel
import com.cuetone.cuetonesync.model.DeviceRole
import com.cuetone.cuetonesync.model.TimecodeDevice
import com.cuetone.cuetonesync.ui.theme.*

@Composable
fun MasterSelectDialog(
    devices: List<TimecodeDevice>,
    onDismiss: () -> Unit,
    onMasterSelected: (DeviceChannel, TimecodeDevice) -> Unit
) {
    val availableChannels = remember(devices) {
        devices.map { it.channel }.distinct().sortedBy { it.ordinal }
    }
    var selectedChannel by remember {
        mutableStateOf(availableChannels.firstOrNull() ?: DeviceChannel.A)
    }

    var selectedMasterId by remember(devices) {
        mutableStateOf(
            devices.firstOrNull { it.role == DeviceRole.MASTER }?.id
                ?: devices.firstOrNull()?.id
        )
    }

    val selectedMaster = devices.firstOrNull { it.id == selectedMasterId }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = {
            Text(
                text = "Объединить устройства",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Целевой канал",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                MasterSoftDropdown(
                    selectedText = "CH ${selectedChannel.name}",
                    options = availableChannels.map { "CH ${it.name}" },
                    onOptionSelected = { selected ->
                        selectedChannel = DeviceChannel.valueOf(
                            selected.removePrefix("CH ")
                        )
                    }
                )

                Text(
                    text = "Устройство-мастер",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                if (devices.isEmpty()) {
                    Text(
                        text = "Нет подключённых устройств",
                        color = TextSecondary
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        devices.forEach { device ->
                            val selected = selectedMasterId == device.id

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (selected) Selected else SurfaceElevated,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .then(
                                        if (selected) {
                                            Modifier.border(
                                                width = 1.dp,
                                                color = Primary,
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
                                    text = "${device.name} · ${device.type} · CH ${device.channel.name}",
                                    color = TextPrimary,
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
                    containerColor = Primary,
                    contentColor = TextPrimary,
                    disabledContainerColor = TextDisabled
                )
            ) {
                Text("Объединить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Отмена",
                    color = Primary
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
