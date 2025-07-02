package com.mpo.trucktow.services

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.mpo.trucktow.models.Driver
import com.mpo.trucktow.models.TowTruck

class LocationTrackingManager(private val context: Context) {
    
    private var locationTrackingService: LocationTrackingService? = null
    private var isBound = false
    
    // Callbacks
    private var onDriverLocationUpdate: ((String, LatLng, Float, Float) -> Unit)? = null
    private var onTruckLocationUpdate: ((String, LatLng, Float, Float) -> Unit)? = null
    private var onServiceConnected: (() -> Unit)? = null
    private var onServiceDisconnected: (() -> Unit)? = null
    private var onServiceError: ((String) -> Unit)? = null
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationTrackingService.LocationBinder
            locationTrackingService = binder.getService()
            isBound = true
            
            // Set up callbacks
            locationTrackingService?.setDriverLocationCallback { driverId, location, speed, heading ->
                onDriverLocationUpdate?.invoke(driverId, location, speed, heading)
            }
            
            locationTrackingService?.setTruckLocationCallback { truckId, location, speed, heading ->
                onTruckLocationUpdate?.invoke(truckId, location, speed, heading)
            }
            
            onServiceConnected?.invoke()
            Log.d(TAG, "Location tracking service connected")
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            locationTrackingService = null
            isBound = false
            onServiceDisconnected?.invoke()
            Log.d(TAG, "Location tracking service disconnected")
        }
    }
    
    fun startService(): Boolean {
        // Check permissions first
        if (!hasRequiredPermissions()) {
            val errorMsg = "Missing required permissions for location tracking"
            Log.e(TAG, errorMsg)
            onServiceError?.invoke(errorMsg)
            return false
        }
        
        try {
            val intent = Intent(context, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            return true
        } catch (e: Exception) {
            val errorMsg = "Failed to start location tracking service: ${e.message}"
            Log.e(TAG, errorMsg, e)
            onServiceError?.invoke(errorMsg)
            return false
        }
    }
    
    private fun hasRequiredPermissions(): Boolean {
        val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val foregroundServicePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Manifest.permission.FOREGROUND_SERVICE_LOCATION
        } else {
            Manifest.permission.FOREGROUND_SERVICE
        }
        
        val allPermissions = locationPermissions + foregroundServicePermission
        
        return allPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun stopService() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
        val intent = Intent(context, LocationTrackingService::class.java)
        context.stopService(intent)
    }
    
    fun startTrackingDriver(driverId: String, driver: Driver) {
        locationTrackingService?.startTrackingDriver(driverId, driver)
    }
    
    fun startTrackingTruck(truckId: String, truck: TowTruck) {
        locationTrackingService?.startTrackingTruck(truckId, truck)
    }
    
    fun stopTrackingDriver(driverId: String) {
        locationTrackingService?.stopTrackingDriver(driverId)
    }
    
    fun stopTrackingTruck(truckId: String) {
        locationTrackingService?.stopTrackingTruck(truckId)
    }
    
    fun setOnDriverLocationUpdate(callback: (String, LatLng, Float, Float) -> Unit) {
        onDriverLocationUpdate = callback
        locationTrackingService?.setDriverLocationCallback(callback)
    }
    
    fun setOnTruckLocationUpdate(callback: (String, LatLng, Float, Float) -> Unit) {
        onTruckLocationUpdate = callback
        locationTrackingService?.setTruckLocationCallback(callback)
    }
    
    fun setOnServiceConnected(callback: () -> Unit) {
        onServiceConnected = callback
    }
    
    fun setOnServiceDisconnected(callback: () -> Unit) {
        onServiceDisconnected = callback
    }
    
    fun setOnServiceError(callback: (String) -> Unit) {
        onServiceError = callback
    }
    
    fun isServiceBound(): Boolean = isBound
    
    companion object {
        private const val TAG = "LocationTrackingManager"
    }
} 