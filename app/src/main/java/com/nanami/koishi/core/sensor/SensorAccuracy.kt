package com.nanami.koishi.core.sensor

import android.hardware.SensorManager

enum class SensorAccuracy {
    UNRELIABLE,
    LOW,
    MEDIUM,
    HIGH,
    UNKNOWN;

    companion object {
        fun fromPlatform(value: Int): SensorAccuracy = when (value) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> HIGH
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> MEDIUM
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> LOW
            SensorManager.SENSOR_STATUS_UNRELIABLE,
            SensorManager.SENSOR_STATUS_NO_CONTACT -> UNRELIABLE
            else -> UNKNOWN
        }

        fun lower(first: SensorAccuracy, second: SensorAccuracy): SensorAccuracy {
            if (first == UNKNOWN) return second
            if (second == UNKNOWN) return first
            return if (first.ordinal <= second.ordinal) first else second
        }
    }
}
