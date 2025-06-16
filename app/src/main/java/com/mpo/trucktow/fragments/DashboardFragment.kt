package com.mpo.trucktow.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
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
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, RequestTowFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
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
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    // TODO: Fetch nearby trucks based on current location
                    loadMockNearbyTrucks(currentLatLng)
                }
            }
        }
    }

    private fun loadMockNearbyTrucks(currentLocation: LatLng) {
        // Mock data for testing
        nearbyTrucks.clear()
        nearbyTrucks.addAll(listOf(
            TowTruck(
                id = "1",
                name = "Fast Tow Service",
                location = LatLng(currentLocation.latitude + 0.01, currentLocation.longitude + 0.01),
                distance = 0.5,
                rating = 4.5
            ),
            TowTruck(
                id = "2",
                name = "Reliable Towing",
                location = LatLng(currentLocation.latitude - 0.01, currentLocation.longitude - 0.01),
                distance = 0.8,
                rating = 4.2
            )
        ))
        
        nearbyTrucksAdapter.notifyDataSetChanged()
        
        // Add markers for each truck
        nearbyTrucks.forEach { truck ->
            map.addMarker(
                MarkerOptions()
                    .position(truck.location)
                    .title(truck.name)
                    .snippet("${truck.distance} km away")
            )
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