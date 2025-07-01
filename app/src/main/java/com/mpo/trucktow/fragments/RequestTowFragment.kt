package com.mpo.trucktow.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.mpo.trucktow.R
import com.mpo.trucktow.databinding.FragmentRequestTowBinding

class RequestTowFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentRequestTowBinding? = null
    private val binding get() = _binding!!
    
    private var map: GoogleMap? = null
    private var pickupLocation: LatLng? = null
    private var userMarker: com.google.android.gms.maps.model.Marker? = null
    private val AUTOCOMPLETE_REQUEST_CODE_PICKUP = 1
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

        setupMap()
        setupLocationInputs()
        setupVehicleTypeChips()
        setupRequestButton()
        setupUpdateLocationButton()
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.pickupMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupLocationInputs() {
        // Get current location first
        getCurrentLocation()
        
        // Still allow manual location selection
        binding.pickupLocationEditText.setOnClickListener {
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(requireContext())
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE_PICKUP)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE_PICKUP) {
            when (resultCode) {
                AutocompleteActivity.RESULT_OK -> {
                    data?.let {
                        val place = Autocomplete.getPlaceFromIntent(it)
                        pickupLocation = place.latLng
                        binding.pickupLocationEditText.setText(place.address)
                        updateMap()
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
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun setupVehicleTypeChips() {
        binding.vehicleTypeChipGroup.setOnCheckedChangeListener { _, _ ->
            // No need to update estimates anymore
        }
    }

    private fun setupRequestButton() {
        binding.requestButton.setOnClickListener {
            if (validateInputs()) {
                // TODO: Implement request submission
                Toast.makeText(context, "Request submitted successfully!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun setupUpdateLocationButton() {
        binding.updateLocationButton.setOnClickListener {
            updateCurrentLocation()
        }
    }

    private fun validateInputs(): Boolean {
        if (pickupLocation == null) {
            Toast.makeText(context, "Please select a pickup location", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.vehicleTypeChipGroup.checkedChipId == View.NO_ID) {
            Toast.makeText(context, "Please select a vehicle type", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.uiSettings?.isMyLocationButtonEnabled = false // Disable default location button
        if (checkLocationPermission()) {
            enableMyLocation()
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
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun enableMyLocation() {
        if (checkLocationPermission()) {
            map?.isMyLocationEnabled = false // Disable default blue dot
            getCurrentLocation()
            startLocationUpdates()
        }
    }

    private fun getCurrentLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    pickupLocation = currentLatLng
                    // Load and scale the custom car icon
                    val carBitmap = BitmapFactory.decodeResource(resources, R.drawable.car_icon)
                    val largeCarIcon = Bitmap.createScaledBitmap(carBitmap, 120, 120, false)
                    val currentMap = map
                    if (currentMap != null) {
                        if (userMarker == null) {
                            userMarker = currentMap.addMarker(
                                MarkerOptions()
                                    .position(currentLatLng)
                                    .title("You")
                                    .icon(BitmapDescriptorFactory.fromBitmap(largeCarIcon))
                                    .anchor(0.5f, 1.5f) // This moves icon slightly upward on map
                            )
                            // Move and zoom the map camera to your location (only on first location)
                            currentMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                        } else {
                            // Update existing marker position
                            userMarker?.position = currentLatLng
                        }
                    }
                    // Get address from coordinates
                    val geocoder = android.location.Geocoder(requireContext(), java.util.Locale.getDefault())
                    try {
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        addresses?.firstOrNull()?.let { address ->
                            val addressText = address.getAddressLine(0)
                            binding.pickupLocationEditText.setText(addressText)
                            updateMap()
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
                                pickupLocation = currentLatLng
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

    private fun updateCurrentLocation() {
        if (checkLocationPermission()) {
            // Show loading indicator
            binding.updateLocationButton.isEnabled = false
            Toast.makeText(context, "Updating location...", Toast.LENGTH_SHORT).show()
            
            // Get fresh location
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    pickupLocation = currentLatLng
                    
                    // Update marker position
                    userMarker?.position = currentLatLng
                    
                    // Move camera to new location
                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                    
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

    private fun updateMap() {
        pickupLocation?.let { location ->
            map?.clear()
            map?.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Pickup Location")
            )
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fusedLocationClient.removeLocationUpdates(object : LocationCallback() {}) // Stop location updates
        _binding = null
    }
} 