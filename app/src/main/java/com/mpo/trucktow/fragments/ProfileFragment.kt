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
        setupSaveButton()
    }

    private fun setupUserInfo() {
        // Initialize user input fields
        binding.userNameInput.setText(user.name)
        binding.phoneNumberInput.setText(user.phoneNumber)
    }

    private fun setupVehicleInfo() {
        binding.vehicleModelInput.setText(vehicle.model)
        binding.licensePlateInput.setText(vehicle.licensePlate)
        binding.vehicleColorInput.setText(vehicle.color)
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            // Add visual feedback
            binding.saveButton.isEnabled = false
            binding.saveButton.text = "Saving..."
            
            saveAllChanges()
            
            // Re-enable button after a short delay
            binding.saveButton.postDelayed({
                binding.saveButton.isEnabled = true
                binding.saveButton.text = "Save Changes"
            }, 2000)
        }
    }

    private fun saveAllChanges() {
        // Validate inputs
        if (!validateInputs()) {
            binding.saveButton.isEnabled = true
            binding.saveButton.text = "Save Changes"
            return
        }

        // Update user data
        user.name = binding.userNameInput.text.toString().trim()
        user.phoneNumber = binding.phoneNumberInput.text.toString().trim()

        // Update vehicle data
        vehicle.model = binding.vehicleModelInput.text.toString().trim()
        vehicle.licensePlate = binding.licensePlateInput.text.toString().trim()
        vehicle.color = binding.vehicleColorInput.text.toString().trim()

        // Save to backend (TODO: Implement actual API call)
        saveUserDetails()
        saveVehicleDetails()

        // Show success message with better styling
        showSuccessMessage()
    }

    private fun showSuccessMessage() {
        Toast.makeText(context, "✅ Profile updated successfully!", Toast.LENGTH_LONG).show()
    }

    private fun validateInputs(): Boolean {
        val name = binding.userNameInput.text.toString().trim()
        val phone = binding.phoneNumberInput.text.toString().trim()
        val vehicleModel = binding.vehicleModelInput.text.toString().trim()
        val licensePlate = binding.licensePlateInput.text.toString().trim()
        val vehicleColor = binding.vehicleColorInput.text.toString().trim()

        if (name.isEmpty()) {
            binding.userNameInput.error = "Please enter your full name"
            return false
        }

        if (name.length < 2) {
            binding.userNameInput.error = "Name must be at least 2 characters long"
            return false
        }

        if (phone.isEmpty()) {
            binding.phoneNumberInput.error = "Please enter your phone number"
            return false
        }

        if (phone.length < 10) {
            binding.phoneNumberInput.error = "Please enter a valid phone number"
            return false
        }

        if (vehicleModel.isEmpty()) {
            binding.vehicleModelInput.error = "Please enter your vehicle model"
            return false
        }

        if (vehicleModel.length < 3) {
            binding.vehicleModelInput.error = "Vehicle model must be at least 3 characters"
            return false
        }

        if (licensePlate.isEmpty()) {
            binding.licensePlateInput.error = "Please enter your license plate number"
            return false
        }

        if (licensePlate.length < 3) {
            binding.licensePlateInput.error = "License plate must be at least 3 characters"
            return false
        }

        if (vehicleColor.isEmpty()) {
            binding.vehicleColorInput.error = "Please enter your vehicle color"
            return false
        }

        if (vehicleColor.length < 2) {
            binding.vehicleColorInput.error = "Vehicle color must be at least 2 characters"
            return false
        }

        return true
    }

    private fun saveUserDetails() {
        // TODO: Implement API call to save user details
        // For now, we'll just show a toast
        Toast.makeText(context, "Personal details saved", Toast.LENGTH_SHORT).show()
    }

    private fun saveVehicleDetails() {
        // TODO: Implement API call to save vehicle details
        // For now, we'll just show a toast
        Toast.makeText(context, "Vehicle details saved", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 