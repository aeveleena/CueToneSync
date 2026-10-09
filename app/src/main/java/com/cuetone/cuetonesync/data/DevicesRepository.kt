package com.cuetone.cuetonesync.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.cuetone.cuetonesync.ble.ClapperLogBeacon
import com.cuetone.cuetonesync.ble.CueToneLegacyProtocol
import com.cuetone.cuetonesync.model.DeviceRole
import com.cuetone.cuetonesync.model.PacketLogEntry
import com.cuetone.cuetonesync.model.TimecodeDevice

object DevicesRepository {

    private const val DEVICE_OFFLINE_TIMEOUT_MS = 10_000L

    val devices = mutableStateListOf<TimecodeDevice>().apply {
        addAll(MockDevices.availableDevices)
    }

    val discoveredDevices = mutableStateListOf<TimecodeDevice>()
    private val lastSeenAt = mutableStateMapOf<Int, Long>()
    private val clapperIds = mutableStateMapOf<Int, Boolean>()
    val latestClapperBeacons = mutableStateMapOf<Int, ClapperLogBeacon>()

    fun onDeviceDiscovered(device: TimecodeDevice) {
        val resolvedDevice = if (clapperIds[device.id] == true) {
            device.copy(
                serialNumber = "CLP-%04X".format(device.id),
                type = "Clapper"
            )
        } else {
            device
        }

        lastSeenAt[resolvedDevice.id] = System.currentTimeMillis()

        val addedIndex = devices.indexOfFirst { it.id == resolvedDevice.id }
        if (addedIndex >= 0) {
            devices[addedIndex] = resolvedDevice
            return
        }

        val discoveredIndex = discoveredDevices.indexOfFirst { it.id == resolvedDevice.id }
        if (discoveredIndex >= 0) {
            discoveredDevices[discoveredIndex] = resolvedDevice
        } else {
            discoveredDevices.add(resolvedDevice)
        }
    }

    fun onClapperBeacon(beacon: ClapperLogBeacon): Boolean {
        clapperIds[beacon.deviceId] = true
        lastSeenAt[beacon.deviceId] = System.currentTimeMillis()

        fun asClapper(device: TimecodeDevice): TimecodeDevice = device.copy(
            serialNumber = "CLP-%04X".format(device.id),
            type = "Clapper"
        )

        val addedIndex = devices.indexOfFirst { it.id == beacon.deviceId }
        if (addedIndex >= 0) devices[addedIndex] = asClapper(devices[addedIndex])

        val discoveredIndex = discoveredDevices.indexOfFirst { it.id == beacon.deviceId }
        if (discoveredIndex >= 0) {
            discoveredDevices[discoveredIndex] = asClapper(discoveredDevices[discoveredIndex])
        }

        val previousSequence = latestClapperBeacons[beacon.deviceId]?.sequence
        if (previousSequence != null && beacon.sequence <= previousSequence) return false
        latestClapperBeacons[beacon.deviceId] = beacon

        val flags = buildList {
            if (beacon.isClap) add("CLAP")
            if (beacon.isGoodMark) add("GOOD_MARK")
            if (beacon.gpsValid) add("GPS")
        }.joinToString().ifBlank { "none" }

        PacketLogRepository.addLog(
            PacketLogEntry(
                timestamp = System.currentTimeMillis(),
                direction = "IN",
                deviceName = "Clapper %04X".format(beacon.deviceId),
                packetType = "Log beacon 0xC2 / seq ${beacon.sequence}",
                status = "$flags / camera ${'A' + beacon.cameraIndex}",
                rawData = "%08X %02X %02X".format(
                    beacon.sequence,
                    beacon.timecodeSeconds,
                    beacon.timecodeFrames
                )
            )
        )
        return true
    }

    fun addDiscoveredDevice(device: TimecodeDevice) {
        if (devices.none { it.id == device.id }) {
            devices.add(device)
        }
        discoveredDevices.removeAll { it.id == device.id }
    }

    fun isDeviceOnline(device: TimecodeDevice, now: Long = System.currentTimeMillis()): Boolean {
        if (device.id < CueToneLegacyProtocol.DEVICE_ID_MIN) return true
        val lastSeen = lastSeenAt[device.id] ?: return false
        return now - lastSeen <= DEVICE_OFFLINE_TIMEOUT_MS
    }

    fun updateDevice(updatedDevice: TimecodeDevice) {
        val index = devices.indexOfFirst { it.id == updatedDevice.id }

        if (index != -1) {
            devices[index] = updatedDevice
        }
    }

    fun getDeviceById(id: Int): TimecodeDevice? {
        return devices.firstOrNull { it.id == id }
            ?: MockDevices.availableDevices.firstOrNull { it.id == id }
    }

    fun getClapperBySerialOrFirst(serialNumber: String?): TimecodeDevice? {
        val allDevices = devices + MockDevices.availableDevices
        return allDevices.firstOrNull {
            it.type == "Clapper" && it.serialNumber == serialNumber
        } ?: devices.firstOrNull {
            it.type == "Clapper" && it.id >= CueToneLegacyProtocol.DEVICE_ID_MIN
        } ?: allDevices.firstOrNull { it.type == "Clapper" }
    }

    fun setMasterForChannel(masterDevice: TimecodeDevice) {
        devices.replaceAll { device ->
            if (device.channel == masterDevice.channel) {
                if (device.id == masterDevice.id) {
                    device.copy(role = DeviceRole.MASTER)
                } else {
                    device.copy(role = DeviceRole.SLAVE)
                }
            } else {
                device
            }
        }
    }

    fun updateDeviceWithMasterRule(updatedDevice: TimecodeDevice) {
        updateDevice(updatedDevice)

        if (updatedDevice.role == DeviceRole.MASTER) {
            setMasterForChannel(updatedDevice)
        }
    }
}
