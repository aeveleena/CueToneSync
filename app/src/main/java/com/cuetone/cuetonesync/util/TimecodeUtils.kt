package com.cuetone.cuetonesync.util

const val FRAME_RATE = 25
const val FRAME_DURATION_MS = 1000L / FRAME_RATE

data class SplitTimecode(
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val frames: Int
)

fun formatTimecode(
    totalFrames: Int,
    frameRate: Int = FRAME_RATE
): String {
    val safeFrames = totalFrames.coerceAtLeast(0)

    val frames = safeFrames % frameRate
    val totalSeconds = safeFrames / frameRate

    val seconds = totalSeconds % 60
    val totalMinutes = totalSeconds / 60

    val minutes = totalMinutes % 60
    val hours = totalMinutes / 60

    return "%02d:%02d:%02d.%02d".format(
        hours,
        minutes,
        seconds,
        frames
    )
}

fun splitFrames(
    totalFrames: Int,
    frameRate: Int
): SplitTimecode {
    val safeFrames = totalFrames.coerceAtLeast(0)

    val hours = safeFrames / (3600 * frameRate)
    val minutes = (safeFrames % (3600 * frameRate)) / (60 * frameRate)
    val seconds = (safeFrames % (60 * frameRate)) / frameRate
    val frames = safeFrames % frameRate

    return SplitTimecode(hours, minutes, seconds, frames)
}

fun currentFrameFromOffset(offset: Int): Int {
    val elapsedFrames =
        (System.currentTimeMillis() / FRAME_DURATION_MS).toInt()

    return elapsedFrames + offset
}