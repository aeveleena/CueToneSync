package com.cuetone.cuetonesync.data

import com.cuetone.cuetonesync.model.DeviceChannel
import com.cuetone.cuetonesync.model.DeviceRole
import com.cuetone.cuetonesync.model.TimecodeDevice

object MockDevices {
    val availableDevices = listOf(
        TimecodeDevice(
            serialNumber = "G-001",
            id = 1,
            name = "CueTone Generator 01",
            type = "Generator",
            role = DeviceRole.SLAVE,
            batteryLevel = 87,
            isRecording = true,
            frameOffset = 0,
            channel = DeviceChannel.A
        ),
        TimecodeDevice(
            serialNumber = "G-002",
            id = 2,
            name = "CueTone Generator 02",
            type = "Generator",
            role = DeviceRole.SLAVE,
            batteryLevel = 64,
            isRecording = true,
            frameOffset = -7,
            channel = DeviceChannel.A
        ),
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
        ),
        TimecodeDevice(
            serialNumber = "G-003",
            id = 4,
            name = "CueTone Generator 03",
            type = "Generator",
            role = DeviceRole.SLAVE,
            batteryLevel = 74,
            isRecording = false,
            frameOffset = 35,
            channel = DeviceChannel.B
        )
    )
}