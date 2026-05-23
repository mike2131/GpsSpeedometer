package com.example.gpsspeedometer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng as AndroidLatLng
import com.google.maps.android.compose.*

@Composable
actual fun MapView(modifier: Modifier, state: SpeedInfo, viewModel: SpeedViewModel) {
    val currentPos = AndroidLatLng(state.latitude, state.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentPos, 15f)
    }

    val isLive = viewModel.isLiveMode.value

    LaunchedEffect(state.latitude, state.longitude) {
        if (isLive && state.latitude != 0.0 && state.longitude != 0.0) {
            cameraPositionState.move(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                    currentPos, 
                    cameraPositionState.position.zoom
                )
            )
        }
    }

    val path = if (isLive) state.pathPoints else viewModel.historyPath.value

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(zoomControlsEnabled = false)
    ) {
        Polyline(
            points = path.map { AndroidLatLng(it.latitude, it.longitude) },
            color = if (isLive) Color.Red else Color.Cyan,
            width = 10f
        )
        if (isLive) {
            Marker(
                state = MarkerState(position = currentPos),
                title = "Current Location"
            )
        }
    }
}
