package com.cuetone.cuetonesync.data

import com.cuetone.cuetonesync.model.DeviceChannel
import com.cuetone.cuetonesync.model.DeviceRole
import com.cuetone.cuetonesync.model.TimecodeDevice

object MockDevices {
    val availableDevices = listOf(
        TimecodeDevice(
            serialNumber = "C-001",
            id = 3,
            name = "CueTone Slate",
            type = "Clapper",
            role = DeviceRole.SLAVE,
            batteryLevel = 92,
            isRecording = false,
            frameOffset = 12,
            channel = DeviceChannel.A
        )
    )
}
