package com.cuetone.cuetonesync.data

import androidx.compose.runtime.mutableStateListOf
import com.cuetone.cuetonesync.model.DeviceRole
import com.cuetone.cuetonesync.model.TimecodeDevice

object DevicesRepository {

    val devices = mutableStateListOf<TimecodeDevice>()

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