package com.cuetone.cuetonesync.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuetone.cuetonesync.data.PacketLogRepository
import com.cuetone.cuetonesync.ui.theme.AppBackground
import com.cuetone.cuetonesync.ui.theme.AppSurface
import com.cuetone.cuetonesync.ui.theme.TextDisabled
import com.cuetone.cuetonesync.ui.theme.TextPrimary
import com.cuetone.cuetonesync.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PacketLogScreen(
    onBackClick: () -> Unit
) {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp)
        ) {
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
                    text = "Журнал BLE-пакетов",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(PacketLogRepository.logs) { log ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = AppSurface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = "${formatter.format(Date(log.timestamp))} · ${log.direction}",
                                color = TextSecondary
                            )

                            Text(
                                text = log.packetType,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = log.deviceName,
                                color = TextSecondary
                            )

                            Text(
                                text = log.status,
                                color = TextSecondary
                            )

                            Text(
                                text = log.rawData,
                                color = TextDisabled,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
