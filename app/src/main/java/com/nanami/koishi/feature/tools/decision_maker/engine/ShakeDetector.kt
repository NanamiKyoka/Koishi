package com.nanami.koishi.feature.tools.decision_maker.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock

/**
 * 加速度平方和阈值摇一摇检测，需与页面生命周期成对启停以避免后台持续耗电
 */
class ShakeDetector(
    private val thresholdG: Float = 2.4f,
    private val minIntervalMs: Long = 110L,
    private val onShakeTick: () -> Unit
) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var lastTickAt = 0L
    private val thresholdSquared = (thresholdG * SensorManager.GRAVITY_EARTH).let { it * it }

    val isRunning: Boolean
        get() = sensorManager != null

    fun start(context: Context) {
        if (sensorManager != null) return
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        if (manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)) {
            sensorManager = manager
            lastTickAt = 0L
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        if (x * x + y * y + z * z < thresholdSquared) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastTickAt < minIntervalMs) return
        lastTickAt = now
        onShakeTick()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
