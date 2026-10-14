package com.nanami.koishi.core.sensor

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface

object SensorDisplay {
    fun currentRotation(context: Context): Int {
        val manager = context.getSystemService(DisplayManager::class.java)
        return manager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0
    }
}
