package com.cuetone.cuetonesync.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuetone.cuetonesync.model.DeviceChannel
import androidx.compose.foundation.background
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.material3.DropdownMenu

@Composable
fun TimecodeHeader(
    mainTimecode: String,
    fps: String?,
    selectedChannel: DeviceChannel?,
    availableChannels: List<DeviceChannel>,
    onChannelSelected: (DeviceChannel) -> Unit,
    onSyncAllClick: () -> Unit,
    onManualSyncClick: () -> Unit
) {
    Column {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mainTimecode,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(150.dp)
            )

            Text(
                text = "| ${fps ?: "--"}",
                color = Color.LightGray,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            HeaderChannelDropdown(
                selectedChannel = selectedChannel,
                availableChannels = availableChannels,
                onChannelSelected = onChannelSelected
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSyncAllClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8E454C),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Sync All")
            }

            OutlinedButton(
                onClick = onManualSyncClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Manual TC")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderChannelDropdown(
    selectedChannel: DeviceChannel?,
    availableChannels: List<DeviceChannel>,
    onChannelSelected: (DeviceChannel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .background(
                    Color(0xFF242424),
                    RoundedCornerShape(10.dp)
                )
                .clickable {
                    expanded = true
                }
                .padding(horizontal = 8.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = selectedChannel?.let { "CH ${it.name}" } ?: "CH",
                color = Color(0xFF6F95B9),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            containerColor = Color(0xFF242424)
        ) {
            availableChannels.forEach { channel ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "CH ${channel.name}",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        onChannelSelected(channel)
                        expanded = false
                    }
                )
            }
        }
    }
}