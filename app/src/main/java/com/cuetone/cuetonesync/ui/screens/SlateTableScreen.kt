package com.cuetone.cuetonesync.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuetone.cuetonesync.data.local.CueToneDatabase
import com.cuetone.cuetonesync.data.repository.ProjectRepository
import com.cuetone.cuetonesync.data.repository.SlateRepository
import com.cuetone.cuetonesync.ui.theme.AppBackground
import com.cuetone.cuetonesync.ui.theme.AppSurface
import com.cuetone.cuetonesync.ui.theme.Primary
import com.cuetone.cuetonesync.ui.theme.TextPrimary
import com.cuetone.cuetonesync.ui.theme.TextSecondary
import com.cuetone.cuetonesync.ui.components.BackButton

@Composable
fun SlateTableScreen(
    tableId: Long,
    onBackClick: () -> Unit,
    onRecordClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val database = remember { CueToneDatabase.getDatabase(context) }
    val slateRepository = remember { SlateRepository(database.slateRecordDao()) }
    val projectRepository = remember {
        ProjectRepository(
            database.projectDao(),
            database.folderDao(),
            database.logTableDao()
        )
    }
    val records by slateRepository.observeRecords(tableId).collectAsState(initial = emptyList())
    val table by projectRepository.observeTable(tableId).collectAsState(initial = null)

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
                BackButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                Text(
                    text = table?.name ?: "Таблица дублей",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(records) { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onRecordClick(record.id)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = AppSurface
                        ),
                        border = if (record.isGood) {
                            BorderStroke(
                                width = 1.5.dp,
                                color = Primary
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
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = record.timecode,
                                color = TextSecondary
                            )

                            Text(
                                text = buildString {
                                    record.cameraIndex?.let { cameraIndex ->
                                        append("К")
                                        append('A' + cameraIndex.coerceIn(0, 2))
                                        append(" · ")
                                    }
                                    append("С${record.scene} / Д${record.take}")
                                },
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
