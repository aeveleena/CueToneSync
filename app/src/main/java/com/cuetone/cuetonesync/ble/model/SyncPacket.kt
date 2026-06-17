package com.cuetone.cuetonesync.ble.model

import com.cuetone.cuetonesync.model.DeviceChannel

data class SyncPacket(
    val channel: DeviceChannel,
    val masterDeviceId: Int,
    val deviceIds: List<Int>,
    val frameRate: Int,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val frames: Int,
    val df: Boolean = false,
    val cf: Boolean = false,
    val userBits: String
)