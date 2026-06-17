package com.cuetone.cuetonesync.ble

import com.cuetone.cuetonesync.ble.model.GeneratorPacket
import com.cuetone.cuetonesync.model.DeviceRole
import com.cuetone.cuetonesync.model.TimecodeDevice
import com.cuetone.cuetonesync.util.splitFrames
import com.cuetone.cuetonesync.ble.model.ClapperPacket
import com.cuetone.cuetonesync.ble.model.SyncPacket
import com.cuetone.cuetonesync.model.FpsOption
import com.cuetone.cuetonesync.ble.model.SlateRecordPacket
import com.cuetone.cuetonesync.model.SlateRecord

object PacketFactory {

    // Формирует пакет состояния генератора таймкода.
    // Используется при изменении настроек генератора, сбросе таймкода или изменении его роли в синхронизации
    fun buildGeneratorPacket(
        device: TimecodeDevice,
        currentFrame: Int
    ): ByteArray {
        // Переводим общее количество кадров в часы, минуты, секунды и кадры
        val tc = splitFrames(
            totalFrames = currentFrame,
            frameRate = device.fps.frameRate
        )

        // Собираем модель пакета генератора на основе текущего состояния устройства
        val packet = GeneratorPacket(
            serialNumber = device.serialNumber,
            deviceName = device.name,

            syncMode = 1,

            isMaster = device.role == DeviceRole.MASTER,
            channel = device.channel.name.first(),
            frameRate = device.fps.frameRate,

            hours = tc.hours,
            minutes = tc.minutes,
            seconds = tc.seconds,
            frames = tc.frames,

            // signalLevel: false = Audio-TC, true = LTC
            signalLevel = device.outputVolume == "LTC",

            displayBrightness = device.displayBrightness,

            // Автоблокировка хранится в секундах
            timeSleep = device.autoLock.seconds,

            userBits = device.userBit
        )

        // Преобразуем модель пакета генератора в массив байтов
        return CueTonePacketSerializer.serializeGeneratorPacket(packet)
    }

    // Формирует пакет состояния электронной кинохлопушки.
    // Похож на пакет генератора, но содержит параметры GPS/GPS Sync и не содержит параметр автоблокировки
    fun buildClapperPacket(
        device: TimecodeDevice,
        currentFrame: Int
    ): ByteArray {
        val tc = splitFrames(
            totalFrames = currentFrame,
            frameRate = device.fps.frameRate
        )

        // Собираем модель пакета хлопушки
        val packet = ClapperPacket(
            serialNumber = device.serialNumber,
            deviceName = device.name,

            syncMode = 1,

            isMaster = device.role == DeviceRole.MASTER,
            channel = device.channel.name.first(),
            frameRate = device.fps.frameRate,

            hours = tc.hours,
            minutes = tc.minutes,
            seconds = tc.seconds,
            frames = tc.frames,

            signalLevel = device.outputVolume == "LTC",

            // Эти параметры актуальны только для электронной кинохлопушки
            gps = device.gpsEnabled,
            gpsSync = device.gpsSyncEnabled,

            displayBrightness = device.displayBrightness,
            userBits = device.userBit
        )

        // Преобразуем модель пакета хлопушки в массив байтов
        return CueTonePacketSerializer.serializeClapperPacket(packet)
    }

    // Универсальный метод для формирования пакета состояния устройства.
    // По типу устройства выбирает пакет №1 для генератора или пакет №2 для хлопушки
    fun buildDeviceStatePacket(
        device: TimecodeDevice,
        currentFrame: Int
    ): ByteArray {
        return if (device.type == "Clapper") {
            buildClapperPacket(
                device = device,
                currentFrame = currentFrame
            )
        } else {
            buildGeneratorPacket(
                device = device,
                currentFrame = currentFrame
            )
        }
    }

    // Формирует пакет синхронизации канала
    // Используется при Sync All и Manual TC
    fun buildSyncPacket(
        devicesInChannel: List<TimecodeDevice>,
        masterDevice: TimecodeDevice,
        targetFrame: Int,
        fps: FpsOption,
        userBit: String
    ): ByteArray {
        // Целевой таймкод переводится из кадров в структуру времени
        val tc = splitFrames(
            totalFrames = targetFrame,
            frameRate = fps.frameRate
        )

        val packet = SyncPacket(
            channel = masterDevice.channel,
            masterDeviceId = masterDevice.id,
            deviceIds = devicesInChannel.map { it.id },
            frameRate = fps.frameRate,
            hours = tc.hours,
            minutes = tc.minutes,
            seconds = tc.seconds,
            frames = tc.frames,
            userBits = userBit
        )

        // Преобразуем пакет синхронизации в массив байтов
        return CueTonePacketSerializer.serializeSyncPacket(packet)
    }

    // Формирует пакет строки таблицы дублей
    // Используется при сохранении изменённой строки таблицы хлопушки
    fun buildSlateRecordPacket(
        record: SlateRecord
    ): ByteArray {
        // Таймкод строки хранится как строка, поэтому перед отправкой разбираем его
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

        // Преобразуем строку таблицы дублей в массив байтов
        return CueTonePacketSerializer.serializeSlateRecordPacket(packet)
    }

    // Разбирает строковое представление таймкода в отдельные поля
    // Поддерживается формат вида хх:хх:хх.хх
    private fun parseTimecode(
        value: String
    ): ParsedTimecode {
        val parts = value
            .replace(".", ":")
            .split(":")

        return ParsedTimecode(
            hours = parts.getOrNull(0)?.toIntOrNull() ?: 0,
            minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            seconds = parts.getOrNull(2)?.toIntOrNull() ?: 0,
            frames = parts.getOrNull(3)?.toIntOrNull() ?: 0
        )
    }

    // Вспомогательная модель для хранения разобранного таймкода
    private data class ParsedTimecode(
        val hours: Int,
        val minutes: Int,
        val seconds: Int,
        val frames: Int
    )
}