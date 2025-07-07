package com.mpo.trucktow.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
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
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.mpo.trucktow.R
import com.mpo.trucktow.databinding.FragmentRequestTowBinding
import com.mpo.trucktow.services.DirectionsApiHelper
import com.mpo.trucktow.ui.CountdownDialogFragment
import kotlinx.coroutines.*

class RequestTowFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentRequestTowBinding? = null
    private val binding get() = _binding!!
    
    private var dropMap: GoogleMap? = null
    private var pickupLocation: LatLng? = null
    private var dropLocation: LatLng? = null
    private var dropMarker: com.google.android.gms.maps.model.Marker? = null
    private var routePolyline: Polyline? = null
    
    // Lists to store markers and polylines for cleanup
    private val markers = mutableListOf<com.google.android.gms.maps.model.Marker>()
    private val polylines = mutableListOf<Polyline>()
    
    // Countdown timer handler
    private var countdownHandler: Handler? = null
    private var countdownRunnable: Runnable? = null
    
    // Countdown state tracking
    private var isCountdownActive = false
    private var remainingTime = 0
    private val totalCountdownTime = 120 // 2 minutes in seconds
    
    // MediaPlayer for tick sound
    private var tickMediaPlayer: MediaPlayer? = null
    
    // Vibrator for haptic feedback
    private var vibrator: Vibrator? = null
    
    // Payment method selection
    private var selectedPaymentMethod: String? = null
    
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
        setupPaymentMethods()
        setupRequestButton()
        setupUpdateLocationButtons()
        checkForActiveCountdown()
    }

    private fun setupMaps() {
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
            enableMyLocation()
        } else {
            requestLocationPermission()
        }
        
        // Add map click listener for manual location selection
        map.setOnMapClickListener { latLng ->
            setDropLocation(latLng)
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
        
        // Setup vehicle type selection listeners
        setupVehicleTypeListeners()
    }

    private fun setupVehicleTypeListeners() {
        binding.carChip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                clearVehicleTypeError()
                updateRequestButtonState()
            }
        }
        
        binding.bikeChip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                clearVehicleTypeError()
                updateRequestButtonState()
            }
        }
        
        binding.truckChip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                clearVehicleTypeError()
                updateRequestButtonState()
            }
        }
    }
    
    private fun setupPaymentMethods() {
        // Card payment option
        binding.cardPaymentCard.setOnClickListener {
            selectPaymentMethod("card")
        }
        
        // Cash payment option
        binding.cashPaymentCard.setOnClickListener {
            selectPaymentMethod("cash")
        }
    }
    
    private fun selectPaymentMethod(method: String) {
        selectedPaymentMethod = method
        
        // Update visual selection
        when (method) {
            "card" -> {
                binding.cardPaymentCheck.visibility = View.VISIBLE
                binding.cashPaymentCheck.visibility = View.GONE
                binding.cardPaymentCard.strokeColor = requireContext().getColor(R.color.accent_blue)
                binding.cashPaymentCard.strokeColor = requireContext().getColor(R.color.divider)
                binding.cardPaymentCard.strokeWidth = 3
                binding.cashPaymentCard.strokeWidth = 1
            }
            "cash" -> {
                binding.cardPaymentCheck.visibility = View.GONE
                binding.cashPaymentCheck.visibility = View.VISIBLE
                binding.cashPaymentCard.strokeColor = requireContext().getColor(R.color.accent_green)
                binding.cardPaymentCard.strokeColor = requireContext().getColor(R.color.divider)
                binding.cashPaymentCard.strokeWidth = 3
                binding.cardPaymentCard.strokeWidth = 1
            }
        }
        
        // Clear payment method error
        binding.paymentMethodError.visibility = View.GONE
        
        // Update request button state
        updateRequestButtonState()
        
        // Show selection feedback
        Toast.makeText(requireContext(), "Selected: ${if (method == "card") "Card Payment" else "Cash Payment"}", Toast.LENGTH_SHORT).show()
    }
    
    private fun clearPaymentMethodError() {
        binding.paymentMethodError.visibility = View.GONE
    }
    
    private fun showPaymentMethodError() {
        binding.paymentMethodError.visibility = View.VISIBLE
        
        // Add shake animation to draw attention
        binding.cardPaymentCard.animate()
            .translationX(15f)
            .setDuration(150)
            .withEndAction {
                binding.cardPaymentCard.animate()
                    .translationX(-15f)
                    .setDuration(150)
                    .withEndAction {
                        binding.cardPaymentCard.animate()
                            .translationX(0f)
                            .setDuration(150)
                            .start()
                    }
                    .start()
            }
            .start()
    }
    
    private fun updateRequestButtonState() {
        val isReady = pickupLocation != null && dropLocation != null && getSelectedVehicleType() != null && selectedPaymentMethod != null
        
        // Always keep button enabled, but show different visual states
        binding.requestButton.isEnabled = true
        
        if (isReady) {
            binding.requestButton.alpha = 1.0f
            binding.requestButton.text = getString(R.string.request_tow_now_button)
            binding.routeStatusText.text = getString(R.string.ready_to_request)
            binding.routeStatusText.setTextColor(requireContext().getColor(R.color.accent_green))
        } else {
            // Button is still clickable but shows it's not ready
            binding.requestButton.alpha = 0.8f
            binding.requestButton.text = getString(R.string.request_tow_now_button)
            binding.routeStatusText.text = getString(R.string.select_locations_hint)
            binding.routeStatusText.setTextColor(requireContext().getColor(R.color.text_hint))
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
        calculateRoute()
        updateRequestButtonState()
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
        updateRequestButtonState()
    }
    
    private fun calculateRoute() {
        if (pickupLocation != null && dropLocation != null) {
            // Show calculating status
            binding.routeStatusText.text = getString(R.string.calculating_route)
            binding.routeStatusText.setTextColor(requireContext().getColor(R.color.accent_blue))
            
            // Clear previous route data
            binding.distanceText.text = "--"
            binding.estimatedTimeText.text = "--"
            binding.estimatedCostText.text = "--"
            
            // Always create a route - either from API or fallback
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Try to get API route first
                    val apiResult = tryGetApiRoute()
                    
                    if (apiResult != null) {
                        // Use API result
                        displayRoute(apiResult.polylinePoints, apiResult.distanceText, apiResult.durationText, "API Route")
                        binding.routeStatusText.text = getString(R.string.route_calculated)
                        binding.routeStatusText.setTextColor(requireContext().getColor(R.color.accent_green))
                    } else {
                        // Use fallback route
                        Log.d("RequestTowFragment", "API route failed, using fallback")
                        createAndDisplayFallbackRoute()
                        binding.routeStatusText.text = getString(R.string.route_calculated_offline)
                        binding.routeStatusText.setTextColor(requireContext().getColor(R.color.accent_orange))
                    }
                    
                } catch (e: Exception) {
                    Log.e("RequestTowFragment", "Error in route calculation", e)
                    // Always provide a fallback route
                    createAndDisplayFallbackRoute()
                    binding.routeStatusText.text = "✅ Route calculated (offline)"
                    binding.routeStatusText.setTextColor(requireContext().getColor(R.color.accent_orange))
                }
            }
        } else {
            Log.d("RequestTowFragment", "Cannot calculate route: pickup=${pickupLocation}, drop=${dropLocation}")
            binding.routeStatusText.text = getString(R.string.select_locations_hint)
            binding.routeStatusText.setTextColor(requireContext().getColor(R.color.text_hint))
            
            // Clear route data
            binding.distanceText.text = "--"
            binding.estimatedTimeText.text = "--"
            binding.estimatedCostText.text = "--"
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
            // Calculate estimated cost
            val estimatedCost = calculateEstimatedCost(distanceText)
            
            // Update route information
            binding.distanceText.text = distanceText
            binding.estimatedTimeText.text = durationText
            binding.estimatedCostText.text = estimatedCost
            
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
            // Calculate estimated cost using actual distance value
            val ratePerKm = 3.0 // 3 AED per km
            val estimatedCost = "%.2f AED".format(distanceInKm * ratePerKm)
            
            // Update route information
            binding.distanceText.text = distanceText
            binding.estimatedTimeText.text = durationText
            binding.estimatedCostText.text = estimatedCost
            
            Log.d("RequestTowFragment", "Route displayed successfully: $distanceText, $durationText, Cost: $estimatedCost")
            
        } catch (e: Exception) {
            Log.e("RequestTowFragment", "Error displaying route", e)
            binding.routeStatusText.text = "Error displaying route"
            Toast.makeText(requireContext(), "Error displaying route", Toast.LENGTH_SHORT).show()
        }
    }



    private fun setupRequestButton() {
        binding.requestButton.setOnClickListener {
            Log.d("RequestTowFragment", "Request button clicked")
            
            // Always run validation to show error messages
            val isValid = validateInputs()
            
            if (isValid) {
                Log.d("RequestTowFragment", "Validation passed, proceeding with request")
                // Show loading state
                showRequestLoadingState()
                
                // Simulate sending request to drivers (2 seconds)
                Handler(Looper.getMainLooper()).postDelayed({
                    // Show countdown after request is sent
                    startCountdownTimer()
                }, 2000)
            } else {
                Log.d("RequestTowFragment", "Validation failed, showing error messages")
                // Error messages are already shown in validateInputs()
            }
        }
    }
    
    // Test method to verify validation is working
    private fun testValidation() {
        Log.d("RequestTowFragment", "=== VALIDATION TEST ===")
        Log.d("RequestTowFragment", "Pickup location: $pickupLocation")
        Log.d("RequestTowFragment", "Drop location: $dropLocation")
        Log.d("RequestTowFragment", "Selected vehicle type: ${getSelectedVehicleType()}")
        Log.d("RequestTowFragment", "Car chip checked: ${binding.carChip.isChecked}")
        Log.d("RequestTowFragment", "Bike chip checked: ${binding.bikeChip.isChecked}")
        Log.d("RequestTowFragment", "Truck chip checked: ${binding.truckChip.isChecked}")
        Log.d("RequestTowFragment", "Selected payment method: ${getSelectedPaymentMethod()}")
        Log.d("RequestTowFragment", "=== END VALIDATION TEST ===")
    }
    

    
    private fun showRequestLoadingState() {
        // Disable the request button and show loading
        binding.requestButton.isEnabled = false
        binding.requestButton.text = "Sending Request..."
        binding.requestButton.setIconResource(android.R.drawable.ic_popup_sync)
        
        // Show progress indicator
        binding.routeStatusText.text = getString(R.string.sending_request)
        binding.routeStatusText.setTextColor(requireContext().getColor(R.color.accent_blue))
    }
    
    private fun startCountdownTimer() {
        // Show countdown overlay
        showCountdownOverlay()
        
        // Initialize MediaPlayer for tick sound and vibrator
        initializeTickSound()
        initializeVibrator()
        
        // Start 2 minute countdown (120 seconds)
        remainingTime = totalCountdownTime
        isCountdownActive = true
        countdownHandler = Handler(Looper.getMainLooper())
        countdownRunnable = object : Runnable {
            override fun run() {
                if (remainingTime > 0 && isAdded && !isDetached) {
                    updateCountdownText(remainingTime)
                    updateButtonWithRemainingTime(remainingTime)
                    playTickSound(remainingTime)
                    remainingTime--
                    countdownHandler?.postDelayed(this, 1000) // Update every second
                } else {
                    // Countdown finished, play final sound and show success
                    if (isAdded && !isDetached) {
                        isCountdownActive = false
                        playTickSound(0) // Play final sound when counter completes
                        Handler(Looper.getMainLooper()).postDelayed({
                            hideCountdownOverlay()
                            releaseTickSound()
                            releaseVibrator()
                            showRequestSuccess()
                        }, 1000) // Wait 1 second after final sound
                    }
                }
            }
        }
        countdownHandler?.post(countdownRunnable!!)
    }
    
    private fun showCountdownOverlay() {
        if (isAdded && !isDetached) {
            try {
                // Create and show a countdown dialog
                val countdownDialog = CountdownDialogFragment()
                countdownDialog.show(childFragmentManager, "CountdownDialog")
            } catch (e: Exception) {
                Log.e("RequestTowFragment", "Error showing countdown dialog", e)
            }
        }
    }
    
    private fun hideCountdownOverlay() {
        if (isAdded && !isDetached) {
            try {
                // Hide the countdown dialog but keep countdown running
                val countdownDialog = childFragmentManager.findFragmentByTag("CountdownDialog") as? CountdownDialogFragment
                countdownDialog?.dismiss()
                
                // Don't stop the countdown - let it continue in background
                // The button will show the remaining time
            } catch (e: Exception) {
                Log.e("RequestTowFragment", "Error hiding countdown dialog", e)
            }
        }
    }
    
    private fun updateCountdownText(secondsLeft: Int) {
        if (!isAdded || isDetached) return
        
        val minutes = secondsLeft / 60
        val seconds = secondsLeft % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)
        
        try {
            // Update the countdown dialog text and progress
            val countdownDialog = childFragmentManager.findFragmentByTag("CountdownDialog") as? CountdownDialogFragment
            countdownDialog?.updateTime(timeText)
            countdownDialog?.updateProgress(secondsLeft, totalCountdownTime)
        } catch (e: Exception) {
            Log.e("RequestTowFragment", "Error updating countdown text", e)
        }
    }
    
    private fun updateButtonWithRemainingTime(secondsLeft: Int) {
        if (!isAdded || isDetached) return
        
        val minutes = secondsLeft / 60
        val seconds = secondsLeft % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)
        
        try {
            binding.requestButton.text = "⏱️ $timeText remaining"
            binding.requestButton.setIconResource(android.R.drawable.ic_popup_sync)
            binding.requestButton.isEnabled = false
        } catch (e: Exception) {
            Log.e("RequestTowFragment", "Error updating button text", e)
        }
    }
    
    private fun initializeTickSound() {
        try {
            tickMediaPlayer = MediaPlayer.create(requireContext(), R.raw.tick)
            tickMediaPlayer?.isLooping = false
        } catch (e: Exception) {
            Log.e("RequestTowFragment", "Error initializing tick sound", e)
        }
    }
    

    
    private fun releaseTickSound() {
        try {
            tickMediaPlayer?.release()
            tickMediaPlayer = null
        } catch (e: Exception) {
            Log.e("RequestTowFragment", "Error releasing tick sound", e)
        }
    }
    
    private fun initializeVibrator() {
        try {
            vibrator = requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
        } catch (e: Exception) {
            Log.e("RequestTowFragment", "Error initializing vibrator", e)
        }
    }
    
    private fun playTickSound(timeLeft: Int) {
        try {
            // Play sound
            tickMediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.seekTo(0)
                player.start()
            }
            
            // Add vibration with counter pattern
            vibrator?.let { vib ->
                if (vib.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        // Create vibration pattern based on countdown progress
                        val vibrationPattern = createVibrationPattern(timeLeft)
                        vib.vibrate(VibrationEffect.createWaveform(vibrationPattern, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        // For older Android versions, use simple vibration
                        vib.vibrate(250)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RequestTowFragment", "Error playing tick sound or vibration", e)
        }
    }
    
    private fun createVibrationPattern(timeLeft: Int): LongArray {
        return when {
            timeLeft <= 10 && timeLeft > 0 -> {
                // Last 10 seconds: Very strong, rapid vibration pattern
                longArrayOf(0, 500, 100, 500, 100, 500, 100, 500)
            }
            timeLeft <= 30 && timeLeft > 10 -> {
                // Last 30 seconds: Strong vibration with pattern
                longArrayOf(0, 400, 100, 400, 100, 400)
            }
            timeLeft <= 60 && timeLeft > 30 -> {
                // Last minute: Medium vibration
                longArrayOf(0, 300, 100, 300)
            }
            timeLeft == 0 -> {
                // Countdown complete: Strong celebration pattern
                longArrayOf(0, 800, 200, 800, 200, 800, 200, 800)
            }
            else -> {
                // Normal countdown: Strong vibration
                longArrayOf(0, 250)
            }
        }
    }
    
    private fun releaseVibrator() {
        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e("RequestTowFragment", "Error releasing vibrator", e)
        }
    }
    

    
    private fun showRequestSuccess() {
        // Stop the countdown
        stopCountdown()
        
        // Show success message
        binding.routeStatusText.text = getString(R.string.tow_truck_found)
        binding.routeStatusText.setTextColor(requireContext().getColor(R.color.accent_green))
        
        // Update button to show success
        binding.requestButton.text = "Success!"
        binding.requestButton.setIconResource(android.R.drawable.ic_dialog_info)
        binding.requestButton.backgroundTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.accent_green))
        binding.requestButton.isEnabled = false
        
        // Show brief success toast
        Toast.makeText(requireContext(), getString(R.string.success_message), Toast.LENGTH_SHORT).show()
        
        // Navigate to ongoing ride after brief delay
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToOngoingRide()
        }, 1500)
    }
    
    private fun navigateToTowTruckSelection() {
        // This method is no longer used - replaced with better flow
    }
    
    private fun showCarTowTruckConnection() {
        try {
            // Get current car location (pickup location)
            val carLocation = pickupLocation ?: return
            
            // Generate a nearby tow truck location (for demo purposes)
            val towTruckLocation = generateNearbyTowTruckLocation(carLocation)
            
            // Calculate time for tow truck to reach car
            val timeToReach = calculateTimeToReach(carLocation, towTruckLocation)
            
            // Show connection info
            binding.routeStatusText.text = "🚗 Tow truck coming in $timeToReach"
            
            // Show success message
            Toast.makeText(requireContext(), "Tow truck dispatched! ETA: $timeToReach", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Log.e("RequestTowFragment", "Error showing car-tow truck connection", e)
            Toast.makeText(requireContext(), "Error showing connection", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun generateNearbyTowTruckLocation(carLocation: LatLng): LatLng {
        // Generate a tow truck location within 2-5 km of the car
        val distanceKm = (Math.random() * 3 + 2).toDouble() // 2-5 km
        val bearing = Math.random() * 360 // Random direction
        
        // Convert to radians
        val lat1 = Math.toRadians(carLocation.latitude)
        val lng1 = Math.toRadians(carLocation.longitude)
        val brng = Math.toRadians(bearing)
        
        // Calculate new position
        val lat2 = Math.asin(
            Math.sin(lat1) * Math.cos(distanceKm / 6371) +
            Math.cos(lat1) * Math.sin(distanceKm / 6371) * Math.cos(brng)
        )
        val lng2 = lng1 + Math.atan2(
            Math.sin(brng) * Math.sin(distanceKm / 6371) * Math.cos(lat1),
            Math.cos(distanceKm / 6371) - Math.sin(lat1) * Math.sin(lat2)
        )
        
        return LatLng(Math.toDegrees(lat2), Math.toDegrees(lng2))
    }
    
    private fun calculateTimeToReach(carLocation: LatLng, towTruckLocation: LatLng): String {
        val distance = calculateDistance(carLocation, towTruckLocation)
        val timeMinutes = (distance * 2).toInt().coerceAtLeast(2) // Minimum 2 minutes
        
        return when {
            timeMinutes < 60 -> "${timeMinutes} mins"
            else -> "${timeMinutes / 60}h ${timeMinutes % 60}m"
        }
    }
    
    private fun navigateToOngoingRide() {
        // Clear previous markers and polylines
        clearMapElements()
        
        // Navigate to ongoing ride fragment
        findNavController().navigate(R.id.ongoingRideFragment)
    }
    
    private fun clearMapElements() {
        // Remove all markers
        markers.forEach { it.remove() }
        markers.clear()
        
        // Remove all polylines
        polylines.forEach { it.remove() }
        polylines.clear()
        
        // Remove route polyline
        routePolyline?.remove()
        routePolyline = null
    }

    private fun setupUpdateLocationButtons() {
        binding.updateLocationButton.setOnClickListener {
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
        var isValid = true
        val missingFields = mutableListOf<String>()
        
        Log.d("RequestTowFragment", "Starting validation...")
        
        // Validate pickup location
        if (pickupLocation == null) {
            Log.d("RequestTowFragment", "Pickup location is null")
            showValidationError(binding.pickupLocationEditText, "Please select a pickup location")
            missingFields.add("Pickup Location")
            isValid = false
        } else {
            Log.d("RequestTowFragment", "Pickup location is valid")
            clearValidationError(binding.pickupLocationEditText)
        }
        
        // Validate drop location
        if (dropLocation == null) {
            Log.d("RequestTowFragment", "Drop location is null")
            showValidationError(binding.dropLocationEditText, "Please select a drop location")
            missingFields.add("Drop Location")
            isValid = false
        } else {
            Log.d("RequestTowFragment", "Drop location is valid")
            clearValidationError(binding.dropLocationEditText)
        }
        
        // Validate vehicle type selection
        val selectedVehicleType = getSelectedVehicleType()
        if (selectedVehicleType == null) {
            Log.d("RequestTowFragment", "Vehicle type is null")
            showVehicleTypeError()
            missingFields.add("Vehicle Type")
            isValid = false
        } else {
            Log.d("RequestTowFragment", "Vehicle type is valid: $selectedVehicleType")
            clearVehicleTypeError()
        }
        
        // Validate payment method selection
        if (selectedPaymentMethod == null) {
            Log.d("RequestTowFragment", "Payment method is null")
            showPaymentMethodError()
            missingFields.add("Payment Method")
            isValid = false
        } else {
            Log.d("RequestTowFragment", "Payment method is valid: $selectedPaymentMethod")
            clearPaymentMethodError()
        }
        
        // Show specific error message if validation fails
        if (!isValid) {
            Log.d("RequestTowFragment", "Validation failed. Missing fields: $missingFields")
            val errorMessage = buildMissingFieldsMessage(missingFields)
            Log.d("RequestTowFragment", "Error message: $errorMessage")
            
            // Show error message in status text
            try {
                binding.routeStatusText.text = errorMessage
                binding.routeStatusText.setTextColor(requireContext().getColor(R.color.red_500))
                Log.d("RequestTowFragment", "Updated routeStatusText successfully")
            } catch (e: Exception) {
                Log.e("RequestTowFragment", "Error updating routeStatusText", e)
            }
            
            // Show prominent toast message
            val toastMessage = "❌ Missing required fields: ${missingFields.joinToString(", ")}"
            Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
            Log.d("RequestTowFragment", "Showed toast: $toastMessage")
            
            // Add visual feedback to the button
            binding.requestButton.alpha = 0.7f
            binding.requestButton.text = "⚠️ Complete Required Fields"
            
            // Reset button after 3 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                binding.requestButton.alpha = 1.0f
                binding.requestButton.text = getString(R.string.request_tow_now_button)
            }, 3000)
            
        } else {
            Log.d("RequestTowFragment", "Validation passed")
        }
        
        return isValid
    }
    
    private fun buildMissingFieldsMessage(missingFields: List<String>): String {
        return when {
            missingFields.size == 1 -> "❌ Missing: ${missingFields.first()}"
            missingFields.size == 2 -> "❌ Missing: ${missingFields[0]} and ${missingFields[1]}"
            missingFields.size == 3 -> "❌ Missing: ${missingFields[0]}, ${missingFields[1]}, and ${missingFields[2]}"
            missingFields.size == 4 -> "❌ Missing: ${missingFields[0]}, ${missingFields[1]}, ${missingFields[2]}, and ${missingFields[3]}"
            else -> getString(R.string.please_complete_fields)
        }
    }
    
    private fun showValidationError(editText: com.google.android.material.textfield.TextInputEditText, message: String) {
        try {
            val parent = editText.parent.parent as? com.google.android.material.textfield.TextInputLayout
            parent?.error = message
            parent?.boxStrokeColor = requireContext().getColor(R.color.red_500)
            Log.d("RequestTowFragment", "Set validation error: $message")
        } catch (e: Exception) {
            Log.e("RequestTowFragment", "Error setting validation error", e)
        }
    }
    
    private fun clearValidationError(editText: com.google.android.material.textfield.TextInputEditText) {
        try {
            val parent = editText.parent.parent as? com.google.android.material.textfield.TextInputLayout
            parent?.error = null
            parent?.boxStrokeColor = requireContext().getColor(R.color.accent_blue)
            Log.d("RequestTowFragment", "Cleared validation error")
        } catch (e: Exception) {
            Log.e("RequestTowFragment", "Error clearing validation error", e)
        }
    }
    
    private fun showVehicleTypeError() {
        // Add a temporary error indicator
        binding.vehicleTypeChipGroup.alpha = 0.7f
        
        // Add a more prominent shake animation to draw attention
        binding.vehicleTypeChipGroup.animate()
            .translationX(15f)
            .setDuration(150)
            .withEndAction {
                binding.vehicleTypeChipGroup.animate()
                    .translationX(-15f)
                    .setDuration(150)
                    .withEndAction {
                        binding.vehicleTypeChipGroup.animate()
                            .translationX(15f)
                            .setDuration(150)
                            .withEndAction {
                                binding.vehicleTypeChipGroup.animate()
                                    .translationX(0f)
                                    .setDuration(150)
                                    .start()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
        
        // Add a red border effect
        binding.vehicleTypeChipGroup.setBackgroundColor(requireContext().getColor(R.color.red_500))
        binding.vehicleTypeChipGroup.alpha = 0.1f
        
        // Remove the red background after animation
        Handler(Looper.getMainLooper()).postDelayed({
            binding.vehicleTypeChipGroup.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }, 1000)
    }
    
    private fun clearVehicleTypeError() {
        binding.vehicleTypeChipGroup.alpha = 1.0f
        binding.vehicleTypeChipGroup.translationX = 0f
    }
    
    private fun getSelectedVehicleType(): String? {
        return when {
            binding.carChip.isChecked -> "Car"
            binding.bikeChip.isChecked -> "Bike"
            binding.truckChip.isChecked -> "Truck"
            else -> null
        }
    }
    
    private fun getSelectedPaymentMethod(): String? {
        return selectedPaymentMethod
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
            Toast.makeText(context, "Updating location...", Toast.LENGTH_SHORT).show()
            
            // Get fresh location
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    setPickupLocation(currentLatLng)
                    
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
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to update location", Toast.LENGTH_SHORT).show()
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

    private fun stopCountdown() {
        isCountdownActive = false
        countdownHandler?.removeCallbacksAndMessages(null)
        countdownHandler = null
        countdownRunnable = null
        releaseTickSound()
        releaseVibrator()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        stopCountdown()
        _binding = null
    }

    private fun checkForActiveCountdown() {
        // Check if there's an active countdown when returning to this fragment
        if (isCountdownActive && remainingTime > 0) {
            // Show remaining time on the button
            updateButtonWithRemainingTime(remainingTime)
            
            // Re-enable the countdown timer if it was stopped
            if (countdownHandler == null) {
                startCountdownTimer()
            }
        }
    }
} 