package com.cuetone.cuetonesync.ble.model

data class GeneratorPacket(
    val serialNumber: String,
    val deviceName: String,

    val syncMode: Int,
    val isMaster: Boolean,
    val channel: Char,
    val frameRate: Int,

    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val frames: Int,

    val signalLevel: Boolean,
    val displayBrightness: Int,
    val timeSleep: Int,

    val userBits: String
)