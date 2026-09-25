package com.cuetone.cuetonesync.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuetone.cuetonesync.model.TimecodeDevice
import com.cuetone.cuetonesync.ui.theme.*

@Composable
fun AddDeviceDialog(
    availableDevices: List<TimecodeDevice>,
    isScanning: Boolean,
    scanStatus: String,
    onDismiss: () -> Unit,
    onScanAgain: () -> Unit,
    onDeviceSelected: (TimecodeDevice) -> Unit
) {
    AlertDialog(
        containerColor = AppSurface,
        onDismissRequest = onDismiss,
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Доступные устройства",
                fontSize = 18.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Primary
                        )
                    }
                    Text(
                        text = scanStatus,
                        color = TextSecondary
                    )
                }

                if (availableDevices.isEmpty()) {
                    Text(
                        text = if (isScanning) "Ожидаем рекламу CUETONE" else "Генераторы не найдены",
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                } else {
                    availableDevices.forEach { device ->
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onDeviceSelected(device)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                contentColor = TextPrimary
                            )
                        ) {
                            Text("${device.name} · ID %04X · CH ${device.channel.name}".format(device.id))
                        }
                    }
                }

                TextButton(
                    onClick = onScanAgain,
                    enabled = !isScanning
                ) {
                    Text("Искать снова")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Закрыть",
                    color = Primary
                )
            }
        }
    )
}
