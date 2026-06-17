package com.cuetone.cuetonesync.ui.components

import android.view.Gravity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuetone.cuetonesync.model.TimecodeDevice

@Composable
fun AddDeviceDialog(
    availableDevices: List<TimecodeDevice>,
    onDismiss: () -> Unit,
    onDeviceSelected: (TimecodeDevice) -> Unit
) {
    AlertDialog(
        containerColor = Color(0xFF242424),
        onDismissRequest = onDismiss,
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Доступные устройства",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (availableDevices.isEmpty()) {
                    Text(
                        text = "Нет доступных устройств",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                } else {
                    availableDevices.forEach { device ->
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onDeviceSelected(device)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6F95B9),
                                contentColor = Color.White
                            )
                        ) {
                            Text(device.name)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Закрыть",
                    color = Color(0xFF6F95B9)
                )
            }
        }
    )
}