package com.cuetone.cuetonesync.ble

import com.cuetone.cuetonesync.ble.model.ClapperPacket
import com.cuetone.cuetonesync.ble.model.SlateRecordPacket

object CueTonePacketSerializer {
    private const val CT_C: Byte = 0x43
    private const val CT_T: Byte = 0x54
    private const val PROTOCOL_VERSION: Byte = 0x01

    fun serializeClapperPacket(packet: ClapperPacket): ByteArray {
        val bytes = mutableListOf(
            CT_C,
            CT_T,
            PROTOCOL_VERSION,
            CueToneLegacyProtocol.PACKET_CLAPPER.toByte()
        )

        addLengthString(bytes, packet.serialNumber, 12)
        bytes.add(packet.syncMode.toByte())
        bytes.add(if (packet.isMaster) 1 else 0)
        bytes.add(packet.channel.code.toByte())
        bytes.add(packet.frameRate.toByte())
        bytes.add(if (packet.signalLevel) 1 else 0)
        bytes.add(if (packet.gps) 1 else 0)
        bytes.add(if (packet.gpsSync) 1 else 0)
        bytes.add(packet.displayBrightness.toByte())
        bytes.add(packet.hours.toByte())
        bytes.add(packet.minutes.toByte())
        bytes.add(packet.seconds.toByte())
        bytes.add(packet.frames.toByte())
        bytes.add(0x00)
        bytes.add(0x00)
        addUserBits(bytes, packet.userBits)
        addLengthString(bytes, packet.deviceName, 24)

        return bytes.toByteArray()
    }

    fun serializeSlateRecordPacket(packet: SlateRecordPacket): ByteArray {
        val bytes = mutableListOf(
            CT_C,
            CT_T,
            PROTOCOL_VERSION,
            CueToneLegacyProtocol.PACKET_SLATE_RECORD.toByte()
        )

        addLengthString(bytes, packet.tableName, 12)
        bytes.add((packet.rowNumber and 0xFF).toByte())
        bytes.add(((packet.rowNumber shr 8) and 0xFF).toByte())
        bytes.add(packet.hours.toByte())
        bytes.add(packet.minutes.toByte())
        bytes.add(packet.seconds.toByte())
        bytes.add(packet.frames.toByte())
        addLengthString(bytes, packet.episode, 8)
        addLengthString(bytes, packet.scene, 8)
        addLengthString(bytes, packet.shot, 8)
        addLengthString(bytes, packet.take, 8)
        bytes.add(if (packet.isGood) 1 else 0)
        addInt32(bytes, (packet.latitude * 1_000_000).toInt())
        addInt32(bytes, (packet.longitude * 1_000_000).toInt())
        addLengthString(bytes, packet.gpsTime, 8)

        return bytes.toByteArray()
    }

    private fun addUserBits(bytes: MutableList<Byte>, value: String) {
        val digits = value.filter { it.isDigit() }.padEnd(8, '0').take(8)
        digits.chunked(2).forEach { pair ->
            bytes.add(((pair[0].digitToInt() shl 4) or pair[1].digitToInt()).toByte())
        }
    }

    private fun addLengthString(
        bytes: MutableList<Byte>,
        value: String,
        maxByteLength: Int
    ) {
        val valueBytes = utf8Prefix(value, maxByteLength)
        bytes.add(valueBytes.size.toByte())
        bytes.addAll(valueBytes.toList())
    }

    private fun utf8Prefix(value: String, maxByteLength: Int): ByteArray {
        val result = mutableListOf<Byte>()
        value.forEach { character ->
            val characterBytes = character.toString().encodeToByteArray()
            if (result.size + characterBytes.size > maxByteLength) {
                return result.toByteArray()
            }
            result.addAll(characterBytes.toList())
        }
        return result.toByteArray()
    }

    private fun addInt32(bytes: MutableList<Byte>, value: Int) {
        bytes.add((value and 0xFF).toByte())
        bytes.add(((value shr 8) and 0xFF).toByte())
        bytes.add(((value shr 16) and 0xFF).toByte())
        bytes.add(((value shr 24) and 0xFF).toByte())
    }
}
