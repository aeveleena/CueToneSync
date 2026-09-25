package com.cuetone.cuetonesync.data

import androidx.compose.runtime.mutableStateListOf
import com.cuetone.cuetonesync.model.PacketLogEntry

object PacketLogRepository {

    val logs = mutableStateListOf<PacketLogEntry>()

    fun addLog(entry: PacketLogEntry) {
        logs.add(0, entry)
    }
}
