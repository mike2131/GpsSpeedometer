package com.example.gpsspeedometer

import com.example.gpsspeedometer.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TripData(
    val id: Long,
    val timestamp: Long,
    val maxSpeed: Double,
    val distance: Double,
    val pathPoints: List<LatLng>
)

class TripRepository(database: AppDatabase) {
    private val queries = database.tripQueries

    suspend fun saveTrip(timestamp: Long, maxSpeed: Double, distance: Double, pathPoints: List<LatLng>) = withContext(Dispatchers.Default) {
        val pathData = pathPoints.joinToString(";") { "${it.latitude},${it.longitude}" }
        queries.insertTrip(timestamp, maxSpeed, distance, pathData)
    }

    suspend fun getTripsByDate(startTime: Long, endTime: Long): List<TripData> = withContext(Dispatchers.Default) {
        queries.selectTripsByDate(startTime, endTime).executeAsList().map {
            TripData(
                id = it.id,
                timestamp = it.timestamp,
                maxSpeed = it.maxSpeed,
                distance = it.distance,
                pathPoints = parsePathData(it.pathData)
            )
        }
    }

    private fun parsePathData(pathData: String): List<LatLng> {
        if (pathData.isEmpty()) return emptyList()
        return pathData.split(";").map {
            val parts = it.split(",")
            LatLng(parts[0].toDouble(), parts[1].toDouble())
        }
    }

    fun convertMultipleTripsToGpx(trips: List<TripData>): String {
        val header = """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="ForestFPS">
<trk>
"""
        val segments = trips.joinToString("\n") { trip ->
            val filteredPoints = filterPathPoints(trip.pathPoints)
            """<trkseg>
""" + filteredPoints.joinToString("\n") { 
                """<trkpt lat="${it.latitude}" lon="${it.longitude}"></trkpt>""" 
            } + """
</trkseg>"""
        }
        val footer = """</trk>
</gpx>"""
        return header + segments + "\n" + footer
    }

    fun filterPathPoints(points: List<LatLng>): List<LatLng> {
        if (points.isEmpty()) return emptyList()
        val filtered = mutableListOf<LatLng>()
        filtered.add(points[0])
        
        for (i in 1 until points.size) {
            val lastValid = filtered.last()
            val current = points[i]
            val dist = calculateDistance(lastValid, current)
            
            if (dist <= 0.3333) {
                filtered.add(current)
            }
        }
        return filtered
    }

    private fun calculateDistance(p1: LatLng, p2: LatLng): Double {
        val r = 6371.0
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(p1.latitude)) * Math.cos(Math.toRadians(p2.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
