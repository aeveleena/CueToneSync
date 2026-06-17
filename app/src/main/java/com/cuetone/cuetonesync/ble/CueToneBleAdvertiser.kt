package com.cuetone.cuetonesync.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import java.util.UUID

class CueToneBleAdvertiser(
    private val context: Context
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager.adapter

    private val advertiser
        get() = bluetoothAdapter?.bluetoothLeAdvertiser

    private val serviceUuid =
        ParcelUuid(UUID.fromString("0000c710-0000-1000-8000-00805f9b34fb"))

    private val handler = Handler(Looper.getMainLooper())

    private var advertisingSet: AdvertisingSet? = null
    private var advertisingSetCallback: AdvertisingSetCallback? = null

    private val advertiseWindowMs = 900L
    private val pauseBetweenPacketsMs = 150L

    fun sendPacket(
        packet: ByteArray,
        onResult: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                onResult("Нет разрешения BLUETOOTH_ADVERTISE")
                return
            }
        }

        val adapter = bluetoothAdapter

        if (adapter == null || !adapter.isEnabled) {
            onResult("Bluetooth выключен или недоступен")
            return
        }

        val bleAdvertiser = advertiser

        if (bleAdvertiser == null) {
            onResult("BLE advertising не поддерживается на этом устройстве")
            return
        }

        if (!adapter.isLeExtendedAdvertisingSupported) {
            onResult("Extended advertising не поддерживается этим смартфоном")
            return
        }

        val maxDataLength = adapter.leMaximumAdvertisingDataLength

        if (packet.size > maxDataLength) {
            onResult("Пакет слишком большой: ${packet.size} байт, максимум $maxDataLength")
            return
        }

        stopAdvertising()

        val parameters = AdvertisingSetParameters.Builder()
            .setLegacyMode(false)
            .setConnectable(false)
            .setScannable(false)
            .setInterval(AdvertisingSetParameters.INTERVAL_LOW)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(serviceUuid)
            .addManufacturerData(0xFFFF, packet)
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()

        val callback = object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStarted(
                advertisingSet: AdvertisingSet?,
                txPower: Int,
                status: Int
            ) {
                if (status == ADVERTISE_SUCCESS) {
                    this@CueToneBleAdvertiser.advertisingSet = advertisingSet
                    onResult("Extended BLE-пакет отправляется")
                } else {
                    onResult("Ошибка extended advertising: $status")
                }
            }

            override fun onAdvertisingSetStopped(
                advertisingSet: AdvertisingSet?
            ) {
                this@CueToneBleAdvertiser.advertisingSet = null
            }
        }

        advertisingSetCallback = callback

        try {
            bleAdvertiser.startAdvertisingSet(
                parameters,
                data,
                null,
                null,
                null,
                0,
                0,
                callback
            )
        } catch (exception: SecurityException) {
            onResult("Нет разрешения BLUETOOTH_ADVERTISE")
        } catch (exception: IllegalArgumentException) {
            onResult("Ошибка запуска extended advertising: ${exception.message}")
        }
    }

    fun sendPacketsSequentially(
        packets: List<ByteArray>,
        onPacketResult: (Int, ByteArray, String) -> Unit
    ) {
        if (packets.isEmpty()) {
            return
        }

        var currentIndex = 0

        fun sendNextPacket() {
            if (currentIndex >= packets.size) {
                stopAdvertising()
                return
            }

            val packetIndex = currentIndex
            val packet = packets[packetIndex]

            sendPacket(packet) { status ->
                onPacketResult(packetIndex, packet, status)

                handler.postDelayed({
                    stopAdvertising()

                    currentIndex++

                    handler.postDelayed({
                        sendNextPacket()
                    }, pauseBetweenPacketsMs)

                }, advertiseWindowMs)
            }
        }

        sendNextPacket()
    }

    fun stopAdvertising() {
        handler.removeCallbacksAndMessages(null)

        val bleAdvertiser = advertiser
        val callback = advertisingSetCallback

        if (bleAdvertiser != null && callback != null) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                ) == PackageManager.PERMISSION_GRANTED

                if (!granted) {
                    advertisingSet = null
                    advertisingSetCallback = null
                    return
                }
            }

            try {
                bleAdvertiser.stopAdvertisingSet(callback)
            } catch (exception: SecurityException) {
                exception.printStackTrace()
            }
        }

        advertisingSet = null
        advertisingSetCallback = null
    }
}