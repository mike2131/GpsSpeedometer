package com.example.gpsspeedometer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    viewModel: SpeedViewModel, 
    repository: TripRepository,
    onShareGpx: (String, String) -> Unit
) {
    val state = viewModel.uiState.value
    var showHistoryDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition()
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    MaterialTheme(colorScheme = darkColorScheme(background = Color.Black)) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(if (viewModel.isLiveMode.value) "LIVE" else "HISTORY", fontSize = 18.sp) },
                    navigationIcon = {
                        if (!viewModel.isLiveMode.value) {
                            IconButton(onClick = { viewModel.switchToLiveMode() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showHistoryDialog = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "History")
                        }
                        if (!viewModel.isLiveMode.value) {
                            IconButton(onClick = { 
                                val date = viewModel.selectedDate.value
                                val trips = viewModel.getTripsForSelectedDate()
                                val gpx = repository.convertMultipleTripsToGpx(trips)
                                val filename = "${date.year}${date.monthNumber.toString().padStart(2, '0')}${date.dayOfMonth.toString().padStart(2, '0')}.gpx"
                                onShareGpx(gpx, filename)
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                BottomAppBar(containerColor = Color.Black) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isPaused = viewModel.isPaused.value
                        Button(
                            onClick = { viewModel.togglePause() },
                            modifier = Modifier.width(220.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPaused) Color.Gray else Color(0xFFA0FFFF)
                            )
                        ) {
                            if (isPaused) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color.Black)
                                )
                                Text(" Logging Stopped", color = Color.Black, fontWeight = FontWeight.Bold)
                            } else {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .graphicsLayer(scaleX = glowScale, scaleY = glowScale)
                                            .alpha(glowAlpha)
                                            .background(Color.Red, CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(Color.Red, CircleShape)
                                    )
                                }
                                Text(" GPS Logging...", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().background(Color.Black).padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${(state.speedKmh * 10).toInt() / 10.0}",
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isEstimated) Color.Gray else Color(0xFF00FF00)
                )
                Text("km/h", fontSize = 24.sp, color = Color.White)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                MapView(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    state = state,
                    viewModel = viewModel
                )
            }
        }
    }

    if (showHistoryDialog) {
        DatePickerDialog(
            onDismissRequest = { showHistoryDialog = false },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("OK")
                }
            }
        ) {
            val datePickerState = rememberDatePickerState()
            DatePicker(state = datePickerState)
            LaunchedEffect(datePickerState.selectedDateMillis) {
                datePickerState.selectedDateMillis?.let {
                    val date = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
                    viewModel.loadHistory(date)
                }
            }
        }
    }
}
