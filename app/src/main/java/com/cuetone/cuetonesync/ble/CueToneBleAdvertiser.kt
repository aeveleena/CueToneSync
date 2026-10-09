package com.cuetone.cuetonesync.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

    private val handler = Handler(Looper.getMainLooper())
    private var advertiseCallback: AdvertiseCallback? = null
    private var operationToken = 0

    private val burstDurationMs = 5_000L
    private val pauseBetweenFragmentsMs = 120L

    fun sendPacket(
        transmission: CueToneTransmission,
        durationMs: Long = burstDurationMs,
        onResult: (String) -> Unit
    ) {
        val validationError = validateEnvironment()
        if (validationError != null) {
            onResult(validationError)
            return
        }

        stopAdvertising()
        val token = operationToken
        sendPayloadAt(transmission, 0, token, durationMs, onResult)
    }

    fun sendPacketsSequentially(
        transmissions: List<CueToneTransmission>,
        onPacketResult: (Int, CueToneTransmission, String) -> Unit
    ) {
        if (transmissions.isEmpty()) return

        var index = 0

        fun sendNext() {
            if (index >= transmissions.size) return
            val currentIndex = index
            val transmission = transmissions[currentIndex]
            sendPacket(transmission) { status ->
                onPacketResult(currentIndex, transmission, status)
                index++
                handler.postDelayed(::sendNext, pauseBetweenFragmentsMs)
            }
        }

        sendNext()
    }

    fun stopAdvertising() {
        operationToken++
        handler.removeCallbacksAndMessages(null)
        stopCurrentBurst()
    }

    private fun sendPayloadAt(
        transmission: CueToneTransmission,
        index: Int,
        token: Int,
        durationMs: Long,
        onResult: (String) -> Unit
    ) {
        if (token != operationToken) return

        if (index >= transmission.advertisingPayloads.size) {
            onResult(
                "Legacy BLE: отправлено ${transmission.fragmentCount} " +
                    fragmentWord(transmission.fragmentCount)
            )
            return
        }

        val bleAdvertiser = advertiser ?: run {
            onResult("BLE advertising не поддерживается на этом устройстве")
            return
        }
        val payload = transmission.advertisingPayloads[index]
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(false)
            .setTimeout(0)
            .build()
        val data = AdvertiseData.Builder()
            .addServiceUuid(createServiceUuid(transmission.target, transmission.role))
            .addManufacturerData(CueToneLegacyProtocol.COMPANY_ID, payload)
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()

        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                if (token != operationToken) return
                handler.postDelayed({
                    if (token != operationToken) return@postDelayed
                    stopCurrentBurst()
                    handler.postDelayed(
                        { sendPayloadAt(transmission, index + 1, token, durationMs, onResult) },
                        pauseBetweenFragmentsMs
                    )
                }, durationMs)
            }

            override fun onStartFailure(errorCode: Int) {
                if (token != operationToken) return
                advertiseCallback = null
                onResult("Ошибка legacy advertising: ${errorName(errorCode)}")
            }
        }

        advertiseCallback = callback

        try {
            bleAdvertiser.startAdvertising(settings, data, callback)
        } catch (_: SecurityException) {
            advertiseCallback = null
            onResult("Нет разрешения BLUETOOTH_ADVERTISE")
        } catch (exception: IllegalArgumentException) {
            advertiseCallback = null
            onResult("Ошибка запуска legacy advertising: ${exception.message}")
        }
    }

    private fun validateEnvironment(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return "Нет разрешения BLUETOOTH_ADVERTISE"
        }

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            return "Bluetooth выключен или недоступен"
        }
        if (!adapter.isMultipleAdvertisementSupported) {
            return "Legacy BLE advertising не поддерживается смартфоном"
        }
        if (advertiser == null) {
            return "BLE advertising не поддерживается на этом устройстве"
        }
        return null
    }

    private fun stopCurrentBurst() {
        val callback = advertiseCallback ?: return
        advertiseCallback = null
        try {
            advertiser?.stopAdvertising(callback)
        } catch (_: SecurityException) {
            // Permission can be revoked while a burst is active.
        }
    }

    private fun createServiceUuid(target: Int, role: Int): ParcelUuid {
        val wireBytes = CueToneLegacyProtocol.serviceUuidWireBytes(target, role)
        val buffer = ByteBuffer.wrap(wireBytes).order(ByteOrder.LITTLE_ENDIAN)
        val leastSignificantBits = buffer.long
        val mostSignificantBits = buffer.long
        return ParcelUuid(UUID(mostSignificantBits, leastSignificantBits))
    }

    private fun fragmentWord(count: Int): String = when {
        count % 10 == 1 && count % 100 != 11 -> "фрагмент"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "фрагмента"
        else -> "фрагментов"
    }

    private fun errorName(errorCode: Int): String = when (errorCode) {
        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "данные больше 31 байта"
        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "нет свободного advertising set"
        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "реклама уже запущена"
        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "внутренняя ошибка Bluetooth"
        AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "функция не поддерживается"
        else -> "код $errorCode"
    }
}
