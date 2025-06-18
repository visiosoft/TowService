package com.mpo.trucktow.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mpo.trucktow.R
import com.mpo.trucktow.models.TowTruck

class HomeFragment : Fragment(), OnMapReadyCallback {
    private var _map: GoogleMap? = null
    private val map get() = _map!!
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private val nearbyTrucks = mutableListOf<TowTruck>()

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
            view.findViewById<FloatingActionButton>(R.id.profileButton)?.setOnClickListener {
                findNavController().navigate(R.id.action_home_to_profile)
            }

            view.findViewById<MaterialButton>(R.id.requestTowButton)?.setOnClickListener {
                findNavController().navigate(R.id.action_home_to_request_tow)
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
            map.uiSettings.isMyLocationButtonEnabled = true

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
                map.isMyLocationEnabled = true
                getCurrentLocation()
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
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        findNearbyTrucks()
                    }
                }
            } catch (e: SecurityException) {
                Toast.makeText(context, "Location permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun findNearbyTrucks() {
        try {
            // TODO: Implement API call to find nearby tow trucks
            // For now, we'll add some dummy data with 5 realistic tow trucks
            val dummyTrucks = listOf(
                TowTruck(
                    id = "1",
                    name = "Mike's Emergency Towing",
                    phoneNumber = "+1234567890",
                    location = LatLng(
                        currentLocation?.latitude?.plus(0.008) ?: 0.0,
                        currentLocation?.longitude?.plus(0.006) ?: 0.0
                    ),
                    distance = 0.8,
                    rating = 4.7,
                    isAvailable = true,
                    vehicleType = "Flatbed Tow Truck"
                ),
                TowTruck(
                    id = "2",
                    name = "Quick Response Towing",
                    phoneNumber = "+1987654321",
                    location = LatLng(
                        currentLocation?.latitude?.minus(0.012) ?: 0.0,
                        currentLocation?.longitude?.plus(0.009) ?: 0.0
                    ),
                    distance = 1.2,
                    rating = 4.3,
                    isAvailable = true,
                    vehicleType = "Wheel Lift Tow Truck"
                ),
                TowTruck(
                    id = "3",
                    name = "Reliable Roadside Rescue",
                    phoneNumber = "+1555123456",
                    location = LatLng(
                        currentLocation?.latitude?.plus(0.015) ?: 0.0,
                        currentLocation?.longitude?.minus(0.011) ?: 0.0
                    ),
                    distance = 1.8,
                    rating = 4.9,
                    isAvailable = true,
                    vehicleType = "Heavy Duty Tow Truck"
                ),
                TowTruck(
                    id = "4",
                    name = "24/7 Express Towing",
                    phoneNumber = "+1777888999",
                    location = LatLng(
                        currentLocation?.latitude?.minus(0.007) ?: 0.0,
                        currentLocation?.longitude?.minus(0.013) ?: 0.0
                    ),
                    distance = 1.5,
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
                        currentLocation?.longitude?.plus(0.018) ?: 0.0
                    ),
                    distance = 2.1,
                    rating = 4.6,
                    isAvailable = true,
                    vehicleType = "Flatbed Tow Truck"
                )
            )

            // Add markers for each truck with more detailed information and custom icons
            val markerColors = listOf(
                BitmapDescriptorFactory.HUE_RED,
                BitmapDescriptorFactory.HUE_BLUE,
                BitmapDescriptorFactory.HUE_GREEN,
                BitmapDescriptorFactory.HUE_ORANGE,
                BitmapDescriptorFactory.HUE_VIOLET
            )
            
            dummyTrucks.forEachIndexed { index, truck ->
                map.addMarker(
                    MarkerOptions()
                        .position(truck.location)
                        .title(truck.name)
                        .snippet("${truck.distance} km away • ⭐ ${truck.rating} • ${truck.vehicleType}")
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColors[index]))
                )
            }
            
            // Show info message about dummy data
            Toast.makeText(context, "Showing 5 dummy tow trucks for testing", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error showing nearby trucks: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _map = null
    }
} 