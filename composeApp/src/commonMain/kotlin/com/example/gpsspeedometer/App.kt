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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    viewModel: SpeedViewModel, 
    repository: TripRepository,
    onKeepScreenOnToggle: (Boolean) -> Unit, 
    onShareGpx: (String, String) -> Unit
) {
    val state = viewModel.uiState.value
    var showHistoryDialog by remember { mutableStateOf(false) }
    var isKeepScreenOn by remember { mutableStateOf(false) }

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
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            isKeepScreenOn = !isKeepScreenOn
                            onKeepScreenOnToggle(isKeepScreenOn)
                        }) {
                            Icon(
                                if (isKeepScreenOn) Icons.Default.Lock else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isKeepScreenOn) Color(0xFF00FF00) else Color.Gray
                            )
                        }
                        
                        Button(
                            onClick = { viewModel.togglePause() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.isPaused.value) Color.Red else Color(0xFF00FF00)
                            )
                        ) {
                            Icon(
                                if (viewModel.isPaused.value) Icons.Default.PlayArrow else Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Text(if (viewModel.isPaused.value) " RESUME" else " PAUSE", color = Color.Black)
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
