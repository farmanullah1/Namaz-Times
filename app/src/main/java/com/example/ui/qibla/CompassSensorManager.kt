package com.example.ui.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class CompassSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _azimuthHeading = MutableStateFlow(0f)
    val azimuthHeading: StateFlow<Float> = _azimuthHeading.asStateFlow()

    private val _hasSensor = MutableStateFlow(rotationSensor != null || (accelerometer != null && magnetometer != null))
    val hasSensor: StateFlow<Boolean> = _hasSensor.asStateFlow()

    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null
    private var currentHeading: Float = 0f

    fun startListening() {
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            magnetometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    fun setManualHeading(heading: Float) {
        _azimuthHeading.value = (heading % 360f + 360f) % 360f
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthRad = orientation[0]
            val azimuthDeg = (Math.toDegrees(azimuthRad.toDouble()).toFloat() + 360f) % 360f
            updateSmoothHeading(azimuthDeg)
        } else {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                gravity = lowPassFilter(event.values.clone(), gravity)
            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                geomagnetic = lowPassFilter(event.values.clone(), geomagnetic)
            }

            if (gravity != null && geomagnetic != null) {
                val r = FloatArray(9)
                val i = FloatArray(9)
                if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(r, orientation)
                    val azimuthRad = orientation[0]
                    val azimuthDeg = (Math.toDegrees(azimuthRad.toDouble()).toFloat() + 360f) % 360f
                    updateSmoothHeading(azimuthDeg)
                }
            }
        }
    }

    private fun updateSmoothHeading(newHeading: Float) {
        // Prevent 360/0 jump jitter
        var diff = newHeading - currentHeading
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f

        val alpha = 0.25f // Smoothing factor
        currentHeading = (currentHeading + alpha * diff + 360f) % 360f
        _azimuthHeading.value = currentHeading
    }

    private fun lowPassFilter(input: FloatArray, output: FloatArray?): FloatArray {
        if (output == null) return input
        val alpha = 0.2f
        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
        return output
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
