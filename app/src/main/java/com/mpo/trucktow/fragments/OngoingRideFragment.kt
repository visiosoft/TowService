package com.mpo.trucktow.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.mpo.trucktow.R
import com.mpo.trucktow.databinding.FragmentOngoingRideBinding
import com.mpo.trucktow.models.Driver
import com.mpo.trucktow.models.TowTruck
import com.mpo.trucktow.models.Vehicle
import com.mpo.trucktow.services.LocationTrackingManager
import com.mpo.trucktow.services.DirectionsApiHelper
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class OngoingRideFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentOngoingRideBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationTrackingManager: LocationTrackingManager
    
    private var driverLocation: LatLng? = null
    private var userLocation: LatLng? = null
    private var driverMarker: com.google.android.gms.maps.model.Marker? = null
    private var userMarker: com.google.android.gms.maps.model.Marker? = null
    private var locationUpdateJob: Job? = null
    
    private var currentDriverSpeed: Float = 0f
    private var currentDriverHeading: Float = 0f
    private var lastUpdateTime: Long = 0L
    private var routePolyline: Polyline? = null
    private var connectionPolyline: Polyline? = null
    
    // Real-time update timer
    private val updateHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateRealTimeInfo()
            updateHandler.postDelayed(this, 3000) // Update every 3 seconds
        }
    }
    
    // Simulation handler for demo
    private var simulationHandler: Handler? = null
    private var simulationRunnable: Runnable? = null
    private var isFragmentActive = false

    // Mock data - Replace with actual data from your backend
    private val driver = Driver(
        id = "1",
        name = "John Doe",
        rating = 4.8f,
        phoneNumber = "+1234567890",
        imageUrl = "https://example.com/driver.jpg",
        currentLocation = null,
        isOnline = true,
        isOnTrip = true,
        tripId = "trip_123"
    )

    private val vehicle = Vehicle(
        id = "1",
        model = "Flatbed Tow Truck",
        licensePlate = "ABC123",
        color = "White"
    )
    
    private val towTruck = TowTruck(
        id = "truck_1",
        name = "John's Tow Service",
        location = LatLng(0.0, 0.0), // Will be updated
        distance = 0.0,
        rating = 4.8,
        isAvailable = false,
        phoneNumber = "+1234567890",
        vehicleType = "Flatbed Tow Truck",
        isTrackingEnabled = true,
        isOnTrip = true,
        tripId = "trip_123"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOngoingRideBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        isFragmentActive = true
        
        setupLocationTrackingManager()
        setupMap()
        setupDriverInfo()
        setupVehicleInfo()
        setupCommunicationButtons()
        setupCancelButton()
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        // Start real-time updates
        startRealTimeUpdates()
        
        // For demo purposes, simulate driver movement
        simulateDriverMovement()
        
        // Additional debugging for cancel button
        view.post {
            binding.cancelRideButton.visibility = View.VISIBLE
            binding.cancelRideButton.bringToFront()
        }
    }
    
    private fun startRealTimeUpdates() {
        updateHandler.post(updateRunnable)
    }
    
    private fun stopRealTimeUpdates() {
        updateHandler.removeCallbacks(updateRunnable)
    }
    
    private fun updateRealTimeInfo() {
        // Check if fragment is still active before updating
        if (!isFragmentActive || !isAdded || context == null) {
            return
        }
        
        updateEstimatedArrivalTime()
        updateDriverStatus()
        updateConnectionLine()
    }
    
    private fun updateConnectionLine() {
        // Check if fragment is still active before updating
        if (!isFragmentActive || !isAdded || context == null) {
            return
        }
        
        userLocation?.let { user ->
            driverLocation?.let { driver ->
                try {
                    // Remove previous connection line
                    connectionPolyline?.remove()
                    
                    // Draw new connection line with dotted pattern
                    connectionPolyline = map.addPolyline(
                        PolylineOptions()
                            .add(user, driver)
                            .color(resources.getColor(R.color.purple_700, null))
                            .width(8f)
                            .pattern(listOf(Dot(), Gap(15f)))
                    )
                    
                    // Update camera to show both markers with some padding
                    val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                        .include(user)
                        .include(driver)
                        .build()
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                } catch (e: Exception) {
                    android.util.Log.e("OngoingRideFragment", "Error updating connection line: ${e.message}")
                }
            }
        }
    }
    
    private fun simulateDriverMovement() {
        // For demo purposes, simulate driver moving towards user
        simulationHandler = Handler(Looper.getMainLooper())
        simulationRunnable = object : Runnable {
            private var step = 0
            override fun run() {
                // Check if fragment is still active before proceeding
                if (!isFragmentActive || !isAdded || context == null) {
                    return
                }
                
                userLocation?.let { user ->
                    // Create a simulated driver location that moves towards the user
                    val initialDriverLat = user.latitude + 0.01 // Start 1km away
                    val initialDriverLng = user.longitude + 0.01
                    
                    val progress = (step % 20) / 20.0 // 20 steps to reach user
                    val currentDriverLat = initialDriverLat - (0.01 * progress)
                    val currentDriverLng = initialDriverLng - (0.01 * progress)
                    
                    val simulatedDriverLocation = LatLng(currentDriverLat, currentDriverLng)
                    val simulatedSpeed = 15f // 15 m/s = ~54 km/h
                    val simulatedHeading = 225f // Moving towards user
                    
                    updateDriverLocation(simulatedDriverLocation, simulatedSpeed, simulatedHeading)
                    
                    step++
                    if (step < 40 && isFragmentActive && isAdded) { // Continue for 40 steps (2 minutes)
                        simulationHandler?.postDelayed(this, 3000) // Update every 3 seconds
                    }
                }
            }
        }
        simulationHandler?.postDelayed(simulationRunnable!!, 2000) // Start after 2 seconds
    }
    
    private fun setupLocationTrackingManager() {
        locationTrackingManager = LocationTrackingManager(requireContext())
        locationTrackingManager.setOnServiceConnected {
            // Start tracking the driver and truck
            locationTrackingManager.startTrackingDriver(driver.id, driver)
            locationTrackingManager.startTrackingTruck(towTruck.id, towTruck)
        }
        
        locationTrackingManager.setOnDriverLocationUpdate { driverId, location, speed, heading ->
            updateDriverLocation(location, speed, heading)
        }
        
        locationTrackingManager.setOnTruckLocationUpdate { truckId, location, speed, heading ->
            updateTruckLocation(location, speed, heading)
        }
        
        locationTrackingManager.setOnServiceError { errorMessage ->
            Toast.makeText(context, "Location tracking error: $errorMessage", Toast.LENGTH_LONG).show()
        }
        
        // Start the tracking service
        val serviceStarted = locationTrackingManager.startService()
        if (!serviceStarted) {
            Toast.makeText(context, "Failed to start location tracking service. Please check permissions.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.rideMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupDriverInfo() {
        binding.driverName.text = driver.name
        binding.driverRating.text = "Rating: ${driver.rating}"
        binding.driverStatus.text = "25 km/h • Just now"
        binding.statusChip.text = "On the way"
        // Load driver image using your preferred image loading library
        // Glide.with(this).load(driver.imageUrl).into(binding.driverImage)
    }

    private fun setupVehicleInfo() {
        binding.vehicleType.text = vehicle.model
        binding.vehicleNumber.text = vehicle.licensePlate
        binding.vehicleColor.text = vehicle.color
        // Load vehicle image using your preferred image loading library
        // Glide.with(this).load(vehicle.imageUrl).into(binding.vehicleImage)
    }

    private fun setupCommunicationButtons() {
        binding.callButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${driver.phoneNumber}")
            }
            startActivity(intent)
        }

        binding.messageButton.setOnClickListener {
            // Implement messaging functionality
            Toast.makeText(context, "Messaging feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCancelButton() {
        binding.cancelRideButton.setOnClickListener {
            // Show confirmation dialog
            showCancelConfirmationDialog()
        }
        
        // Make sure the button is visible and properly positioned
        binding.cancelRideButton.visibility = View.VISIBLE
        binding.cancelRideButton.bringToFront()
        
        // Add some debugging and ensure proper positioning
        binding.cancelRideButton.post {
            binding.cancelRideButton.visibility = View.VISIBLE
            binding.cancelRideButton.bringToFront()
            
            // Force layout update
            binding.cancelRideButton.requestLayout()
            
            // Add a small delay to ensure proper positioning
            Handler(Looper.getMainLooper()).postDelayed({
                binding.cancelRideButton.visibility = View.VISIBLE
                binding.cancelRideButton.bringToFront()
            }, 100)
        }
    }

    private fun showCancelConfirmationDialog() {
        // Implement cancel confirmation dialog
        Toast.makeText(context, "Cancel ride feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isMyLocationButtonEnabled = false // Disable default location button
        if (checkLocationPermission()) {
            enableMyLocation()
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = false // Disable default blue dot
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    userLocation = LatLng(location.latitude, location.longitude)
                    
                    // Load and scale the custom car icon
                    val carBitmap = BitmapFactory.decodeResource(resources, R.drawable.car_icon)
                    val largeCarIcon = Bitmap.createScaledBitmap(carBitmap, 120, 120, false)

                    // Add or update marker with custom car icon
                    if (userMarker == null) {
                        userMarker = map.addMarker(
                            MarkerOptions()
                                .position(userLocation!!)
                                .title("Your Location")
                                .icon(BitmapDescriptorFactory.fromBitmap(largeCarIcon))
                                .anchor(0.5f, 1.5f) // This moves icon slightly upward on map
                        )
                    } else {
                        // Update existing marker position
                        userMarker?.position = userLocation!!
                    }
                    
                    updateMapMarkers()
                }
            }
        }
    }

    private fun startLocationUpdates() {
        // Start real-time location updates for user
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000) // Update every 5 seconds
                    .setMinUpdateDistanceMeters(10f) // Update if moved 10 meters
                    .build()

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            locationResult.lastLocation?.let { location ->
                                userLocation = LatLng(location.latitude, location.longitude)
                                
                                // Update existing marker position
                                userMarker?.position = userLocation!!
                                
                                // Update connection line and ETA
                                updateConnectionLine()
                                updateEstimatedArrivalTime()
                            }
                        }
                    },
                    requireActivity().mainLooper
                )
            } catch (e: SecurityException) {
                Toast.makeText(context, "Location permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateDriverLocation(location: LatLng, speed: Float, heading: Float) {
        // Check if fragment is still active before updating
        if (!isFragmentActive || !isAdded || context == null) {
            return
        }
        
        driverLocation = location
        currentDriverSpeed = speed
        currentDriverHeading = heading
        lastUpdateTime = System.currentTimeMillis()
        
        updateMapMarkers()
        updateEstimatedArrivalTime()
        updateConnectionLine()
    }
    
    private fun updateTruckLocation(location: LatLng, speed: Float, heading: Float) {
        // Check if fragment is still active before updating
        if (!isFragmentActive || !isAdded || context == null) {
            return
        }
        
        // Update truck location if needed
        updateMapMarkers()
        updateEstimatedArrivalTime()
        updateConnectionLine()
    }
    
    private fun updateDriverStatus() {
        // Check if fragment is still active before updating
        if (!isFragmentActive || !isAdded || context == null || _binding == null) {
            return
        }
        
        try {
            val speedKmh = (currentDriverSpeed * 3.6).toInt() // Convert m/s to km/h
            val timeAgo = getTimeAgo(lastUpdateTime)
            
            binding.driverStatus.text = "Speed: ${speedKmh} km/h • Updated: $timeAgo"
        } catch (e: Exception) {
            android.util.Log.e("OngoingRideFragment", "Error updating driver status: ${e.message}")
        }
    }
    
    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            else -> "${diff / 3600000}h ago"
        }
    }
    


    private fun updateMapMarkers() {
        // Check if fragment is still active and attached
        if (!isFragmentActive || !isAdded || context == null) {
            return
        }
        
        try {
            // Clear existing driver marker
            driverMarker?.remove()
            // Remove previous polyline
            routePolyline?.remove()
            // Add driver marker with custom icon
            driverLocation?.let { location ->
                val truckBitmap = BitmapFactory.decodeResource(resources, R.drawable.tow_truck_icon)
                val largeTruckIcon = Bitmap.createScaledBitmap(truckBitmap, 120, 120, false)
                driverMarker = map.addMarker(
                    MarkerOptions()
                        .position(location)
                        .title("Driver Location")
                        .snippet("Speed: ${(currentDriverSpeed * 3.6).toInt()} km/h")
                        .icon(BitmapDescriptorFactory.fromBitmap(largeTruckIcon))
                        .rotation(currentDriverHeading)
                        .anchor(0.5f, 0.5f)
                )
            }
            // Update camera to show both markers
            if (userLocation != null && driverLocation != null) {
                val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                    .include(userLocation!!)
                    .include(driverLocation!!)
                    .build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                // Fetch and draw route
                fetchAndDrawRoute(userLocation!!, driverLocation!!)
            }
        } catch (e: Exception) {
            // Log the error but don't crash
            android.util.Log.e("OngoingRideFragment", "Error updating map markers: ${e.message}")
        }
    }

    private fun fetchAndDrawRoute(origin: LatLng, destination: LatLng) {
        val apiKey = getString(R.string.google_maps_key)
        CoroutineScope(Dispatchers.Main).launch {
            val result = DirectionsApiHelper.getRoute(origin, destination, apiKey)
            if (result != null) {
                // Draw polyline
                routePolyline = map.addPolyline(
                    PolylineOptions()
                        .addAll(result.polylinePoints)
                        .color(resources.getColor(R.color.purple_700, null))
                        .width(10f)
                )
            }
        }
    }

    private fun updateEstimatedArrivalTime() {
        // Check if fragment is still active before updating
        if (!isFragmentActive || !isAdded || context == null || _binding == null) {
            return
        }
        
        userLocation?.let { user ->
            driverLocation?.let { driver ->
                try {
                    val distance = calculateDistance(user, driver)
                    val estimatedTimeMinutes = calculateEstimatedTime(distance, currentDriverSpeed)
                    
                    // Format time display
                    val timeDisplay = when {
                        estimatedTimeMinutes < 1 -> "Less than 1 min"
                        estimatedTimeMinutes < 60 -> "$estimatedTimeMinutes mins"
                        else -> "${estimatedTimeMinutes / 60}h ${estimatedTimeMinutes % 60}m"
                    }
                    
                    binding.estimatedArrivalTime.text = "Arriving in $timeDisplay"
                } catch (e: Exception) {
                    android.util.Log.e("OngoingRideFragment", "Error updating ETA: ${e.message}")
                }
            }
        }
    }
    
    private fun calculateEstimatedTime(distanceKm: Double, speedMs: Float): Int {
        val speedKmh = speedMs * 3.6 // Convert m/s to km/h
        return if (speedKmh > 0) {
            ((distanceKm / speedKmh) * 60).toInt() // Convert to minutes
        } else {
            (distanceKm * 2).toInt() // Fallback: assume 30 km/h average
        }.coerceAtLeast(1) // Minimum 1 minute
    }

    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0] / 1000.0 // Convert to kilometers
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation()
                    startLocationUpdates()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        
        // Mark fragment as inactive
        isFragmentActive = false
        
        // Stop simulation
        simulationHandler?.removeCallbacksAndMessages(null)
        simulationRunnable = null
        simulationHandler = null
        
        // Stop location updates
        fusedLocationClient.removeLocationUpdates { }
        locationUpdateJob?.cancel()
        
        // Stop real-time updates
        stopRealTimeUpdates()
        
        // Stop tracking service
        locationTrackingManager.stopTrackingDriver(driver.id)
        locationTrackingManager.stopTrackingTruck(towTruck.id)
        locationTrackingManager.stopService()
        
        _binding = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
} 