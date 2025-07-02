package com.mpo.trucktow.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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
        
        setupLocationTrackingManager()
        setupMap()
        setupDriverInfo()
        setupVehicleInfo()
        setupCommunicationButtons()
        setupCancelButton()
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
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
        // Load driver image using your preferred image loading library
        // Glide.with(this).load(driver.imageUrl).into(binding.driverImage)
    }

    private fun setupVehicleInfo() {
        binding.vehicleType.text = vehicle.model
        binding.vehicleNumber.text = "License: ${vehicle.licensePlate}"
        binding.vehicleColor.text = "Color: ${vehicle.color}"
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
        
        binding.updateLocationButton.setOnClickListener {
            updateCurrentLocation()
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
        driverLocation = location
        currentDriverSpeed = speed
        currentDriverHeading = heading
        lastUpdateTime = System.currentTimeMillis()
        
        updateMapMarkers()
        updateEstimatedArrivalTime()
        updateDriverStatus()
    }
    
    private fun updateTruckLocation(location: LatLng, speed: Float, heading: Float) {
        // Update truck location if needed
        updateMapMarkers()
        updateEstimatedArrivalTime()
    }
    
    private fun updateDriverStatus() {
        val speedKmh = (currentDriverSpeed * 3.6).toInt() // Convert m/s to km/h
        val timeAgo = getTimeAgo(lastUpdateTime)
        
        binding.driverStatus.text = "Speed: ${speedKmh} km/h • Updated: $timeAgo"
    }

    private fun updateMapMarkers() {
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
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            // Fetch and draw route
            fetchAndDrawRoute(userLocation!!, driverLocation!!)
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
                // Update ETA and distance in UI
                binding.estimatedArrivalTime.text = "Arriving in ${result.durationText}"
                binding.distanceInfo.text = result.distanceText + " away"
            }
        }
    }

    private fun updateEstimatedArrivalTime() {
        userLocation?.let { user ->
            driverLocation?.let { driver ->
                val distance = calculateDistance(user, driver)
                val estimatedTimeMinutes = calculateEstimatedTime(distance, currentDriverSpeed)
                binding.estimatedArrivalTime.text = "Arriving in $estimatedTimeMinutes mins"
                
                // Update distance info
                val distanceText = if (distance < 1) {
                    "${(distance * 1000).toInt()}m away"
                } else {
                    "${String.format("%.1f", distance)}km away"
                }
                binding.distanceInfo.text = distanceText
            }
        }
    }
    
    private fun calculateEstimatedTime(distanceKm: Double, speedMs: Float): Int {
        val speedKmh = speedMs * 3.6 // Convert m/s to km/h
        return if (speedKmh > 0) {
            ((distanceKm / speedKmh) * 60).toInt() // Convert to minutes
        } else {
            (distanceKm * 2).toInt() // Fallback: assume 30 km/h average
        }
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
    
    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            else -> "${diff / 3600000}h ago"
        }
    }

    private fun updateCurrentLocation() {
        if (checkLocationPermission()) {
            // Show loading indicator
            binding.updateLocationButton.isEnabled = false
            Toast.makeText(context, "Updating location...", Toast.LENGTH_SHORT).show()
            
            // Get fresh location with proper permission check
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    userLocation = LatLng(location.latitude, location.longitude)
                    
                    // Update marker position
                    userMarker?.position = userLocation!!
                    
                    // Update map markers and camera
                    updateMapMarkers()
                    updateEstimatedArrivalTime()
                    
                    Toast.makeText(context, "Location updated!", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
                
                // Re-enable button
                binding.updateLocationButton.isEnabled = true
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to update location", Toast.LENGTH_SHORT).show()
                binding.updateLocationButton.isEnabled = true
            }
            } catch (e: SecurityException) {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
                binding.updateLocationButton.isEnabled = true
            }
        } else {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
            requestLocationPermission()
        }
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
        fusedLocationClient.removeLocationUpdates { } // Stop location updates
        locationUpdateJob?.cancel()
        
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