package com.cuetone.cuetonesync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuetone.cuetonesync.model.DeviceChannel
import com.cuetone.cuetonesync.ui.theme.*

@Composable
fun DevicesToolbar(
    selectedChannel: DeviceChannel?,
    availableChannels: List<DeviceChannel>,
    onChannelSelected: (DeviceChannel?) -> Unit,
    onSyncAllClick: () -> Unit,
    onManualSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onSyncAllClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = TextPrimary,
                disabledContainerColor = TextDisabled
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text("Sync All")
        }

        OutlinedButton(
            onClick = onManualSyncClick,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SurfaceElevated,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text("Manual TC")
        }

        HeaderChannelDropdown(
            selectedChannel = selectedChannel,
            availableChannels = availableChannels,
            onChannelSelected = onChannelSelected
        )
    }
}

@Composable
private fun HeaderChannelDropdown(
    selectedChannel: DeviceChannel?,
    availableChannels: List<DeviceChannel>,
    onChannelSelected: (DeviceChannel?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .background(PrimarySoft, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = selectedChannel?.let { "CH ${it.name}" } ?: "All",
                color = Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = SurfaceElevated
        ) {
            DropdownMenuItem(
                text = { Text("All", color = TextPrimary) },
                onClick = {
                    onChannelSelected(null)
                    expanded = false
                }
            )

            availableChannels.forEach { channel ->
                DropdownMenuItem(
                    text = { Text("CH ${channel.name}", color = TextPrimary) },
                    onClick = {
                        onChannelSelected(channel)
                        expanded = false
                    }
                )
            }
        }
    }
}
