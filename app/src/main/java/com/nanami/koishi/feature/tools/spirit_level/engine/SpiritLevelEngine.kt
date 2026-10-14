package com.nanami.koishi.feature.tools.spirit_level.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.nanami.koishi.core.sensor.SensorAccuracy
import com.nanami.koishi.core.sensor.SensorDisplay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class SpiritLevelEngine(
    private val smoothingFactor: Double = 0.18
) {

    fun isSupported(context: Context): Boolean {
        val manager = context.getSystemService(SensorManager::class.java) ?: return false
        return manager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null ||
            manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }

    fun frames(context: Context): Flow<SpiritLevelFrame> = callbackFlow {
        val manager = context.getSystemService(SensorManager::class.java)
        if (manager == null) {
            close(IllegalStateException("SensorManager unavailable"))
            return@callbackFlow
        }

        val sensor = manager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) {
            close(IllegalStateException("Level sensor unavailable"))
            return@callbackFlow
        }

        var smoothed: FloatArray? = null
        var accuracy = SensorAccuracy.UNKNOWN

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != sensor.type) return
                val values = SpiritLevelMath.smoothVector(smoothed, event.values, smoothingFactor)
                smoothed = values
                val frame = SpiritLevelMath.fromGravity(
                    values = values,
                    displayRotation = SensorDisplay.currentRotation(context)
                )
                trySend(frame.copy(accuracy = accuracy))
            }

            override fun onAccuracyChanged(changedSensor: Sensor?, changedAccuracy: Int) {
                if (changedSensor?.type == sensor.type) {
                    accuracy = SensorAccuracy.fromPlatform(changedAccuracy)
                }
            }
        }

        if (!manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)) {
            close(IllegalStateException("Unable to register level sensor"))
            return@callbackFlow
        }

        awaitClose {
            manager.unregisterListener(listener)
        }
    }.conflate()
}
