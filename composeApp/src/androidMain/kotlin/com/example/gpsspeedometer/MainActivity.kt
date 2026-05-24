package com.example.gpsspeedometer

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.gpsspeedometer.db.AppDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repository: TripRepository
    private lateinit var viewModel: SpeedViewModel

    private fun logState(state: String) {
        if (::repository.isInitialized) {
            lifecycleScope.launch {
                repository.initializeDatabase()
                val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                repository.insertManualLog(timestamp, "LIFECYCLE", state)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val driver = AndroidSqliteDriver(AppDatabase.Schema, applicationContext, "gps_speedometer.db")
        val database = AppDatabase(driver)
        repository = TripRepository(database)
        repository.initializeDatabase()
        
        logState("onCreate")

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { _ -> }

        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )

        val locationService = AndroidLocationService(applicationContext)
        viewModel = SpeedViewModel(locationService, repository)

        lifecycleScope.launch {
            repository.syncLegacyData()
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            App(
                viewModel = viewModel,
                repository = repository,
                onShareGpx = { gpx, filename ->
                    ShareUtils.shareGpx(this, gpx, filename)
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        logState("onStart")
    }

    override fun onResume() {
        super.onResume()
        logState("onResume")
    }

    override fun onPause() {
        super.onPause()
        logState("onPause")
        if (::viewModel.isInitialized) {
            viewModel.stopTracking()
        }
    }

    override fun onStop() {
        super.onStop()
        logState("onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        logState("onDestroy")
    }
}
