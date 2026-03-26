package com.byd.dilink.extras.prayer.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import com.byd.dilink.extras.prayer.engine.QiblaCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class QiblaViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel(), SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val _currentHeading = MutableStateFlow(0f)
    val currentHeading: StateFlow<Float> = _currentHeading.asStateFlow()

    private val _qiblaBearing = MutableStateFlow(0.0)
    val qiblaBearing: StateFlow<Double> = _qiblaBearing.asStateFlow()

    private val _distanceToMakkah = MutableStateFlow(0.0)
    val distanceToMakkah: StateFlow<Double> = _distanceToMakkah.asStateFlow()

    // Rotation vector data
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Low-pass filter
    private var filteredHeading = 0f
    private val alpha = 0.15f

    init {
        // Calculate Qibla from GPS or default
        calculateQibla()

        // Register sensor
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationSensor != null) {
            sensorManager?.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            // Fallback to magnetic field + accelerometer
            val magneticSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            magneticSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            accelSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun calculateQibla() {
        var lat = 36.19 // Default: Erbil
        var lon = 44.01
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val providers = lm?.getProviders(true) ?: emptyList()
            val provider = when {
                providers.contains(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                providers.contains(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }
            if (provider != null) {
                val location = lm?.getLastKnownLocation(provider)
                if (location != null) {
                    lat = location.latitude
                    lon = location.longitude
                }
            }
        } catch (_: SecurityException) {
            // Use default
        } catch (_: Exception) {
            // Use default
        }

        _qiblaBearing.value = QiblaCalculator.qiblaBearing(lat, lon)
        _distanceToMakkah.value = QiblaCalculator.distanceToMakkah(lat, lon)
    }

    private var lastAccel: FloatArray? = null
    private var lastMagnetic: FloatArray? = null

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                val normalizedHeading = (azimuthDeg + 360f) % 360f
                filteredHeading = lowPass(normalizedHeading, filteredHeading)
                _currentHeading.value = filteredHeading
            }
            Sensor.TYPE_ACCELEROMETER -> {
                lastAccel = event.values.copyOf()
                computeHeadingFromAccelMag()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                lastMagnetic = event.values.copyOf()
                computeHeadingFromAccelMag()
            }
        }
    }

    private fun computeHeadingFromAccelMag() {
        val accel = lastAccel ?: return
        val magnetic = lastMagnetic ?: return
        val r = FloatArray(9)
        val i = FloatArray(9)
        if (SensorManager.getRotationMatrix(r, i, accel, magnetic)) {
            SensorManager.getOrientation(r, orientationAngles)
            val azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            val normalizedHeading = (azimuthDeg + 360f) % 360f
            filteredHeading = lowPass(normalizedHeading, filteredHeading)
            _currentHeading.value = filteredHeading
        }
    }

    private fun lowPass(input: Float, output: Float): Float {
        // Handle angle wrapping (e.g., 359 → 1 should not average to 180)
        var diff = input - output
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        val result = output + alpha * diff
        return ((result % 360f) + 360f) % 360f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCleared() {
        super.onCleared()
        sensorManager?.unregisterListener(this)
    }
}
