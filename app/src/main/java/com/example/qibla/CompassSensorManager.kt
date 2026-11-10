package com.example.qibla

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Rotation-vector compass with an accelerometer + magnetometer fallback, low-pass smoothing and
 * true-north correction using geomagnetic declination when a location is known. Accuracy is
 * reported honestly and never conflated with GPS accuracy.
 */
class CompassSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationVector = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _headingDegrees = MutableStateFlow(0f)
    val headingDegrees: StateFlow<Float> = _headingDegrees.asStateFlow()

    private val _accuracy = MutableStateFlow(CompassAccuracy.UNAVAILABLE)
    val accuracy: StateFlow<CompassAccuracy> = _accuracy.asStateFlow()

    private val _sensorAvailable = MutableStateFlow(false)
    val sensorAvailable: StateFlow<Boolean> = _sensorAvailable.asStateFlow()

    private var declinationDegrees = 0f
    private var started = false

    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null
    private var smoothed: Float? = null

    fun updateLocation(latitude: Double, longitude: Double, altitudeMeters: Double = 0.0) {
        try {
            val field = GeomagneticField(
                latitude.toFloat(),
                longitude.toFloat(),
                altitudeMeters.toFloat(),
                System.currentTimeMillis()
            )
            declinationDegrees = field.declination
        } catch (error: Exception) {
            declinationDegrees = 0f
        }
    }

    fun start() {
        if (started) return
        started = true
        val manager = sensorManager ?: return
        val usingRotation = if (rotationVector != null) {
            manager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_GAME)
        } else {
            false
        }
        if (!usingRotation && accelerometer != null && magnetometer != null) {
            manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            manager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        }
        _sensorAvailable.value = usingRotation || (accelerometer != null && magnetometer != null)
        _accuracy.value = if (_sensorAvailable.value) CompassAccuracy.MODERATE else CompassAccuracy.UNAVAILABLE
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        started = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val sensorEvent = event ?: return
        when (sensorEvent.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values)
                publish(rotationMatrix)
            }
            Sensor.TYPE_ACCELEROMETER -> gravity = sensorEvent.values.clone()
            Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = sensorEvent.values.clone()
            else -> return
        }
        if (sensorEvent.sensor.type != Sensor.TYPE_ROTATION_VECTOR) {
            val gravityValues = gravity ?: return
            val magneticValues = geomagnetic ?: return
            val rotationMatrix = FloatArray(9)
            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, magneticValues)) {
                publish(rotationMatrix)
            }
        }
    }

    private fun publish(rotationMatrix: FloatArray) {
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)
        var degrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
        degrees = (degrees + 360f) % 360f
        val trueHeading = (degrees + declinationDegrees + 360f) % 360f
        val previous = smoothed
        val next = if (previous == null) {
            trueHeading
        } else {
            // Shortest-path low-pass smoothing to avoid needle jitter.
            var delta = trueHeading - previous
            if (delta > 180f) delta -= 360f
            if (delta < -180f) delta += 360f
            if (abs(delta) > 45f) trueHeading else (previous + delta * 0.18f + 360f) % 360f
        }
        smoothed = next
        _headingDegrees.value = next
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        val mapped = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.HIGH
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.MODERATE
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.LOW
            SensorManager.SENSOR_STATUS_UNRELIABLE -> CompassAccuracy.LOW
            else -> CompassAccuracy.UNAVAILABLE
        }
        _accuracy.value = if (!_sensorAvailable.value) CompassAccuracy.UNAVAILABLE else mapped
    }
}
