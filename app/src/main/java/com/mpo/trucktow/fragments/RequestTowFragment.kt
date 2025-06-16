package com.mpo.trucktow.fragments

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import java.text.SimpleDateFormat
import java.util.*

class RequestTowFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentRequestTowBinding? = null
    private val binding get() = _binding!!
    
    private var map: GoogleMap? = null
    private var pickupLocation: LatLng? = null
    
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
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
        setupTowingTypeChips()
        setupScheduleOptions()
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
                        updateEstimate()
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
            updateEstimate()
        }
    }

    private fun setupTowingTypeChips() {
        binding.towingTypeChipGroup.setOnCheckedChangeListener { group, checkedId ->
            updateEstimate()
        }
    }

    private fun setupScheduleOptions() {
        binding.scheduleChipGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.nowChip -> {
                    binding.scheduleTimeLayout.visibility = View.GONE
                }
                R.id.laterChip -> {
                    binding.scheduleTimeLayout.visibility = View.VISIBLE
                    showDateTimePicker()
                }
            }
        }

        binding.scheduleTimeEditText.setOnClickListener {
            showDateTimePicker()
        }
    }

    private fun showDateTimePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                showTimePicker()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                updateScheduleTimeDisplay()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun updateScheduleTimeDisplay() {
        val dateTime = "${dateFormatter.format(calendar.time)} at ${timeFormatter.format(calendar.time)}"
        binding.scheduleTimeEditText.setText(dateTime)
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

    private fun updateEstimate() {
        if (pickupLocation != null) {
            val baseFare = 50.0 // Base fare
            val vehicleMultiplier = when (binding.vehicleTypeChipGroup.checkedChipId) {
                R.id.carChip -> 1.0
                R.id.bikeChip -> 0.8
                R.id.truckChip -> 1.5
                else -> 1.0
            }
            val towingMultiplier = when (binding.towingTypeChipGroup.checkedChipId) {
                R.id.flatbedChip -> 1.2
                R.id.hookAndChainChip -> 1.0
                R.id.wheelLiftChip -> 1.1
                else -> 1.0
            }
            
            val totalFare = baseFare * vehicleMultiplier * towingMultiplier
            val estimatedTime = 30 // Base time in minutes
            
            binding.estimatedFareTextView.text = "Estimated Fare: $${String.format("%.2f", totalFare)}"
            binding.estimatedTimeTextView.text = "Estimated Time: $estimatedTime min"
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
        if (binding.towingTypeChipGroup.checkedChipId == View.NO_ID) {
            Toast.makeText(context, "Please select a towing type", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.scheduleChipGroup.checkedChipId == R.id.laterChip && 
            binding.scheduleTimeEditText.text.isNullOrEmpty()) {
            Toast.makeText(context, "Please select a schedule time", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun enableMyLocation() {
        try {
            map?.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    pickupLocation = currentLatLng
                    updateMap()
                    
                    // Get address from coordinates
                    val geocoder = android.location.Geocoder(requireContext(), Locale.getDefault())
                    try {
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        addresses?.firstOrNull()?.let { address ->
                            val addressText = address.getAddressLine(0)
                            binding.pickupLocationEditText.setText(addressText)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "Location permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMap() {
        pickupLocation?.let { latLng ->
            map?.clear()
            map?.addMarker(MarkerOptions().position(latLng).title("Pickup Location"))
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        checkLocationPermission()
        
        // Enable zoom controls
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        
        googleMap.setOnMapClickListener { latLng ->
            pickupLocation = latLng
            updateMap()
            
            // Get address from coordinates
            val geocoder = android.location.Geocoder(requireContext(), Locale.getDefault())
            try {
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                addresses?.firstOrNull()?.let { address ->
                    val addressText = address.getAddressLine(0)
                    binding.pickupLocationEditText.setText(addressText)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 