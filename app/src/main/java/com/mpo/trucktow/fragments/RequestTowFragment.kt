package com.mpo.trucktow.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
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
            Places.initialize(requireContext(), "YOUR_API_KEY") // Replace with your actual API key
        }

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupMap()
        setupLocationInputs()
        setupVehicleTypeChips()
        setupRequestButton()
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.pickupMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupLocationInputs() {
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
        binding.vehicleTypeChipGroup.setOnCheckedChangeListener { group, checkedId ->
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
            map?.isMyLocationEnabled = true
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    map?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
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
        _binding = null
    }
} 