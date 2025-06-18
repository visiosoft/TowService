package com.mpo.trucktow.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mpo.trucktow.R
import com.mpo.trucktow.databinding.FragmentProfileBinding
import com.mpo.trucktow.models.User
import com.mpo.trucktow.models.Vehicle
import com.mpo.trucktow.SessionManager

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    // Mock user data - Replace with actual user data from your backend
    private val user = User(
        id = "1",
        name = "John Doe",
        email = "john.doe@example.com",
        phoneNumber = "+1234567890",
        address = "123 Main St, City, State 12345"
    )

    // Mock vehicle data - Replace with actual vehicle data from your backend
    private val vehicle = Vehicle(
        id = "1",
        model = "Toyota Camry",
        licensePlate = "ABC123",
        color = "Silver"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionManager = SessionManager(requireContext())
        
        setupUserInfo()
        setupVehicleInfo()
        setupSettings()
    }

    private fun setupUserInfo() {
        binding.userName.text = user.name
        binding.userEmail.text = user.email
        binding.phoneNumberInput.setText(user.phoneNumber)
        binding.addressInput.setText(user.address)

        // Save personal details when changed
        binding.phoneNumberInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                user.phoneNumber = binding.phoneNumberInput.text.toString()
                saveUserDetails()
            }
        }

        binding.addressInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                user.address = binding.addressInput.text.toString()
                saveUserDetails()
            }
        }
    }

    private fun setupVehicleInfo() {
        binding.vehicleModelInput.setText(vehicle.model)
        binding.licensePlateInput.setText(vehicle.licensePlate)
        binding.vehicleColorInput.setText(vehicle.color)

        // Save vehicle details when changed
        binding.vehicleModelInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                vehicle.model = binding.vehicleModelInput.text.toString()
                saveVehicleDetails()
            }
        }

        binding.licensePlateInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                vehicle.licensePlate = binding.licensePlateInput.text.toString()
                saveVehicleDetails()
            }
        }

        binding.vehicleColorInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                vehicle.color = binding.vehicleColorInput.text.toString()
                saveVehicleDetails()
            }
        }
    }

    private fun setupSettings() {
        // Notifications switch
        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Implement notifications settings
            Toast.makeText(context, "Notifications ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        // Help button
        binding.helpButton.setOnClickListener {
            // TODO: Implement help & support
            Toast.makeText(context, "Help & Support coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Logout button
        binding.logoutButton.setOnClickListener {
            // Clear saved credentials and logout
            sessionManager.logout()
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
            // Navigate to login screen
            findNavController().navigate(R.id.action_profile_to_login)
        }
    }

    private fun saveUserDetails() {
        // TODO: Implement API call to save user details
        Toast.makeText(context, "Personal details saved", Toast.LENGTH_SHORT).show()
    }

    private fun saveVehicleDetails() {
        // TODO: Implement API call to save vehicle details
        Toast.makeText(context, "Vehicle details saved", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 