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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mpo.trucktow.R
import com.mpo.trucktow.models.TowTruck

class HomeFragment : Fragment(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
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

        // Initialize map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Setup UI elements
        view.findViewById<FloatingActionButton>(R.id.profileButton).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_profile)
        }

        view.findViewById<MaterialButton>(R.id.requestTowButton).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_request_tow)
        }

        // Request location permission
        checkLocationPermission()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        if (hasLocationPermission()) {
            enableMyLocation()
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
        if (hasLocationPermission()) {
            map.isMyLocationEnabled = true
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (hasLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = it
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    findNearbyTrucks()
                }
            }
        }
    }

    private fun findNearbyTrucks() {
        // TODO: Implement API call to find nearby tow trucks
        // For now, we'll add some dummy data
        val dummyTrucks = listOf(
            TowTruck(
                id = "1",
                name = "John Doe",
                phoneNumber = "+1234567890",
                location = LatLng(
                    currentLocation?.latitude?.plus(0.01) ?: 0.0,
                    currentLocation?.longitude?.plus(0.01) ?: 0.0
                ),
                distance = 1.5,
                rating = 4.5,
                isAvailable = true
            ),
            TowTruck(
                id = "2",
                name = "Jane Smith",
                phoneNumber = "+1987654321",
                location = LatLng(
                    currentLocation?.latitude?.minus(0.01) ?: 0.0,
                    currentLocation?.longitude?.minus(0.01) ?: 0.0
                ),
                distance = 2.0,
                rating = 4.8,
                isAvailable = true
            )
        )

        // Add markers for each truck
        dummyTrucks.forEach { truck ->
            map.addMarker(
                MarkerOptions()
                    .position(truck.location)
                    .title("Tow Truck ${truck.name}")
                    .snippet("Driver: ${truck.name}")
            )
        }
    }
} 