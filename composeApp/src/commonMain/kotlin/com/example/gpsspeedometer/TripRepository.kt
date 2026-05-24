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

data class TripVerifyData(
    val id: Long,
    val timestamp: Long,
    val pathPoints: List<LatLng>
)

class TripRepository(database: AppDatabase) {
    private val queries = database.tripQueries

    fun initializeDatabase() {
        queries.createTripTable()
        queries.createTripVerifyTable()
        queries.createSaveLogTable()
    }

    fun insertManualLog(timestamp: Long, status: String, message: String) {
        queries.insertSaveLog(timestamp, status, message)
    }

    suspend fun saveTrip(timestamp: Long, maxSpeed: Double, distance: Double, pathPoints: List<LatLng>): Long? = withContext(Dispatchers.Default) {
        try {
            initializeDatabase()
            val legacyPathData = pathPoints.joinToString(";") { "${it.latitude},${it.longitude}" }
            queries.insertTrip(timestamp, maxSpeed, distance, legacyPathData)
            val tripId = queries.lastInsertRowId().executeAsOne()

            val newPathData = pathPoints.joinToString(";") { 
                val latEnc = Base64Int.encode((it.latitude * 1000000).toLong())
                val lonEnc = Base64Int.encode((it.longitude * 1000000).toLong())
                val timeEnc = Base64Int.encode(it.timeOffset)
                val accEnc = Base64Int.encode(AccuracyLogic.compress(it.accuracy).toLong())
                val provId = when(it.provider) {
                    "gps" -> 0L
                    "network" -> 1L
                    "fused" -> 2L
                    else -> 3L
                }
                val provEnc = Base64Int.encode(provId)
                "$latEnc,$lonEnc,$timeEnc,$accEnc,$provEnc"
            }
            queries.insertTripVerify(timestamp, newPathData)
            
            queries.insertSaveLog(
                timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                status = "SUCCESS",
                message = "Created Trip ID $tripId with ${pathPoints.size} points"
            )
            tripId
        } catch (e: Exception) {
            queries.insertSaveLog(
                timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                status = "FAILED",
                message = "Save failed: ${e.message}"
            )
            null
        }
    }

    suspend fun updateTrip(tripId: Long, pathPoints: List<LatLng>) = withContext(Dispatchers.Default) {
        try {
            val legacyPathData = pathPoints.joinToString(";") { "${it.latitude},${it.longitude}" }
            queries.updateTripPath(legacyPathData, tripId)

            val newPathData = pathPoints.joinToString(";") { 
                val latEnc = Base64Int.encode((it.latitude * 1000000).toLong())
                val lonEnc = Base64Int.encode((it.longitude * 1000000).toLong())
                val timeEnc = Base64Int.encode(it.timeOffset)
                val accEnc = Base64Int.encode(AccuracyLogic.compress(it.accuracy).toLong())
                val provId = when(it.provider) {
                    "gps" -> 0L
                    "network" -> 1L
                    "fused" -> 2L
                    else -> 3L
                }
                val provEnc = Base64Int.encode(provId)
                "$latEnc,$lonEnc,$timeEnc,$accEnc,$provEnc"
            }
            // TripVerifyのIDもTripと同じであると仮定（同時挿入のため）
            queries.updateTripVerifyPath(newPathData, tripId)

            queries.insertSaveLog(
                timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                status = "UPDATE",
                message = "Updated Trip ID $tripId to ${pathPoints.size} points"
            )
            true
        } catch (e: Exception) {
            queries.insertSaveLog(
                timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                status = "UPDATE_FAILED",
                message = "Update failed for ID $tripId: ${e.message}"
            )
            false
        }
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

    suspend fun getTripVerifyByDate(startTime: Long, endTime: Long): List<TripVerifyData> = withContext(Dispatchers.Default) {
        queries.selectTripVerifyByDate(startTime, endTime).executeAsList().map {
            TripVerifyData(
                id = it.id,
                timestamp = it.timestamp,
                pathPoints = parsePathData(it.pathData)
            )
        }
    }

    suspend fun syncLegacyData() = withContext(Dispatchers.Default) {
        initializeDatabase()
        val verifyCount = queries.countTripVerify().executeAsOne()
        if (verifyCount == 0L) {
            val allTrips = queries.selectAllTrips().executeAsList()
            allTrips.forEach { trip ->
                queries.insertTripVerify(trip.timestamp, trip.pathData)
            }
        }
    }

    private fun parsePathData(pathData: String): List<LatLng> {
        if (pathData.isEmpty()) return emptyList()
        return pathData.split(";").mapNotNull { segment ->
            val parts = segment.split(",")
            when (parts.size) {
                2 -> {
                    LatLng(
                        latitude = parts[0].toDoubleOrNull() ?: 0.0,
                        longitude = parts[1].toDoubleOrNull() ?: 0.0
                    )
                }
                5 -> {
                    val lat = Base64Int.decode(parts[0]).toDouble() / 1000000.0
                    val lon = Base64Int.decode(parts[1]).toDouble() / 1000000.0
                    val time = Base64Int.decode(parts[2])
                    val acc = AccuracyLogic.decompress(Base64Int.decode(parts[3]).toInt())
                    val prov = when (Base64Int.decode(parts[4])) {
                        0L -> "gps"
                        1L -> "network"
                        2L -> "fused"
                        else -> "unknown"
                    }
                    LatLng(lat, lon, time, acc, prov)
                }
                else -> null
            }
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
