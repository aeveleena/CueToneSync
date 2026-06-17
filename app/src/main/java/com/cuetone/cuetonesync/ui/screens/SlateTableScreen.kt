package com.cuetone.cuetonesync.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.cuetone.cuetonesync.data.SlateRepository

@Composable
fun SlateTableScreen(
    onBackClick: () -> Unit,
    onRecordClick: (Int) -> Unit
) {
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
                    text = "Таблица дублей",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(SlateRepository.records) { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onRecordClick(record.id)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF242424)
                        ),
                        border = if (record.isGood) {
                            BorderStroke(
                                width = 1.5.dp,
                                color = Color(0xFF6F95B9)
                            )
                        } else {
                            null
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "№${record.number}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = record.timecode,
                                color = Color.LightGray
                            )

                            Text(
                                text = "С${record.scene} / Д${record.take}",
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}