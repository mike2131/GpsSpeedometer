package com.example.gpsspeedometer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng as AndroidLatLng
import com.google.maps.android.compose.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
actual fun MapView(modifier: Modifier, state: SpeedInfo, viewModel: SpeedViewModel) {
    val currentPos = AndroidLatLng(state.latitude, state.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentPos, 15f)
    }

    var isAutoTracking by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableStateOf(0L) }

    val isLive = viewModel.isLiveMode.value

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
            isAutoTracking = false
            lastInteractionTime = System.currentTimeMillis()
        }
    }

    LaunchedEffect(isAutoTracking, lastInteractionTime) {
        if (!isAutoTracking) {
            delay(15000)
            isAutoTracking = true
        }
    }

    LaunchedEffect(state.latitude, state.longitude, isAutoTracking) {
        if (isLive && isAutoTracking && state.latitude != 0.0 && state.longitude != 0.0) {
            cameraPositionState.move(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                    currentPos, 
                    cameraPositionState.position.zoom
                )
            )
        }
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false)
        ) {
            if (isLive) {
                Polyline(
                    points = state.pathPoints.map { AndroidLatLng(it.latitude, it.longitude) },
                    color = Color.Red,
                    width = 10f
                )
                Marker(
                    state = MarkerState(position = currentPos),
                    title = "Current Location"
                )
            } else {
                Polyline(
                    points = viewModel.cyanPath.value.map { AndroidLatLng(it.latitude, it.longitude) },
                    color = Color.Cyan,
                    width = 12f
                )
                Polyline(
                    points = viewModel.magentaPath.value.map { AndroidLatLng(it.latitude, it.longitude) },
                    color = Color.Magenta,
                    width = 6f
                )
            }
        }

        if (isLive && !isAutoTracking) {
            FloatingActionButton(
                onClick = { isAutoTracking = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "Re-center")
            }
        }
    }
}
