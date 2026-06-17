package com.cuetone.cuetonesync.ble

import com.cuetone.cuetonesync.ble.model.GeneratorPacket
import com.cuetone.cuetonesync.ble.model.ClapperPacket
import com.cuetone.cuetonesync.ble.model.SyncPacket
import com.cuetone.cuetonesync.ble.model.SlateRecordPacket

// отвечает за преобразование моделей BLE-пакетов в массив байтов
// который затем передаётся через BLE advertising
object CueTonePacketSerializer {

    // Сигнатура протокола CueTone: символы "C" и "T"
    // По ней принимающая сторона может определить, что пакет относится к системе CueTone
    private const val CT_C: Byte = 0x43
    private const val CT_T: Byte = 0x54

    // Версия протокола и номер пакета состояния генератора
    private const val PROTOCOL_VERSION: Byte = 0x01
    private const val PACKET_GENERATOR_STATE: Byte = 0x01

    // Сериализация пакета №1 — состояния генератора таймкода
    fun serializeGeneratorPacket(
        packet: GeneratorPacket
    ): ByteArray {
        val bytes = mutableListOf<Byte>()

        // Заголовок пакета: сигнатура, версия протокола и номер пакета
        bytes.add(CT_C)
        bytes.add(CT_T)
        bytes.add(PROTOCOL_VERSION)
        bytes.add(PACKET_GENERATOR_STATE)

        // Серийный номер устройства-получателя
        // Используется устройством для проверки, предназначен ли пакет именно ему
        val serialBytes = packet.serialNumber
            .take(12)
            .encodeToByteArray()

        bytes.add(serialBytes.size.toByte())
        bytes.addAll(serialBytes.toList())

        // Основные параметры состояния генератора
        bytes.add(packet.syncMode.toByte())
        bytes.add(if (packet.isMaster) 1 else 0)
        bytes.add(packet.channel.code.toByte())
        bytes.add(packet.frameRate.toByte())

        // signalLevel: 0 = Audio-TC, 1 = LTC
        bytes.add(if (packet.signalLevel) 1 else 0)

        bytes.add(packet.displayBrightness.toByte())

        // Время автоблокировки передаётся двумя байтами,
        // так как значение до 600 секунд не помещается в один байт
        bytes.add(((packet.timeSleep shr 8) and 0xFF).toByte())
        bytes.add((packet.timeSleep and 0xFF).toByte())

        // Текущее значение таймкода
        bytes.add(packet.hours.toByte())
        bytes.add(packet.minutes.toByte())
        bytes.add(packet.seconds.toByte())
        bytes.add(packet.frames.toByte())

        // Флаги таймкода. Пока используются значения по умолчанию
        bytes.add(0x00) // DF
        bytes.add(0x00) // CF

        // User Bits отображаются в интерфейсе как xx:xx:xx:xx,
        // но в пакет передаются как 8 цифр, упакованных в 4 байта
        val userBits = packet.userBits
            .filter { it.isDigit() }
            .padEnd(8, '0')
            .take(8)

        userBits.chunked(2).forEach { pair ->
            val highNibble = pair[0].digitToInt()
            val lowNibble = pair[1].digitToInt()
            val packedByte = (highNibble shl 4) or lowNibble

            bytes.add(packedByte.toByte())
        }

        // Пользовательское имя устройства
        val nameBytes = packet.deviceName
            .take(24)
            .encodeToByteArray()

        bytes.add(nameBytes.size.toByte())
        bytes.addAll(nameBytes.toList())

        return bytes.toByteArray()
    }

    private const val PACKET_CLAPPER_STATE: Byte = 0x02

    // Сериализация пакета №2 — состояния электронной кинохлопушки
    fun serializeClapperPacket(
        packet: ClapperPacket
    ): ByteArray {
        val bytes = mutableListOf<Byte>()

        // Заголовок пакета.
        bytes.add(CT_C)
        bytes.add(CT_T)
        bytes.add(PROTOCOL_VERSION)
        bytes.add(PACKET_CLAPPER_STATE)

        // Серийный номер хлопушки-получателя
        val serialBytes = packet.serialNumber
            .take(12)
            .encodeToByteArray()

        bytes.add(serialBytes.size.toByte())
        bytes.addAll(serialBytes.toList())

        // Основные параметры состояния хлопушки
        bytes.add(packet.syncMode.toByte())
        bytes.add(if (packet.isMaster) 1 else 0)
        bytes.add(packet.channel.code.toByte())
        bytes.add(packet.frameRate.toByte())

        // signalLevel: 0 = Audio-TC, 1 = LTC
        bytes.add(if (packet.signalLevel) 1 else 0)

        // Параметры GPS используются только для электронной кинохлопушки
        bytes.add(if (packet.gps) 1 else 0)
        bytes.add(if (packet.gpsSync) 1 else 0)

        bytes.add(packet.displayBrightness.toByte())

        // Текущее значение таймкода
        bytes.add(packet.hours.toByte())
        bytes.add(packet.minutes.toByte())
        bytes.add(packet.seconds.toByte())
        bytes.add(packet.frames.toByte())

        // Флаги таймкода. Пока используются значения по умолчанию
        bytes.add(0x00) // DF
        bytes.add(0x00) // CF

        // User Bits упаковываются аналогично пакету генератора
        val userBits = packet.userBits
            .filter { it.isDigit() }
            .padEnd(8, '0')
            .take(8)

        userBits.chunked(2).forEach { pair ->
            val highNibble = pair[0].digitToInt()
            val lowNibble = pair[1].digitToInt()
            val packedByte = (highNibble shl 4) or lowNibble

            bytes.add(packedByte.toByte())
        }

        // Пользовательское имя хлопушки
        val nameBytes = packet.deviceName
            .take(24)
            .encodeToByteArray()

        bytes.add(nameBytes.size.toByte())
        bytes.addAll(nameBytes.toList())

        return bytes.toByteArray()
    }

