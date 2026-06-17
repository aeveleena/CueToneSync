package com.cuetone.cuetonesync.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuetone.cuetonesync.data.PacketLogRepository
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PacketLogScreen(
    onBackClick: () -> Unit
) {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
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
                    color = Color.White,
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
                            containerColor = Color(0xFF242424)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = "${formatter.format(Date(log.timestamp))} · ${log.direction}",
                                color = Color.LightGray
                            )

                            Text(
                                text = log.packetType,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = log.deviceName,
                                color = Color.LightGray
                            )

                            Text(
                                text = log.status,
                                color = Color.LightGray
                            )

                            Text(
                                text = log.rawData,
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}