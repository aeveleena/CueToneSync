package com.cuetone.cuetonesync.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuetone.cuetonesync.model.DeviceRole
import com.cuetone.cuetonesync.model.TimecodeDevice
import com.cuetone.cuetonesync.util.FRAME_RATE
import com.cuetone.cuetonesync.util.formatTimecode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.FlowRow

@Composable
fun DeviceCard(
    device: TimecodeDevice,
    currentFrame: Int,
    onClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onMakeMasterClick: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF242424)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = device.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${device.batteryLevel}%",
                    color = Color.LightGray
                )
            }

            Text(
                text = device.type,
                color = Color.Gray
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimecode(currentFrame, device.fps.frameRate),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(150.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "| ${device.fps.title}",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {

                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InfoChip(text = roleTitle(device.role))
                    InfoChip(text = "CH ${device.channel.name}")

                    if (device.isRecording) {
                        InfoChip(text = "REC", color = Color(0xFF8E454C))
                    } else {
                        InfoChip(text = "READY")
                    }
                }

                Box(
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    IconButton(
                        onClick = { menuExpanded = true }
                    ) {
                        Text(
                            text = "⋮",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Удалить") },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )

                        if (device.role != DeviceRole.MASTER) {
                            DropdownMenuItem(
                                text = { Text("Назначить мастером") },
                                onClick = {
                                    menuExpanded = false
                                    onMakeMasterClick()
                                }
                            )
                        }
                    }
                }
            }

        }
    }
}

private fun roleTitle(role: DeviceRole): String {
    return when (role) {
        DeviceRole.MASTER -> "Master"
        DeviceRole.SLAVE -> "Slave"
    }
}