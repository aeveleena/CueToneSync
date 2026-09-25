package com.cuetone.cuetonesync.data

import android.os.SystemClock
import androidx.compose.runtime.mutableStateListOf
import com.cuetone.cuetonesync.model.TimecodeDevice

object TimecodeRepository {

    private val appStartTime = SystemClock.elapsedRealtime()

    fun getMasterFrame(frameRate: Int = 25): Int {
        val elapsedMs = SystemClock.elapsedRealtime() - appStartTime
        return ((elapsedMs / 1000.0) * frameRate).toInt()
    }

    fun getDeviceFrame(device: TimecodeDevice): Int {
        return getMasterFrame(device.fps.frameRate) + device.frameOffset
    }

    fun offsetForTargetFrame(targetFrame: Int, frameRate: Int = 25): Int {
        return targetFrame - getMasterFrame(frameRate)
    }
}
