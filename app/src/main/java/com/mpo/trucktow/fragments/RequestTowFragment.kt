package com.mpo.trucktow.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
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
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.mpo.trucktow.R
import com.mpo.trucktow.databinding.FragmentRequestTowBinding
import com.mpo.trucktow.services.DirectionsApiHelper
import kotlinx.coroutines.*

class RequestTowFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentRequestTowBinding? = null
    private val binding get() = _binding!!
    
    private var pickupMap: GoogleMap? = null
    private var dropMap: GoogleMap? = null
    private var pickupLocation: LatLng? = null
    private var dropLocation: LatLng? = null
    private var pickupMarker: com.google.android.gms.maps.model.Marker? = null
    private var dropMarker: com.google.android.gms.maps.model.Marker? = null
    private var routePolyline: Polyline? = null
    
    private val AUTOCOMPLETE_REQUEST_CODE_PICKUP = 1
    private val AUTOCOMPLETE_REQUEST_CODE_DROP = 2
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                enableMyLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                enableMyLocation()
            }
            else -> {
                Toast.makeText(requireContext(), "Location permission is required for this feature", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequestTowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Places
        if (!Places.isInitialized()) {
            val apiKey = requireContext().packageManager.getApplicationInfo(
                requireContext().packageName,
                PackageManager.GET_META_DATA
            ).metaData.getString("com.google.android.geo.API_KEY")
            Places.initialize(requireContext(), apiKey ?: "")
        }

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupMaps()
        setupLocationInputs()
        setupVehicleTypeChips()
        setupRequestButton()
        setupUpdateLocationButtons()
    }

    private fun setupMaps() {
        val pickupMapFragment = childFragmentManager.findFragmentById(R.id.pickupMap) as SupportMapFragment
        pickupMapFragment.getMapAsync { map ->
            pickupMap = map
            setupMap(map, true)
        }
        
        val dropMapFragment = childFragmentManager.findFragmentById(R.id.dropMap) as SupportMapFragment
        dropMapFragment.getMapAsync { map ->
            dropMap = map
            setupMap(map, false)
        }
    }
    
    private fun setupMap(map: GoogleMap, isPickupMap: Boolean) {
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false
        
        if (checkLocationPermission()) {
            if (isPickupMap) {
                enableMyLocation()
            }
        } else {
            requestLocationPermission()
        }
        
        // Add map click listener for manual location selection
        map.setOnMapClickListener { latLng ->
            if (isPickupMap) {
                setPickupLocation(latLng)
            } else {
                setDropLocation(latLng)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        // This method is required by OnMapReadyCallback but we handle maps in setupMaps()
        // The actual map setup is done in setupMap() method
    }

    private fun setupLocationInputs() {
        // Get current location first
        getCurrentLocation()
        
        // Setup pickup location input
        binding.pickupLocationEditText.setOnClickListener {
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(requireContext())
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE_PICKUP)
        }
        
        // Setup drop location input
        binding.dropLocationEditText.setOnClickListener {
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(requireContext())
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE_DROP)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            AUTOCOMPLETE_REQUEST_CODE_PICKUP -> {
                when (resultCode) {
                    AutocompleteActivity.RESULT_OK -> {
                        data?.let {
                            val place = Autocomplete.getPlaceFromIntent(it)
                            setPickupLocation(place.latLng)
                            binding.pickupLocationEditText.setText(place.address)
                        }
                    }
                    AutocompleteActivity.RESULT_ERROR -> {
                        val status = Autocomplete.getStatusFromIntent(data!!)
                        Toast.makeText(requireContext(), "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
                    }
                    AutocompleteActivity.RESULT_CANCELED -> {
                        // User canceled the operation
                    }
                }
            }
            AUTOCOMPLETE_REQUEST_CODE_DROP -> {
                when (resultCode) {
                    AutocompleteActivity.RESULT_OK -> {
                        data?.let {
                            val place = Autocomplete.getPlaceFromIntent(it)
                            setDropLocation(place.latLng)
                            binding.dropLocationEditText.setText(place.address)
                        }
                    }
                    AutocompleteActivity.RESULT_ERROR -> {
                        val status = Autocomplete.getStatusFromIntent(data!!)
                        Toast.makeText(requireContext(), "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
                    }
                    AutocompleteActivity.RESULT_CANCELED -> {
                        // User canceled the operation
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
    
    private fun setPickupLocation(location: LatLng) {
        pickupLocation = location
        pickupMarker?.remove()
        pickupMarker = pickupMap?.addMarker(
            MarkerOptions()
                .position(location)
                .title("Pickup Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
        pickupMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        calculateRoute()
    }
    
    private fun setDropLocation(location: LatLng) {
        dropLocation = location
        dropMarker?.remove()
        dropMarker = dropMap?.addMarker(
            MarkerOptions()
                .position(location)
                .title("Drop Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
        dropMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        calculateRoute()
    }
    
    private fun calculateRoute() {
        if (pickupLocation != null && dropLocation != null) {
            binding.routeStatusText.text = "Calculating route..."
            
            // Always create a route - either from API or fallback
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Try to get API route first
                    val apiResult = tryGetApiRoute()
                    
                    if (apiResult != null) {
                        // Use API result
                        displayRoute(apiResult.polylinePoints, apiResult.distanceText, apiResult.durationText, "API Route")
                    } else {
                        // Use fallback route
                        Log.d("RequestTowFragment", "API route failed, using fallback")
                        createAndDisplayFallbackRoute()
                    }
                    
                } catch (e: Exception) {
                    Log.e("RequestTowFragment", "Error in route calculation", e)
                    // Always provide a fallback route
                    createAndDisplayFallbackRoute()
                }
            }
        } else {
            Log.d("RequestTowFragment", "Cannot calculate route: pickup=${pickupLocation}, drop=${dropLocation}")
            binding.routeStatusText.text = "Select both locations to see route"
        }
    }
    
    private suspend fun tryGetApiRoute(): DirectionsApiHelper.DirectionsResult? {
        return try {
            // Try multiple methods to get API key
            var apiKey = ""
            
            // Method 1: Try from meta-data
            try {
                apiKey = requireContext().packageManager.getApplicationInfo(
                    requireContext().packageName,
                    PackageManager.GET_META_DATA
                ).metaData.getString("com.google.android.geo.API_KEY") ?: ""
            } catch (e: Exception) {
                Log.w("RequestTowFragment", "Failed to get API key from meta-data", e)
            }
            
            // Method 2: Try from resources
            if (apiKey.isEmpty()) {
                try {
                    apiKey = getString(R.string.google_maps_key)
                } catch (e: Exception) {
                    Log.w("RequestTowFragment", "Failed to get API key from resources", e)
                }
            }
            
            // Method 3: Use a test API key for development
            if (apiKey.isEmpty() || apiKey.contains("XXXXX")) {
                apiKey = "AIzaSyDeDuZnABhpolPxDiUoAvWOXIABQ-eGoKA"
                Log.d("RequestTowFragment", "Using development API key")
            }
            
            if (apiKey.isEmpty()) {
                Log.w("RequestTowFragment", "No API key available")
                return null
            }
            
            Log.d("RequestTowFragment", "Trying API route calculation")
            val result = DirectionsApiHelper.getRoute(pickupLocation!!, dropLocation!!, apiKey)
            
            if (result != null) {
                Log.d("RequestTowFragment", "API route successful: ${result.distanceText}, ${result.durationText}")
            } else {
                Log.w("RequestTowFragment", "API route returned null")
            }
            
            result
            
        } catch (e: Exception) {
            Log.e("RequestTowFragment", "Error in API route calculation", e)
            null
        }
    }
    
    private fun createAndDisplayFallbackRoute() {
        try {
            Log.d("RequestTowFragment", "Creating fallback route")
            
            // Calculate distance
            val distance = calculateDistance(pickupLocation!!, dropLocation!!)
            val distanceText = if (distance < 1) {
                "${(distance * 1000).toInt()}m"
            } else {
                "${String.format("%.1f", distance)}km"
            }
            
            // Estimate time (assuming 30 km/h average speed)
            val estimatedTimeMinutes = (distance * 2).toInt().coerceAtLeast(1)
            val timeText = "${estimatedTimeMinutes} mins"
            
            // Create curved route path
            val polylinePoints = createCurvedRoute(pickupLocation!!, dropLocation!!)
            
            // Display the route with cost calculation
            displayRouteWithCost(polylinePoints, distance, distanceText, timeText, "Fallback Route")
            
        } catch (e: Exception) {
            Log.e("RequestTowFragment", "Error creating fallback route", e)
            // Last resort - show error
            binding.routeStatusText.text = "Failed to create route"
            Toast.makeText(requireContext(), "Failed to create route", Toast.LENGTH_SHORT).show()
        }
    }
    

    
    private fun displayRoute(polylinePoints: List<LatLng>, distanceText: String, durationText: String, routeType: String) {
        try {
            // Draw polyline on pickup map
            routePolyline?.remove()
            routePolyline = pickupMap?.addPolyline(
                PolylineOptions()
                    .addAll(polylinePoints)
                    .color(resources.getColor(R.color.purple_700, null))
                    .width(8f)
            )
            
            // Calculate estimated cost
            val estimatedCost = calculateEstimatedCost(distanceText)
            
            // Update route information
            binding.distanceText.text = distanceText
            binding.estimatedTimeText.text = durationText
            binding.estimatedCostText.text = estimatedCost
            binding.routeStatusText.text = "✅ Route calculated ($routeType)"
            
            // Fit camera to show entire route
            val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                .include(pickupLocation!!)
                .include(dropLocation!!)
                .build()
            pickupMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            
            // Show success message
            Toast.makeText(requireContext(), "Route: $distanceText, $durationText, Cost: $estimatedCost", Toast.LENGTH_SHORT).show()
            
            Log.d("RequestTowFragment", "Route displayed successfully: $distanceText, $durationText, Cost: $estimatedCost")
            
        } catch (e: Exception) {
            Log.e("RequestTowFragment", "Error displaying route", e)
            binding.routeStatusText.text = "Error displaying route"
            Toast.makeText(requireContext(), "Error displaying route", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun calculateEstimatedCost(distanceText: String): String {
        return try {
            val ratePerKm = 3.0 // 3 AED per km
            
            // Parse distance from text (e.g., "2.5km" or "1500m")
            val distanceInKm = when {
                distanceText.contains("km") -> {
                    distanceText.replace("km", "").trim().toDouble()
                }
                distanceText.contains("m") -> {
                    distanceText.replace("m", "").trim().toDouble() / 1000.0
                }
                else -> {
                    // Try to parse as number, assume km
                    distanceText.toDoubleOrNull() ?: 0.0
                }
            }
            
            val estimatedCost = distanceInKm * ratePerKm
            
            // Format cost with 2 decimal places
            "%.2f AED".format(estimatedCost)
            
        } catch (e: Exception) {
            Log.e("RequestTowFragment", "Error calculating cost", e)
            "N/A"
        }
    }
    
    private fun displayRouteWithCost(polylinePoints: List<LatLng>, distanceInKm: Double, distanceText: String, durationText: String, routeType: String) {
        try {
            // Draw polyline on pickup map
            routePolyline?.remove()
            routePolyline = pickupMap?.addPolyline(
                PolylineOptions()
                    .addAll(polylinePoints)
                    .color(resources.getColor(R.color.purple_700, null))
                    .width(8f)
            )
            
            // Calculate estimated cost using actual distance value
            val ratePerKm = 3.0 // 3 AED per km
            val estimatedCost = "%.2f AED".format(distanceInKm * ratePerKm)
            
            // Update route information
            binding.distanceText.text = distanceText
            binding.estimatedTimeText.text = durationText
            binding.estimatedCostText.text = estimatedCost
            binding.routeStatusText.text = "✅ Route calculated ($routeType)"
            
            // Fit camera to show entire route
            val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                .include(pickupLocation!!)
                .include(dropLocation!!)
                .build()
            pickupMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            
            // Show success message
            Toast.makeText(requireContext(), "Route: $distanceText, $durationText, Cost: $estimatedCost", Toast.LENGTH_SHORT).show()
            
            Log.d("RequestTowFragment", "Route displayed successfully: $distanceText, $durationText, Cost: $estimatedCost")
            
        } catch (e: Exception) {
            Log.e("RequestTowFragment", "Error displaying route", e)
            binding.routeStatusText.text = "Error displaying route"
            Toast.makeText(requireContext(), "Error displaying route", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupVehicleTypeChips() {
        binding.vehicleTypeChipGroup.setOnCheckedChangeListener { _, _ ->
            // No need to update estimates anymore
        }
    }

    private fun setupRequestButton() {
        binding.requestButton.setOnClickListener {
            if (validateInputs()) {
                // Navigate to ongoing ride fragment with route information
                navigateToOngoingRide()
            }
        }
    }
    
    private fun navigateToOngoingRide() {
        // Navigate to ongoing ride fragment
        findNavController().navigate(R.id.ongoingRideFragment)
    }

    private fun setupUpdateLocationButtons() {
        binding.updateLocationButton.setOnClickListener {
            updateCurrentLocation()
        }
        
        binding.updateDropLocationButton.setOnClickListener {
            // For demo purposes, set a random drop location near the pickup
            pickupLocation?.let { pickup ->
                val randomLat = pickup.latitude + (Math.random() - 0.5) * 0.01
                val randomLng = pickup.longitude + (Math.random() - 0.5) * 0.01
                val randomDropLocation = LatLng(randomLat, randomLng)
                setDropLocation(randomDropLocation)
                
                // Get address for the random location
                val geocoder = android.location.Geocoder(requireContext(), java.util.Locale.getDefault())
                try {
                    val addresses = geocoder.getFromLocation(randomLat, randomLng, 1)
                    addresses?.firstOrNull()?.let { address ->
                        binding.dropLocationEditText.setText(address.getAddressLine(0))
                    }
                } catch (e: Exception) {
                    binding.dropLocationEditText.setText("Random Drop Location")
                }
            }
        }
        

    }

    private fun validateInputs(): Boolean {
        if (pickupLocation == null) {
            Toast.makeText(context, "Please select a pickup location", Toast.LENGTH_SHORT).show()
            return false
        }
        if (dropLocation == null) {
            Toast.makeText(context, "Please select a drop location", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.vehicleTypeChipGroup.checkedChipId == View.NO_ID) {
            Toast.makeText(context, "Please select a vehicle type", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
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
        if (checkLocationPermission()) {
            pickupMap?.isMyLocationEnabled = false // Disable default blue dot
            getCurrentLocation()
            startLocationUpdates()
        }
    }

    private fun getCurrentLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    setPickupLocation(currentLatLng)
                    
                    // Get address from coordinates
                    val geocoder = android.location.Geocoder(requireContext(), java.util.Locale.getDefault())
                    try {
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        addresses?.firstOrNull()?.let { address ->
                            val addressText = address.getAddressLine(0)
                            binding.pickupLocationEditText.setText(addressText)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } ?: run {
                    Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (checkLocationPermission()) {
            try {
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000) // Update every 5 seconds
                    .setMinUpdateDistanceMeters(10f) // Update if moved 10 meters
                    .build()
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            locationResult.lastLocation?.let { location ->
                                val currentLatLng = LatLng(location.latitude, location.longitude)
                                // Only update if user hasn't manually set pickup location
                                if (pickupLocation == null) {
                                    setPickupLocation(currentLatLng)
                                }
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

    private fun updateCurrentLocation() {
        if (checkLocationPermission()) {
            // Show loading indicator
            binding.updateLocationButton.isEnabled = false
            Toast.makeText(context, "Updating location...", Toast.LENGTH_SHORT).show()
            
            // Get fresh location
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    setPickupLocation(currentLatLng)
                    
                    // Move camera to new location
                    pickupMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                    
                    // Update address
                    val geocoder = android.location.Geocoder(requireContext(), java.util.Locale.getDefault())
                    try {
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        addresses?.firstOrNull()?.let { address ->
                            val addressText = address.getAddressLine(0)
                            binding.pickupLocationEditText.setText(addressText)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
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
        } else {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
            requestLocationPermission()
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

    private fun createCurvedRoute(origin: LatLng, destination: LatLng): List<LatLng> {
        val points = mutableListOf<LatLng>()
        points.add(origin)
        
        // Create intermediate points with curves
        val steps = 15
        for (i in 1 until steps) {
            val fraction = i.toFloat() / steps
            
            // Calculate base point
            val lat = origin.latitude + (destination.latitude - origin.latitude) * fraction
            val lng = origin.longitude + (destination.longitude - origin.longitude) * fraction
            
            // Add curve variation
            val curveVariation = 0.0002 * Math.sin(fraction * Math.PI * 2)
            val curvedLat = lat + curveVariation
            val curvedLng = lng + curveVariation
            
            points.add(LatLng(curvedLat, curvedLng))
        }
        
        points.add(destination)
        return points
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fusedLocationClient.removeLocationUpdates(object : LocationCallback() {}) // Stop location updates
        _binding = null
    }
} 