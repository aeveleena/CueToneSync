package com.cuetone.cuetonesync.data

import androidx.compose.runtime.mutableStateListOf
import com.cuetone.cuetonesync.model.SlateRecord

object SlateRepository {

    val records = mutableStateListOf(
        SlateRecord(
            id = 1,
            number = 1,
            timecode = "00:01:12.08",
            episode = "01",
            scene = "1A",
            shot = "03",
            take = "1",
            isGood = true,
            latitude = 59.9343,
            longitude = 30.3351,
            gpsTime = "12:45:10",
            note = ""
        ),
        SlateRecord(
            id = 2,
            number = 2,
            timecode = "00:03:45.14",
            episode = "01",
            scene = "1A",
            shot = "03",
            take = "2",
            isGood = false,
            latitude = 59.9343,
            longitude = 30.3351,
            gpsTime = "12:47:32",
            note = "Повтор из-за шума"
        ),
        SlateRecord(
            id = 3,
            number = 3,
            timecode = "00:06:18.22",
            episode = "01",
            scene = "1B",
            shot = "04",
            take = "1",
            isGood = false,
            latitude = 59.9345,
            longitude = 30.3354,
            gpsTime = "12:52:08",
            note = "Нужно проверить фокус"
        ),
        SlateRecord(
            id = 4,
            number = 4,
            timecode = "00:08:03.06",
            episode = "01",
            scene = "1B",
            shot = "04",
            take = "2",
            isGood = true,
            latitude = 59.9345,
            longitude = 30.3354,
            gpsTime = "12:55:41",
            note = "Хороший дубль"
        ),
        SlateRecord(
            id = 5,
            number = 5,
            timecode = "00:11:27.19",
            episode = "02",
            scene = "2A",
            shot = "01",
            take = "1",
            isGood = false,
            latitude = 59.9348,
            longitude = 30.3360,
            gpsTime = "13:04:16",
            note = "Актёр начал раньше хлопка"
        ),
        SlateRecord(
            id = 6,
            number = 6,
            timecode = "00:14:09.11",
            episode = "02",
            scene = "2A",
            shot = "01",
            take = "2",
            isGood = true,
            latitude = 59.9348,
            longitude = 30.3360,
            gpsTime = "13:08:33",
            note = "Можно использовать"
        )
    )

    fun getRecordById(id: Int): SlateRecord? {
        return records.firstOrNull { it.id == id }
    }

    fun updateRecord(updated: SlateRecord) {
        val index = records.indexOfFirst { it.id == updated.id }

        if (index != -1) {
            records[index] = updated
        }
    }
}