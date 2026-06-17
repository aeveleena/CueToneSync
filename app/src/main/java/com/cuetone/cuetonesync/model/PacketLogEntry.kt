package com.cuetone.cuetonesync.model

data class PacketLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long,
    val direction: String,
    val deviceName: String,
    val packetType: String,
    val status: String,
    val rawData: String
)