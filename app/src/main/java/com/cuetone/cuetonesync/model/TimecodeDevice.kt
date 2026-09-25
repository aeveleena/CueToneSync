package com.cuetone.cuetonesync.model

data class TimecodeDevice(
    val serialNumber: String,
    val id: Int,
    val name: String,
    val type: String,
    val role: DeviceRole,
    val batteryLevel: Int,
    val isRecording: Boolean,
    val frameOffset: Int,
    val channel: DeviceChannel = DeviceChannel.A,
    val autoLock: AutoLockOption = AutoLockOption.NEVER,
    val fps: FpsOption = FpsOption.FPS_25,
    val userBit: String = defaultUserBit(),
    val outputVolume: String = "LTC",
    val displayBrightness: Int = 80,
    val gpsEnabled: Boolean = false,
    val gpsSyncEnabled: Boolean = false
)

enum class DeviceRole {
    MASTER,
    SLAVE
}

enum class DeviceChannel {
    A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P
}

enum class AutoLockOption(
    val title: String,
    val seconds: Int
) {
    NEVER("Никогда", 0),
    SECONDS_30("Через 30 секунд", 30),
    MINUTE_1("Через 1 минуту", 60),
    MINUTES_5("Через 5 минут", 300),
    MINUTES_10("Через 10 минут", 600)
}

enum class FpsOption(val title: String, val frameRate: Int) {
    FPS_23_98("23.98F", 24),
    FPS_24("24.00F", 24),
    FPS_25("25.00F", 25),
    FPS_29_97("29.97F", 30),
    FPS_29_97_DF("29.97DF", 30),
    FPS_30("30.00F", 30),
    FPS_47_95("47.95F", 48),
    FPS_48("48.00F", 48),
    FPS_50("50.00F", 50),
    FPS_59_94("59.94F", 60),
    FPS_59_94_DF("59.94DF", 60),
    FPS_60("60.00F", 60)
}

fun defaultUserBit(): String {
    val date = java.time.LocalDate.now()
    return "%02d:%02d:%02d:00".format(
        date.dayOfMonth,
        date.monthValue,
        date.year % 100
    )
}
