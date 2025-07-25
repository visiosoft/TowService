package com.mpo.trucktow.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.card.MaterialCardView
import com.mpo.trucktow.R
import com.mpo.trucktow.models.TowTruck
import com.mpo.trucktow.ui.TowTruckDetailsDialog

class HomeFragment : Fragment(), OnMapReadyCallback {
    private var _map: GoogleMap? = null
    private val map get() = _map!!
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private var userMarker: com.google.android.gms.maps.model.Marker? = null
    private var truckMarkers = mutableListOf<com.google.android.gms.maps.model.Marker>()
    private var trucksAdded = false
    private var nearbyTrucks = mutableListOf<TowTruck>()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted
                enableMyLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted
                enableMyLocation()
            }
            else -> {
                // No location access granted
                Toast.makeText(context, "Location permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            // Initialize map
            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            mapFragment?.getMapAsync(this)

            // Initialize location services
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

            // Setup UI elements
            view.findViewById<MaterialCardView>(R.id.supportButtonCard)?.setOnClickListener {
                // Add visual feedback with animation
                val supportCard = view.findViewById<MaterialCardView>(R.id.supportButtonCard)
                supportCard?.animate()
                    ?.scaleX(0.95f)
                    ?.scaleY(0.95f)
                    ?.setDuration(100)
                    ?.withEndAction {
                        supportCard.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                    }
                    ?.start()

                // Show enhanced support dialog
                showSupportDialog()
            }

            view.findViewById<FloatingActionButton>(R.id.locationUpdateButton)?.setOnClickListener {
                updateCurrentLocation()
            }

            // Request location permission
            checkLocationPermission()
        } catch (e: Exception) {
            Toast.makeText(context, "Error initializing map: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        try {
            _map = googleMap
            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = false // Disable default location button

            // Set up marker click listener
            googleMap.setOnMarkerClickListener { marker ->
                // Find the truck corresponding to this marker
                val truck = nearbyTrucks.find { truck ->
                    truck.location.latitude == marker.position.latitude && 
                    truck.location.longitude == marker.position.longitude
                }
                
                truck?.let {
                    showTruckDetailsDialog(it)
                    return@setOnMarkerClickListener true // Consume the click
                }
                
                false // Don't consume the click
            }

            if (hasLocationPermission()) {
                enableMyLocation()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error setting up map: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkLocationPermission() {
        when {
            hasLocationPermission() -> {
                enableMyLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(context, "Location permission is required for this feature", Toast.LENGTH_LONG).show()
                requestLocationPermission()
            }
            else -> {
                requestLocationPermission()
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun enableMyLocation() {
        try {
            if (hasLocationPermission() && _map != null) {
                map.isMyLocationEnabled = false // Disable default blue dot
                getCurrentLocation()
                startLocationUpdates()
            } else {
                requestLocationPermission()
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Location permission is required", Toast.LENGTH_LONG).show()
            requestLocationPermission()
        }
    }

    private fun getCurrentLocation() {
        if (hasLocationPermission()) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        currentLocation = it
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        
                        // Load and scale the custom car icon
                        val carBitmap = BitmapFactory.decodeResource(resources, R.drawable.car_icon)
                        val largeCarIcon = Bitmap.createScaledBitmap(carBitmap, 120, 120, false)

                        // Add or update marker with custom car icon
                        if (userMarker == null) {
                            userMarker = map.addMarker(
                                MarkerOptions()
                                    .position(currentLatLng)
                                    .title("You")
                                    .icon(BitmapDescriptorFactory.fromBitmap(largeCarIcon))
                                    .anchor(0.5f, 1.5f) // This moves icon slightly upward on map
                            )
                            // Move and zoom the map camera to your location (only on first location)
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                        } else {
                            // Update existing marker position
                            userMarker?.position = currentLatLng
                        }
                        
                        // Only add trucks once
                        if (!trucksAdded) {
                            findNearbyTrucks()
                        }
                    } ?: run {
                        Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {
                Toast.makeText(context, "Location permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startLocationUpdates() {
        if (hasLocationPermission()) {
            try {
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000) // Update every 5 seconds
                    .setMinUpdateDistanceMeters(10f) // Update if moved 10 meters
                    .build()

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            locationResult.lastLocation?.let { location ->
                                currentLocation = location
                                val currentLatLng = LatLng(location.latitude, location.longitude)
                                
                                // Update existing marker position
                                userMarker?.position = currentLatLng
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

    private fun findNearbyTrucks() {
        try {
            // Clear any existing truck markers
            truckMarkers.forEach { it.remove() }
            truckMarkers.clear()
            nearbyTrucks.clear()
            
            // TODO: Implement API call to find nearby tow trucks
            // For now, we'll add some dummy data with 5 realistic tow trucks near your location
            val dummyTrucks = listOf(
                TowTruck(
                    id = "1",
                    name = "Mike's Emergency Towing",
                    phoneNumber = "+1234567890",
                    location = LatLng(
                        currentLocation?.latitude?.plus(0.005) ?: 0.0,
                        currentLocation?.longitude?.plus(0.004) ?: 0.0
                    ),
                    distance = 0.5,
                    rating = 4.7,
                    isAvailable = true,
                    vehicleType = "Flatbed Tow Truck"
                ),
                TowTruck(
                    id = "2",
                    name = "Quick Response Towing",
                    phoneNumber = "+1987654321",
                    location = LatLng(
                        currentLocation?.latitude?.minus(0.006) ?: 0.0,
                        currentLocation?.longitude?.plus(0.005) ?: 0.0
                    ),
                    distance = 0.8,
                    rating = 4.3,
                    isAvailable = true,
                    vehicleType = "Wheel Lift Tow Truck"
                ),
                TowTruck(
                    id = "3",
                    name = "Reliable Roadside Rescue",
                    phoneNumber = "+1555123456",
                    location = LatLng(
                        currentLocation?.latitude?.plus(0.007) ?: 0.0,
                        currentLocation?.longitude?.minus(0.006) ?: 0.0
                    ),
                    distance = 0.9,
                    rating = 4.9,
                    isAvailable = true,
                    vehicleType = "Heavy Duty Tow Truck"
                ),
                TowTruck(
                    id = "4",
                    name = "24/7 Express Towing",
                    phoneNumber = "+1777888999",
                    location = LatLng(
                        currentLocation?.latitude?.minus(0.004) ?: 0.0,
                        currentLocation?.longitude?.minus(0.007) ?: 0.0
                    ),
                    distance = 0.7,
                    rating = 4.1,
                    isAvailable = true,
                    vehicleType = "Standard Tow Truck"
                ),
                TowTruck(
                    id = "5",
                    name = "Premium Towing Services",
                    phoneNumber = "+1444333222",
                    location = LatLng(
                        currentLocation?.latitude?.plus(0.003) ?: 0.0,
                        currentLocation?.longitude?.plus(0.008) ?: 0.0
                    ),
                    distance = 0.6,
                    rating = 4.6,
                    isAvailable = true,
                    vehicleType = "Flatbed Tow Truck"
                )
            )

            // Load and scale the custom tow truck icon
            val towTruckBitmap = BitmapFactory.decodeResource(resources, R.drawable.tow_truck_icon)
            val largeTowTruck = Bitmap.createScaledBitmap(towTruckBitmap, 120, 120, false)
            
            // Store trucks in the list and add markers
            nearbyTrucks.addAll(dummyTrucks)
            
            dummyTrucks.forEach { truck ->
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(truck.location)
                        .title(truck.name)
                        .snippet("${truck.distance} km away • ⭐ ${truck.rating} • ${truck.vehicleType}")
                        .icon(BitmapDescriptorFactory.fromBitmap(largeTowTruck))
                        .anchor(0.5f, 1.5f) // This moves icon slightly upward on map
                )
                marker?.let { truckMarkers.add(it) }
            }
            
            trucksAdded = true
            // Show info message about dummy data
            Toast.makeText(context, "Showing 5 dummy tow trucks for testing", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error showing nearby trucks: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showTruckDetailsDialog(truck: TowTruck) {
        val dialog = TowTruckDetailsDialog.newInstance(truck) { selectedTruck ->
            // Handle reserve button click
            Toast.makeText(context, "Reserving ${selectedTruck.name}...", Toast.LENGTH_SHORT).show()
            // Navigate to request tow fragment with selected truck
            findNavController().navigate(R.id.action_home_to_request_tow)
        }
        dialog.show(childFragmentManager, "TruckDetailsDialog")
    }

    private fun updateCurrentLocation() {
        if (hasLocationPermission()) {
            // Show loading indicator
            view?.findViewById<FloatingActionButton>(R.id.locationUpdateButton)?.isEnabled = false
            Toast.makeText(context, "Updating location...", Toast.LENGTH_SHORT).show()
            
            // Get fresh location with proper permission check
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = it
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    
                    // Update marker position
                    userMarker?.position = currentLatLng
                    
                    // Move camera to new location
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                    
                    Toast.makeText(context, "Location updated!", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
                
                // Re-enable button
                view?.findViewById<FloatingActionButton>(R.id.locationUpdateButton)?.isEnabled = true
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to update location", Toast.LENGTH_SHORT).show()
                view?.findViewById<FloatingActionButton>(R.id.locationUpdateButton)?.isEnabled = true
            }
            } catch (e: SecurityException) {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
                view?.findViewById<FloatingActionButton>(R.id.locationUpdateButton)?.isEnabled = true
            }
        } else {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
            requestLocationPermission()
        }
    }

    private fun showSupportDialog() {
        val options = arrayOf("📞 Call Support", "💬 Live Chat", "📧 Email Support", "❓ FAQ")
        
        AlertDialog.Builder(requireContext())
            .setTitle("🛟 Support Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Call Support
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:+1800SUPPORT")
                        }
                        startActivity(intent)
                    }
                    1 -> {
                        // Live Chat
                        Toast.makeText(context, "💬 Live chat feature coming soon!", Toast.LENGTH_LONG).show()
                    }
                    2 -> {
                        // Email Support
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@trucktow.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Support Request")
                        }
                        startActivity(intent)
                    }
                    3 -> {
                        // FAQ
                        Toast.makeText(context, "📚 FAQ section coming soon!", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fusedLocationClient.removeLocationUpdates { } // Stop location updates
        _map = null
    }
} 