package com.cuetone.cuetonesync.ble.model

data class SlateRecordPacket(
    val tableName: String,
    val rowNumber: Int,

    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val frames: Int,

    val episode: String,
    val scene: String,
    val shot: String,
    val take: String,

    val isGood: Boolean,

    val latitude: Double,
    val longitude: Double,
    val gpsTime: String
)