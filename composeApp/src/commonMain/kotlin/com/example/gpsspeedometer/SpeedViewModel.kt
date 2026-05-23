package com.example.gpsspeedometer

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.*

class SpeedViewModel(
    private val locationService: LocationService,
    private val repository: TripRepository
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _uiState = mutableStateOf(SpeedInfo())
    val uiState: State<SpeedInfo> = _uiState

    private val _historyPath = mutableStateOf<List<LatLng>>(emptyList())
    val historyPath: State<List<LatLng>> = _historyPath

    private val _currentHistoryTrip = mutableStateOf<TripData?>(null)
    val currentHistoryTrip: State<TripData?> = _currentHistoryTrip

    private val _selectedDate = mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    val selectedDate: State<LocalDate> = _selectedDate

    private var _tripsForDate = mutableListOf<TripData>()

    private val _isLiveMode = mutableStateOf(true)
    val isLiveMode: State<Boolean> = _isLiveMode

    private val _isPaused = mutableStateOf(false)
    val isPaused: State<Boolean> = _isPaused

    private var maxSpeed = 0.0
    private var startTime = 0L

    init {
        startTracking()
        locationService.speedInfo.onEach {
            if (_isPaused.value) return@onEach
            _uiState.value = it
            if (it.speedKmh > maxSpeed) maxSpeed = it.speedKmh
        }.launchIn(viewModelScope)
    }

    fun startTracking() {
        maxSpeed = 0.0
        startTime = Clock.System.now().toEpochMilliseconds()
        _isLiveMode.value = true
        locationService.startTracking()
    }

    fun stopTracking() {
        locationService.stopTracking()
        saveCurrentTrip()
    }

    private fun saveCurrentTrip() {
        val points = _uiState.value.pathPoints
        if (points.size < 2) return
        
        viewModelScope.launch {
            repository.saveTrip(startTime, maxSpeed, 0.0, points)
        }
    }

    fun loadHistory(date: LocalDate) {
        _selectedDate.value = date
        val start = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val end = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        
        viewModelScope.launch {
            _tripsForDate = repository.getTripsByDate(start, end).toMutableList()
            if (_tripsForDate.isNotEmpty()) {
                _currentHistoryTrip.value = _tripsForDate.first()
                _historyPath.value = repository.filterPathPoints(_tripsForDate.flatMap { it.pathPoints })
                _isLiveMode.value = false
            }
        }
    }

    fun getTripsForSelectedDate(): List<TripData> = _tripsForDate

    fun togglePause() {
        _isPaused.value = !_isPaused.value
    }

    fun switchToLiveMode() {
        _isLiveMode.value = true
    }

    fun getGpxData(): String? {
        return if (_tripsForDate.isNotEmpty()) repository.convertMultipleTripsToGpx(_tripsForDate) else null
    }
}
