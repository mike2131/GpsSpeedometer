package com.example.gpsspeedometer

import kotlinx.coroutines.flow.StateFlow

interface LocationService {
    val speedInfo: StateFlow<SpeedInfo>
    fun startTracking()
    fun stopTracking()
}
