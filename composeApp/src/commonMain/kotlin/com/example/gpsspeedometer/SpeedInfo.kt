package com.example.gpsspeedometer

data class LatLng(val latitude: Double, val longitude: Double)

data class SpeedInfo(
    val speedKmh: Double = 0.0,
    val speedMph: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val pathPoints: List<LatLng> = emptyList(),
    val isEstimated: Boolean = false
)
