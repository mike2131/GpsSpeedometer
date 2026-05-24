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

    private val _cyanPath = mutableStateOf<List<LatLng>>(emptyList())
    val cyanPath: State<List<LatLng>> = _cyanPath

    private val _magentaPath = mutableStateOf<List<LatLng>>(emptyList())
    val magentaPath: State<List<LatLng>> = _magentaPath

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
    private var currentTripId: Long? = null

    init {
        startTracking()
        locationService.speedInfo.onEach { info ->
            if (info.provider == "" || info.provider == "unknown") return@onEach
            if (info.latitude == 0.0 && info.longitude == 0.0) return@onEach

            val currentPoints = if (_isPaused.value) {
                _uiState.value.pathPoints
            } else {
                _uiState.value.pathPoints + LatLng(
                    latitude = info.latitude, 
                    longitude = info.longitude,
                    timeOffset = info.timeOffset,
                    accuracy = info.accuracy,
                    provider = info.provider
                )
            }

            if (!_isPaused.value && info.speedKmh > maxSpeed) {
                maxSpeed = info.speedKmh
            }

            _uiState.value = info.copy(pathPoints = currentPoints)

            // Step 2.4: 自動上書き保存ロジック (100地点ごと)
            if (!isPaused.value && currentPoints.size % 100 == 0 && currentPoints.isNotEmpty()) {
                viewModelScope.launch {
                    val id = currentTripId
                    if (id != null) {
                        repository.updateTrip(id, currentPoints)
                    } else {
                        currentTripId = repository.saveTrip(startTime, maxSpeed, 0.0, currentPoints)
                    }
                }
            }
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
            currentTripId = repository.saveTrip(startTime, maxSpeed, 0.0, points)
            if (currentTripId != null) {
                println("GPS Logging: Save SUCCESS (ID: $currentTripId)")
            } else {
                println("GPS Logging: Save FAILED")
            }
        }
    }

    fun loadHistory(date: LocalDate) {
        _selectedDate.value = date
        val start = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val end = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        
        viewModelScope.launch {
            _tripsForDate = repository.getTripsByDate(start, end).toMutableList()
            val verifyTrips = repository.getTripVerifyByDate(start, end)

            _cyanPath.value = _tripsForDate.flatMap { it.pathPoints }
            _magentaPath.value = verifyTrips.flatMap { it.pathPoints }

            if (_tripsForDate.isNotEmpty()) {
                _currentHistoryTrip.value = _tripsForDate.first()
            } else {
                _currentHistoryTrip.value = null
            }
            _isLiveMode.value = false
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