    private const val PACKET_SYNC: Byte = 0x03

    // Сериализация пакета №3 — синхронизации устройств в выбранном канале
    fun serializeSyncPacket(
        packet: SyncPacket
    ): ByteArray {
        val bytes = mutableListOf<Byte>()

        // Заголовок пакета
        bytes.add(CT_C)
        bytes.add(CT_T)
        bytes.add(PROTOCOL_VERSION)
        bytes.add(PACKET_SYNC)

        // Канал синхронизации кодируется одним байтом
        bytes.add(
            when (packet.channel.name) {
                "A" -> 1
                "B" -> 2
                "C" -> 3
                "D" -> 4
                else -> 0
            }.toByte()
        )

        // Идентификатор устройства-мастера передаётся как uint16 в little-endian
        bytes.add((packet.masterDeviceId and 0xFF).toByte())
        bytes.add(((packet.masterDeviceId shr 8) and 0xFF).toByte())

        // Массив устройств канала имеет фиксированный размер 100 элементов
        // Если устройств меньше, оставшиеся позиции заполняются нулями
        val paddedDeviceIds = packet.deviceIds
            .take(100)
            .let { ids ->
                ids + List(100 - ids.size) { 0 }
            }

        paddedDeviceIds.forEach { id ->
            bytes.add((id and 0xFF).toByte())
            bytes.add(((id shr 8) and 0xFF).toByte())
        }

        // Частота кадров и целевой таймкод для синхронизации
        bytes.add(packet.frameRate.toByte())
        bytes.add(packet.hours.toByte())
        bytes.add(packet.minutes.toByte())
        bytes.add(packet.seconds.toByte())
        bytes.add(packet.frames.toByte())

        // Флаги DF и CF
        bytes.add(if (packet.df) 1 else 0)
        bytes.add(if (packet.cf) 1 else 0)

        // User Bits, передаваемые при ручной синхронизации
        val userBits = packet.userBits
            .filter { it.isDigit() }
            .padEnd(8, '0')
            .take(8)

        userBits.chunked(2).forEach { pair ->
            val highNibble = pair[0].digitToInt()
            val lowNibble = pair[1].digitToInt()
            val packedByte = (highNibble shl 4) or lowNibble

            bytes.add(packedByte.toByte())
        }

        return bytes.toByteArray()
    }

    private const val PACKET_SLATE_ROW: Byte = 0x04

    // Сериализация пакета №4 — строки таблицы дублей
    fun serializeSlateRecordPacket(
        packet: SlateRecordPacket
    ): ByteArray {
        val bytes = mutableListOf<Byte>()

        // Заголовок пакета.
        bytes.add(CT_C)
        bytes.add(CT_T)
        bytes.add(PROTOCOL_VERSION)
        bytes.add(PACKET_SLATE_ROW)

        // Имя таблицы, к которой относится строка
        val tableNameBytes = packet.tableName
            .take(12)
            .encodeToByteArray()

        bytes.add(tableNameBytes.size.toByte())
        bytes.addAll(tableNameBytes.toList())

        // Номер строки таблицы как uint16 в little-endian
        bytes.add((packet.rowNumber and 0xFF).toByte())
        bytes.add(((packet.rowNumber shr 8) and 0xFF).toByte())

        // Таймкод момента хлопка
        bytes.add(packet.hours.toByte())
        bytes.add(packet.minutes.toByte())
        bytes.add(packet.seconds.toByte())
        bytes.add(packet.frames.toByte())

        // Данные сцены
        addFixedString(bytes, packet.episode, 8)
        addFixedString(bytes, packet.scene, 8)
        addFixedString(bytes, packet.shot, 8)
        addFixedString(bytes, packet.take, 8)

        // Метка удачного дубля
        bytes.add(if (packet.isGood) 1 else 0)

        // GPS-координаты передаются как Int32
        // Для сохранения точности значение умножается на 1_000_000
        val lat = (packet.latitude * 1_000_000).toInt()
        val lon = (packet.longitude * 1_000_000).toInt()

        addInt32(bytes, lat)
        addInt32(bytes, lon)

        // GPS-время момента хлопка
        addFixedString(bytes, packet.gpsTime, 8)

        return bytes.toByteArray()
    }

    // Добавляет строковое поле в пакет:
    // сначала длина строки, затем сами байты строки
    private fun addFixedString(
        bytes: MutableList<Byte>,
        value: String,
        maxLength: Int
    ) {
        val valueBytes = value
            .take(maxLength)
            .encodeToByteArray()

        bytes.add(valueBytes.size.toByte())
        bytes.addAll(valueBytes.toList())
    }

    // Добавляет Int32 в little-endian порядке
    private fun addInt32(
        bytes: MutableList<Byte>,
        value: Int
    ) {
        bytes.add((value and 0xFF).toByte())
        bytes.add(((value shr 8) and 0xFF).toByte())
        bytes.add(((value shr 16) and 0xFF).toByte())
        bytes.add(((value shr 24) and 0xFF).toByte())
    }
}