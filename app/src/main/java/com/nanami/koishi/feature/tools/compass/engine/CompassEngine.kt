package com.nanami.koishi.feature.tools.compass.engine

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

class CompassEngine(
    private val smoothingFactor: Double = 0.18
) {

    fun isSupported(context: Context): Boolean {
        val manager = context.getSystemService(SensorManager::class.java) ?: return false
        if (manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null) return true
        return manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null &&
            manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
    }

    fun frames(context: Context): Flow<CompassFrame> = callbackFlow {
        val manager = context.getSystemService(SensorManager::class.java)
        if (manager == null) {
            close(IllegalStateException("SensorManager unavailable"))
            return@callbackFlow
        }

        val rotationSensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val useRotationVector = rotationSensor != null
        val sensors = when {
            useRotationVector -> listOf(rotationSensor)
            accelerometer != null && magnetometer != null -> listOf(accelerometer, magnetometer)
            else -> emptyList()
        }

        if (sensors.isEmpty()) {
            close(IllegalStateException("Compass sensors unavailable"))
            return@callbackFlow
        }

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        val accuracyByType = mutableMapOf<Int, SensorAccuracy>()
        var gravity: FloatArray? = null
        var magneticField: FloatArray? = null
        var lastHeading: Double? = null
        var lastFrame: CompassFrame? = null

        fun combinedAccuracy(): SensorAccuracy = if (useRotationVector) {
            accuracyByType[Sensor.TYPE_ROTATION_VECTOR] ?: SensorAccuracy.UNKNOWN
        } else {
            SensorAccuracy.lower(
                accuracyByType[Sensor.TYPE_ACCELEROMETER] ?: SensorAccuracy.UNKNOWN,
                accuracyByType[Sensor.TYPE_MAGNETIC_FIELD] ?: SensorAccuracy.UNKNOWN
            )
        }

        fun sendFrame(azimuthRad: Double, pitchRad: Double, rollRad: Double) {
            val rawHeading = CompassMath.applyDisplayRotation(
                Math.toDegrees(azimuthRad),
                SensorDisplay.currentRotation(context)
            )
            val heading = CompassMath.smoothHeading(lastHeading, rawHeading, smoothingFactor)
            lastHeading = heading
            val frame = CompassFrame(
                headingDeg = heading,
                pitchDeg = Math.toDegrees(pitchRad),
                rollDeg = Math.toDegrees(rollRad),
                accuracy = combinedAccuracy()
            )
            lastFrame = frame
            trySend(frame)
        }

        fun updateFromVectors() {
            val gravityValues = gravity ?: return
            val magneticValues = magneticField ?: return
            if (!SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, magneticValues)) {
                return
            }
            SensorManager.getOrientation(rotationMatrix, orientation)
            sendFrame(
                azimuthRad = orientation[0].toDouble(),
                pitchRad = orientation[1].toDouble(),
                rollRad = orientation[2].toDouble()
            )
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        sendFrame(
                            azimuthRad = orientation[0].toDouble(),
                            pitchRad = orientation[1].toDouble(),
                            rollRad = orientation[2].toDouble()
                        )
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        gravity = event.values.copyOf()
                        updateFromVectors()
                    }

                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        magneticField = event.values.copyOf()
                        updateFromVectors()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor == null) return
                accuracyByType[sensor.type] = SensorAccuracy.fromPlatform(accuracy)
                lastFrame?.let { trySend(it.copy(accuracy = combinedAccuracy())) }
            }
        }

        var registered = 0
        sensors.forEach { sensor ->
            if (manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)) {
                registered += 1
            }
        }

        if (registered == 0) {
            close(IllegalStateException("Unable to register compass sensors"))
            return@callbackFlow
        }

        awaitClose {
            manager.unregisterListener(listener)
        }
    }.conflate()
}

