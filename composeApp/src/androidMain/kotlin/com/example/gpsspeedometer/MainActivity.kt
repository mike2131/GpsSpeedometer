package com.example.gpsspeedometer

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.gpsspeedometer.db.AppDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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

        val driver = AndroidSqliteDriver(AppDatabase.Schema, applicationContext, "gps_speedometer.db")
        val database = AppDatabase(driver)
        val repository = TripRepository(database)
        val locationService = AndroidLocationService(applicationContext)
        val viewModel = SpeedViewModel(locationService, repository)

        setContent {
            App(
                viewModel = viewModel,
                repository = repository,
                onKeepScreenOnToggle = { enabled ->
                    if (enabled) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                },
                onShareGpx = { gpx, filename ->
                    ShareUtils.shareGpx(this, gpx, filename)
                }
            )
        }
    }
}
