package com.example.gpsspeedometer

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class AndroidLocationService(private val context: Context) : LocationService, SensorEventListener {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    private val _speedInfo = MutableStateFlow(SpeedInfo())
    override val speedInfo: StateFlow<SpeedInfo> = _speedInfo.asStateFlow()

    private var currentSpeedMs = 0.0
    private var lastLocationTime = 0L
    private var startRealtime = 0L

    init {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastLocationTime > 1000) {
            val dt = 0.1
            val acc = event.values[2].toDouble()
            if (abs(acc) > 0.5) {
                currentSpeedMs = (currentSpeedMs + acc * dt).coerceAtLeast(0.0)
                _speedInfo.value = _speedInfo.value.copy(
                    speedKmh = currentSpeedMs * 3.6,
                    isEstimated = true
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            lastLocationTime = SystemClock.elapsedRealtime()
            currentSpeedMs = if (location.hasSpeed()) location.speed.toDouble() else 0.0
            
            val offset = if (startRealtime > 0) (lastLocationTime - startRealtime) / 10 else 0L

            _speedInfo.value = SpeedInfo(
                speedKmh = currentSpeedMs * 3.6,
                speedMph = currentSpeedMs * 2.23694,
                latitude = location.latitude,
                longitude = location.longitude,
                timeOffset = offset,
                accuracy = location.accuracy,
                provider = location.provider ?: "unknown",
                pathPoints = emptyList(),
                isEstimated = false
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun startTracking() {
        startRealtime = SystemClock.elapsedRealtime()
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    override fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
