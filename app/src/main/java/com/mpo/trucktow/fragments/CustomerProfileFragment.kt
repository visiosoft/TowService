package com.mpo.trucktow.fragments

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mpo.trucktow.R
import com.mpo.trucktow.databinding.FragmentCustomerProfileBinding
import com.mpo.trucktow.adapters.PastRequestsAdapter
import com.mpo.trucktow.adapters.PaymentMethodsAdapter
import com.mpo.trucktow.models.PastRequest
import com.mpo.trucktow.models.PaymentMethod

class CustomerProfileFragment : Fragment() {

    private var _binding: FragmentCustomerProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var pastRequestsAdapter: PastRequestsAdapter
    private lateinit var paymentMethodsAdapter: PaymentMethodsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupLocationServices()
        setupRecyclerViews()
        setupClickListeners()
        loadUserData()
    }

    private fun setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        updateCurrentLocation()
    }

    private fun setupRecyclerViews() {
        // Setup Past Requests RecyclerView
        pastRequestsAdapter = PastRequestsAdapter(getMockPastRequests())
        binding.pastRequestsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = pastRequestsAdapter
        }

        // Setup Payment Methods RecyclerView
        paymentMethodsAdapter = PaymentMethodsAdapter(getMockPaymentMethods())
        binding.paymentMethodsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = paymentMethodsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.updateLocationButton.setOnClickListener {
            updateCurrentLocation()
        }

        binding.addPaymentMethodButton.setOnClickListener {
            showAddPaymentMethodDialog()
        }

        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Implement dark mode toggle
            Toast.makeText(context, "Dark mode: $isChecked", Toast.LENGTH_SHORT).show()
        }

        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Implement notifications toggle
            Toast.makeText(context, "Notifications: $isChecked", Toast.LENGTH_SHORT).show()
        }

        binding.languageButton.setOnClickListener {
            showLanguageSelectionDialog()
        }

        binding.contactSupportButton.setOnClickListener {
            // Implement contact support
            Toast.makeText(context, "Contact support clicked", Toast.LENGTH_SHORT).show()
        }

        binding.feedbackButton.setOnClickListener {
            showFeedbackDialog()
        }
    }

    private fun loadUserData() {
        // TODO: Load user data from your backend
        binding.fullNameText.text = "John Doe"
        binding.emailText.text = "john.doe@example.com"
        binding.phoneNumberInput.setText("+1234567890")
    }

    private fun updateCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Show loading indicator
            binding.updateLocationButton.isEnabled = false
            binding.currentLocationText.text = "Updating location..."
            
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    // Get address from coordinates
                    val geocoder = android.location.Geocoder(requireContext(), java.util.Locale.getDefault())
                    try {
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        addresses?.firstOrNull()?.let { address ->
                            val addressText = address.getAddressLine(0)
                            binding.currentLocationText.text = addressText
                            Toast.makeText(context, "Location updated!", Toast.LENGTH_SHORT).show()
                        } ?: run {
                            binding.currentLocationText.text = "Lat: ${location.latitude}, Lng: ${location.longitude}"
                            Toast.makeText(context, "Location updated!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        binding.currentLocationText.text = "Lat: ${location.latitude}, Lng: ${location.longitude}"
                        Toast.makeText(context, "Location updated!", Toast.LENGTH_SHORT).show()
                    }
                } ?: run {
                    binding.currentLocationText.text = "Unable to get current location"
                    Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
                
                // Re-enable button
                binding.updateLocationButton.isEnabled = true
            }.addOnFailureListener {
                binding.currentLocationText.text = "Failed to update location"
                Toast.makeText(context, "Failed to update location", Toast.LENGTH_SHORT).show()
                binding.updateLocationButton.isEnabled = true
            }
            } catch (e: SecurityException) {
                binding.currentLocationText.text = "Location permission denied"
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
                binding.updateLocationButton.isEnabled = true
            }
        } else {
            binding.currentLocationText.text = "Location permission required"
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
            // Request location permission
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1001
            )
        }
    }

    private fun showChangePasswordDialog() {
        // TODO: Implement change password dialog
        Toast.makeText(context, "Change password clicked", Toast.LENGTH_SHORT).show()
    }

    private fun showAddPaymentMethodDialog() {
        // TODO: Implement add payment method dialog
        Toast.makeText(context, "Add payment method clicked", Toast.LENGTH_SHORT).show()
    }

    private fun showLanguageSelectionDialog() {
        val languages = arrayOf("English", "Spanish", "French", "German")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Language")
            .setItems(languages) { _, which ->
                // Handle language selection
                Toast.makeText(context, "Selected: ${languages[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showFeedbackDialog() {
        // TODO: Implement feedback dialog
        Toast.makeText(context, "Feedback clicked", Toast.LENGTH_SHORT).show()
    }

    private fun getMockPastRequests(): List<PastRequest> {
        return listOf(
            PastRequest("1", "2024-02-20", "Flat tire", "Completed"),
            PastRequest("2", "2024-02-15", "Battery jump", "Completed"),
            PastRequest("3", "2024-02-10", "Engine trouble", "Completed")
        )
    }

    private fun getMockPaymentMethods(): List<PaymentMethod> {
        return listOf(
            PaymentMethod("1", "Visa ending in 1234", "Visa"),
            PaymentMethod("2", "Mastercard ending in 5678", "Mastercard")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 