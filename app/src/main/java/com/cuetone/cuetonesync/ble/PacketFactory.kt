package com.cuetone.cuetonesync.ble

import com.cuetone.cuetonesync.ble.model.SlateRecordPacket
import com.cuetone.cuetonesync.model.DeviceRole
import com.cuetone.cuetonesync.model.FpsOption
import com.cuetone.cuetonesync.model.SlateRecord
import com.cuetone.cuetonesync.model.TimecodeDevice
import com.cuetone.cuetonesync.util.splitFrames

object PacketFactory {
    fun buildGeneratorPacket(
        device: TimecodeDevice,
        currentFrame: Int
    ): CueToneTransmission {
        val tc = splitFrames(currentFrame, device.fps.frameRate)
        val channel = device.channel.ordinal + 1
        val syncMode = if (device.role == DeviceRole.MASTER) 0 else 1
        val flags0 =
            (if (isDropFrame(device.fps)) 1 else 0) or
                (if (device.role == DeviceRole.MASTER) (1 shl 4) else 0) or
                (1 shl 5)
        val flags1 = fpsEnum(device.fps) and 0x0F
        val signalInfo =
            (if (device.outputVolume == "LTC") 1 else 0) or
                ((syncMode and 0x03) shl 3) or
                CueToneLegacyProtocol.channelSignalBits(channel)
        val channelAndBatteryTime = CueToneLegacyProtocol.channelBatteryBits(channel)

        val payload = byteArrayOf(
            flags0.toByte(),
            flags1.toByte(),
            device.batteryLevel.coerceIn(0, 100).toByte(),
            tc.hours.toByte(),
            tc.minutes.toByte(),
            tc.seconds.toByte(),
            tc.frames.toByte(),
            signalInfo.toByte(),
            channelAndBatteryTime.toByte()
        )

        return CueToneLegacyProtocol.directTransmission(
            target = CueToneLegacyProtocol.normalizeDeviceTarget(device.id),
            packetType = CueToneLegacyProtocol.PACKET_GENERATOR,
            payload = payload
        )
    }

    fun buildGeneratorNamePacket(
        deviceId: Int,
        name: String
    ): CueToneTransmission {
        val nameBytes = utf8Prefix(name.trim(), 6)
        require(nameBytes.isNotEmpty()) { "Generator name must not be empty" }
        val payload = byteArrayOf(
            CueToneLegacyProtocol.CONTROL_SET_DEVICE_NAME.toByte(),
            nameBytes.size.toByte()
        ) + nameBytes

        return CueToneLegacyProtocol.directTransmission(
            target = CueToneLegacyProtocol.normalizeDeviceTarget(deviceId),
            packetType = CueToneLegacyProtocol.PACKET_DEVICE_NAME,
            payload = payload,
            role = CueToneLegacyProtocol.ROLE_APP_CONTROL
        )
    }

    fun buildDeviceStatePacket(
        device: TimecodeDevice,
        currentFrame: Int
    ): CueToneTransmission = buildGeneratorPacket(device, currentFrame)

    fun buildClapperLogRequest(deviceId: Int): CueToneTransmission =
        CueToneLegacyProtocol.directTransmission(
            target = CueToneLegacyProtocol.normalizeDeviceTarget(deviceId),
            packetType = CueToneLegacyProtocol.PACKET_LOG_REQUEST,
            payload = byteArrayOf(CueToneLegacyProtocol.LOG_REQUEST_MAGIC.toByte())
        )

    @Suppress("UNUSED_PARAMETER")
    fun buildSyncPacket(
        devicesInChannel: List<TimecodeDevice>,
        masterDevice: TimecodeDevice,
        targetFrame: Int,
        fps: FpsOption,
        userBit: String,
        broadcast: Boolean = false
    ): CueToneTransmission {
        require(devicesInChannel.isNotEmpty()) { "Channel sync requires at least one device" }
        val tc = splitFrames(targetFrame, fps.frameRate)
        val channel = masterDevice.channel.ordinal + 1
        val masterId = CueToneLegacyProtocol.normalizeDeviceTarget(masterDevice.id)
        val flags0 =
            (fpsEnum(fps) and 0x0F) or
                (if (isDropFrame(fps)) (1 shl 4) else 0) or
                (1 shl 6)
        val payload = byteArrayOf(
            tc.hours.toByte(),
            tc.minutes.toByte(),
            tc.seconds.toByte(),
            tc.frames.toByte(),
            (masterId and 0xFF).toByte(),
            ((masterId shr 8) and 0xFF).toByte(),
            flags0.toByte(),
            (channel and 0x1F).toByte()
        )

        return CueToneLegacyProtocol.directTransmission(
            target = if (broadcast) {
                CueToneLegacyProtocol.TARGET_BROADCAST
            } else {
                CueToneLegacyProtocol.channelTarget(masterDevice.channel.ordinal)
            },
            packetType = CueToneLegacyProtocol.PACKET_SYNC,
            payload = payload
        )
    }

    fun buildSlateRecordPacket(
        record: SlateRecord,
        clapperDeviceId: Int
    ): CueToneTransmission {
        val timecode = parseTimecode(record.timecode)
        val packet = SlateRecordPacket(
            tableName = "SlateLog",
            rowNumber = record.number,
            hours = timecode.hours,
            minutes = timecode.minutes,
            seconds = timecode.seconds,
            frames = timecode.frames,
            episode = record.episode,
            scene = record.scene,
            shot = record.shot,
            take = record.take,
            isGood = record.isGood,
            latitude = record.latitude,
            longitude = record.longitude,
            gpsTime = record.gpsTime
        )
        val payload = CueTonePacketSerializer.serializeSlateRecordPacket(packet)

        return CueToneLegacyProtocol.fragmentedTransmission(
            target = CueToneLegacyProtocol.normalizeDeviceTarget(clapperDeviceId),
            packetType = CueToneLegacyProtocol.PACKET_SLATE_RECORD,
            payload = payload
        )
    }

    private fun fpsEnum(fps: FpsOption): Int = when (fps) {
        FpsOption.FPS_23_98 -> 0
        FpsOption.FPS_24 -> 1
        FpsOption.FPS_25 -> 2
        FpsOption.FPS_29_97,
        FpsOption.FPS_29_97_DF -> 3
        FpsOption.FPS_30 -> 4
        FpsOption.FPS_47_95 -> 5
        FpsOption.FPS_48 -> 6
        FpsOption.FPS_50 -> 7
        FpsOption.FPS_59_94,
        FpsOption.FPS_59_94_DF -> 8
        FpsOption.FPS_60 -> 9
    }

    private fun isDropFrame(fps: FpsOption): Boolean =
        fps == FpsOption.FPS_29_97_DF || fps == FpsOption.FPS_59_94_DF

    private fun parseTimecode(value: String): ParsedTimecode {
        val parts = value.replace(".", ":").split(":")
        return ParsedTimecode(
            hours = parts.getOrNull(0)?.toIntOrNull() ?: 0,
            minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            seconds = parts.getOrNull(2)?.toIntOrNull() ?: 0,
            frames = parts.getOrNull(3)?.toIntOrNull() ?: 0
        )
    }

    private fun utf8Prefix(value: String, maxByteLength: Int): ByteArray {
        val result = mutableListOf<Byte>()
        value.forEach { character ->
            val bytes = character.toString().encodeToByteArray()
            if (result.size + bytes.size > maxByteLength) return result.toByteArray()
            result.addAll(bytes.toList())
        }
        return result.toByteArray()
    }

    private data class ParsedTimecode(
        val hours: Int,
        val minutes: Int,
        val seconds: Int,
        val frames: Int
    )
}
