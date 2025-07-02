package com.mpo.trucktow.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.mpo.trucktow.R
import com.mpo.trucktow.models.Driver
import com.mpo.trucktow.models.TowTruck
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class LocationTrackingService : Service() {
    
    private val binder = LocationBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var locationUpdateJob: Job? = null
    
    // Tracked entities
    private val trackedDrivers = ConcurrentHashMap<String, Driver>()
    private val trackedTrucks = ConcurrentHashMap<String, TowTruck>()
    
    // Callbacks for location updates
    private var driverLocationCallback: ((String, LatLng, Float, Float) -> Unit)? = null
    private var truckLocationCallback: ((String, LatLng, Float, Float) -> Unit)? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "LocationTrackingChannel"
        private const val LOCATION_UPDATE_INTERVAL = 3000L // 3 seconds
        private const val MIN_DISTANCE_CHANGE = 10f // 10 meters
    }
    
    inner class LocationBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }
    
    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: SecurityException) {
            // Handle permission error - log and stop service
            android.util.Log.e("LocationTrackingService", "Failed to start foreground service: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    fun startTrackingDriver(driverId: String, driver: Driver) {
        trackedDrivers[driverId] = driver
        startLocationUpdates()
    }
    
    fun startTrackingTruck(truckId: String, truck: TowTruck) {
        trackedTrucks[truckId] = truck
        startLocationUpdates()
    }
    
    fun stopTrackingDriver(driverId: String) {
        trackedDrivers.remove(driverId)
        if (trackedDrivers.isEmpty() && trackedTrucks.isEmpty()) {
            stopLocationUpdates()
        }
    }
    
    fun stopTrackingTruck(truckId: String) {
        trackedTrucks.remove(truckId)
        if (trackedDrivers.isEmpty() && trackedTrucks.isEmpty()) {
            stopLocationUpdates()
        }
    }
    
    fun setDriverLocationCallback(callback: (String, LatLng, Float, Float) -> Unit) {
        driverLocationCallback = callback
    }
    
    fun setTruckLocationCallback(callback: (String, LatLng, Float, Float) -> Unit) {
        truckLocationCallback = callback
    }
    
    private fun startLocationUpdates() {
        if (locationCallback != null) return
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_CHANGE)
            .setWaitForAccurateLocation(false)
            .build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateTrackedEntities(location)
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Handle permission error
        }
        
        // Start simulated updates for demo purposes
        startSimulatedUpdates()
    }
    
    private fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
        }
        locationUpdateJob?.cancel()
    }
    
    private fun updateTrackedEntities(location: Location) {
        val currentLatLng = LatLng(location.latitude, location.longitude)
        
        // Update tracked drivers
        trackedDrivers.forEach { (driverId, driver) ->
            val updatedDriver = driver.copy(
                currentLocation = currentLatLng,
                currentSpeed = location.speed,
                heading = location.bearing,
                lastActiveTime = System.currentTimeMillis()
            )
            trackedDrivers[driverId] = updatedDriver
            driverLocationCallback?.invoke(driverId, currentLatLng, location.speed, location.bearing)
        }
        
        // Update tracked trucks
        trackedTrucks.forEach { (truckId, truck) ->
            val updatedTruck = truck.copy(
                location = currentLatLng,
                currentSpeed = location.speed,
                heading = location.bearing,
                lastLocationUpdate = System.currentTimeMillis()
            )
            trackedTrucks[truckId] = updatedTruck
            truckLocationCallback?.invoke(truckId, currentLatLng, location.speed, location.bearing)
        }
    }
    
    private fun startSimulatedUpdates() {
        locationUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                // Simulate driver/truck movement for demo
                simulateMovement()
                delay(LOCATION_UPDATE_INTERVAL)
            }
        }
    }
    
    private fun simulateMovement() {
        trackedDrivers.forEach { (driverId, driver) ->
            driver.currentLocation?.let { currentLocation ->
                // Simulate realistic movement
                val newLat = currentLocation.latitude + (Math.random() - 0.5) * 0.0001
                val newLng = currentLocation.longitude + (Math.random() - 0.5) * 0.0001
                val newLocation = LatLng(newLat, newLng)
                
                val updatedDriver = driver.copy(
                    currentLocation = newLocation,
                    currentSpeed = (Math.random() * 20 + 10).toFloat(), // 10-30 km/h
                    heading = (Math.random() * 360).toFloat(),
                    lastActiveTime = System.currentTimeMillis()
                )
                trackedDrivers[driverId] = updatedDriver
                driverLocationCallback?.invoke(driverId, newLocation, updatedDriver.currentSpeed, updatedDriver.heading)
            }
        }
        
        trackedTrucks.forEach { (truckId, truck) ->
            // Simulate realistic movement
            val newLat = truck.location.latitude + (Math.random() - 0.5) * 0.0001
            val newLng = truck.location.longitude + (Math.random() - 0.5) * 0.0001
            val newLocation = LatLng(newLat, newLng)
            
            val updatedTruck = truck.copy(
                location = newLocation,
                currentSpeed = (Math.random() * 25 + 15).toFloat(), // 15-40 km/h
                heading = (Math.random() * 360).toFloat(),
                lastLocationUpdate = System.currentTimeMillis()
            )
            trackedTrucks[truckId] = updatedTruck
            truckLocationCallback?.invoke(truckId, newLocation, updatedTruck.currentSpeed, updatedTruck.heading)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks tow truck locations in real-time"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tow Truck Tracking")
            .setContentText("Tracking ${trackedDrivers.size + trackedTrucks.size} vehicles")
            .setSmallIcon(R.drawable.ic_tow_truck)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
} 