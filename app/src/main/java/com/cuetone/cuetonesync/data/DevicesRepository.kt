package com.cuetone.cuetonesync.data

import androidx.compose.runtime.mutableStateListOf
import com.cuetone.cuetonesync.model.DeviceRole
import com.cuetone.cuetonesync.model.TimecodeDevice

object DevicesRepository {

    val devices = mutableStateListOf<TimecodeDevice>().apply {
        addAll(MockDevices.availableDevices)
    }

    val discoveredGenerators = mutableStateListOf<TimecodeDevice>()

    fun onGeneratorDiscovered(device: TimecodeDevice) {
        if (device.type != "Generator") return

        val addedIndex = devices.indexOfFirst { it.id == device.id }
        if (addedIndex >= 0) {
            devices[addedIndex] = device
            return
        }

        val discoveredIndex = discoveredGenerators.indexOfFirst { it.id == device.id }
        if (discoveredIndex >= 0) {
            discoveredGenerators[discoveredIndex] = device
        } else {
            discoveredGenerators.add(device)
        }
    }

    fun addDiscoveredGenerator(device: TimecodeDevice) {
        if (devices.none { it.id == device.id }) {
            devices.add(device)
        }
        discoveredGenerators.removeAll { it.id == device.id }
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
