package com.cuetone.cuetonesync.data

import androidx.compose.runtime.mutableStateListOf
import com.cuetone.cuetonesync.model.PacketLogEntry
import kotlinx.coroutines.*

object PacketLogRepository {

    val logs = mutableStateListOf<PacketLogEntry>()

    private val repositoryScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main
    )

    fun addLog(entry: PacketLogEntry) {
        logs.add(0, entry)
    }

    fun updateStatus(
        logId: Long,
        newStatus: String
    ) {
        val index = logs.indexOfFirst { it.id == logId }

        if (index != -1) {
            logs[index] = logs[index].copy(status = newStatus)
        }
    }

    fun simulateConfirmation(
        logId: Long,
        status: String
    ) {
        repositoryScope.launch {
            delay(3000)

            updateStatus(
                logId = logId,
                newStatus = "$status · подтверждение получено (demo)"
            )
        }
    }
}