package com.cuetone.cuetonesync.model

data class SlateRecord(
    val id: Int,
    val number: Int,
    val timecode: String,

    val episode: String,
    val scene: String,
    val shot: String,
    val take: String,

    val isGood: Boolean,

    val latitude: Double,
    val longitude: Double,
    val gpsTime: String,

    val note: String
)