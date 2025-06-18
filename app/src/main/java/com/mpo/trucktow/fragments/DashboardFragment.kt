package com.mpo.trucktow.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.mpo.trucktow.R
import com.mpo.trucktow.adapters.NearbyTrucksAdapter
import com.mpo.trucktow.databinding.FragmentDashboardBinding
import com.mpo.trucktow.models.TowTruck

class DashboardFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var nearbyTrucksAdapter: NearbyTrucksAdapter
    
    private val nearbyTrucks = mutableListOf<TowTruck>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupMap()
        setupRecyclerView()
        setupRequestButton()
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupRecyclerView() {
        nearbyTrucksAdapter = NearbyTrucksAdapter(nearbyTrucks) { truck ->
            // Handle truck selection
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(truck.location, 15f))
        }
        
        binding.nearbyTrucksRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = nearbyTrucksAdapter
        }
    }

    private fun setupRequestButton() {
        binding.requestButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_request_tow)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (checkLocationPermission()) {
            enableMyLocation()
        } else {
            requestLocationPermission()
        }

        // Enable zoom controls and my location button
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isMyLocationButtonEnabled = true
            isCompassEnabled = true
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
            map.isMyLocationEnabled = true
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        // Load nearby trucks after getting current location
                        loadMockNearbyTrucks(currentLatLng)
                    } ?: run {
                        // If last location is null, request current location
                        requestCurrentLocation()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun requestCurrentLocation() {
        try {
            val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
                priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 0
                fastestInterval = 0
                numUpdates = 1
            }

            fusedLocationClient.requestLocationUpdates(locationRequest,
                object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                        val location = locationResult.lastLocation
                        if (location != null) {
                            val currentLatLng = LatLng(location.latitude, location.longitude)
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                            loadMockNearbyTrucks(currentLatLng)
                        }
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                },
                null
            )
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "Location permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMockNearbyTrucks(currentLocation: LatLng) {
        // Mock data for testing with 5 realistic tow trucks
        nearbyTrucks.clear()
        nearbyTrucks.addAll(listOf(
            TowTruck(
                id = "1",
                name = "Mike's Emergency Towing",
                location = LatLng(currentLocation.latitude + 0.008, currentLocation.longitude + 0.006),
                distance = 0.8,
                rating = 4.7,
                isAvailable = true,
                vehicleType = "Flatbed Tow Truck",
                phoneNumber = "+1234567890"
            ),
            TowTruck(
                id = "2",
                name = "Quick Response Towing",
                location = LatLng(currentLocation.latitude - 0.012, currentLocation.longitude + 0.009),
                distance = 1.2,
                rating = 4.3,
                isAvailable = true,
                vehicleType = "Wheel Lift Tow Truck",
                phoneNumber = "+1987654321"
            ),
            TowTruck(
                id = "3",
                name = "Reliable Roadside Rescue",
                location = LatLng(currentLocation.latitude + 0.015, currentLocation.longitude - 0.011),
                distance = 1.8,
                rating = 4.9,
                isAvailable = true,
                vehicleType = "Heavy Duty Tow Truck",
                phoneNumber = "+1555123456"
            ),
            TowTruck(
                id = "4",
                name = "24/7 Express Towing",
                location = LatLng(currentLocation.latitude - 0.007, currentLocation.longitude - 0.013),
                distance = 1.5,
                rating = 4.1,
                isAvailable = true,
                vehicleType = "Standard Tow Truck",
                phoneNumber = "+1777888999"
            ),
            TowTruck(
                id = "5",
                name = "Premium Towing Services",
                location = LatLng(currentLocation.latitude + 0.003, currentLocation.longitude + 0.018),
                distance = 2.1,
                rating = 4.6,
                isAvailable = true,
                vehicleType = "Flatbed Tow Truck",
                phoneNumber = "+1444333222"
            )
        ))
        
        nearbyTrucksAdapter.notifyDataSetChanged()
        
        // Add markers for each truck with detailed information and custom colors
        val markerColors = listOf(
            BitmapDescriptorFactory.HUE_RED,
            BitmapDescriptorFactory.HUE_BLUE,
            BitmapDescriptorFactory.HUE_GREEN,
            BitmapDescriptorFactory.HUE_ORANGE,
            BitmapDescriptorFactory.HUE_VIOLET
        )
        
        nearbyTrucks.forEachIndexed { index, truck ->
            map.addMarker(
                MarkerOptions()
                    .position(truck.location)
                    .title(truck.name)
                    .snippet("${truck.distance} km away • ⭐ ${truck.rating} • ${truck.vehicleType}")
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColors[index]))
            )
        }
        
        // Show info message about dummy data
        Toast.makeText(requireContext(), "Showing 5 dummy tow trucks for testing", Toast.LENGTH_SHORT).show()
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
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
} 