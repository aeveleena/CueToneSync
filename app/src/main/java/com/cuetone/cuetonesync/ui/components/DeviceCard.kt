package com.cuetone.cuetonesync.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cuetone.cuetonesync.model.DeviceRole
import com.cuetone.cuetonesync.model.TimecodeDevice
import com.cuetone.cuetonesync.ui.theme.AppSurface
import com.cuetone.cuetonesync.ui.theme.OfflineSurface
import com.cuetone.cuetonesync.ui.theme.OfflineTextSecondary
import com.cuetone.cuetonesync.ui.theme.Primary
import com.cuetone.cuetonesync.ui.theme.PrimarySoft
import com.cuetone.cuetonesync.ui.theme.Recording
import com.cuetone.cuetonesync.ui.theme.RecordingSoft
import com.cuetone.cuetonesync.ui.theme.SurfaceElevated
import com.cuetone.cuetonesync.ui.theme.TextDisabled
import com.cuetone.cuetonesync.ui.theme.TextPrimary
import com.cuetone.cuetonesync.ui.theme.TextSecondary
import com.cuetone.cuetonesync.util.formatTimecode

@Composable
fun DeviceCard(
    device: TimecodeDevice,
    currentFrame: Int,
    isOnline: Boolean,
    onClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onMakeMasterClick: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val primaryText = if (isOnline) TextPrimary else TextDisabled
    val secondaryText = if (isOnline) TextSecondary else OfflineTextSecondary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isOnline) SurfaceElevated else OfflineSurface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = device.name,
                    color = primaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Действия с устройством",
                            tint = secondaryText,
                            modifier = Modifier.size(20.dp)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = device.type,
                    color = secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${device.batteryLevel}%",
                    color = secondaryText,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = formatTimecode(currentFrame, device.fps.frameRate),
                color = primaryText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOnline) {
                    InfoChip(
                        text = roleTitle(device.role),
                        color = Primary,
                        backgroundColor = PrimarySoft,
                        compact = true
                    )

                    if (device.isRecording) {
                        InfoChip(
                            text = "REC",
                            color = Recording,
                            backgroundColor = RecordingSoft,
                            compact = true
                        )
                    } else {
                        InfoChip(
                            text = "READY",
                            color = Primary,
                            backgroundColor = PrimarySoft,
                            compact = true
                        )
                    }
                } else {
                    InfoChip(
                        text = roleTitle(device.role),
                        color = TextDisabled,
                        backgroundColor = AppSurface,
                        compact = true
                    )
                    InfoChip(
                        text = "OFFLINE",
                        color = TextDisabled,
                        backgroundColor = AppSurface,
                        compact = true
                    )
                }
            }
        }
    }
}

private fun roleTitle(role: DeviceRole): String = when (role) {
    DeviceRole.MASTER -> "Master"
    DeviceRole.SLAVE -> "Slave"
}
