package com.mpo.trucktow.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mpo.trucktow.R
import com.mpo.trucktow.databinding.FragmentProfileBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

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
        setupBottomNavigation()
        setupClickListeners()
        loadUserProfile()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    findNavController().navigate(R.id.action_profile_to_login)
                    true
                }
                R.id.navigation_profile -> {
                    // Already on profile
                    true
                }
                R.id.navigation_settings -> {
                    // TODO: Navigate to settings
                    Toast.makeText(context, "Settings coming soon!", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        // Set profile as selected
        binding.bottomNavigation.selectedItemId = R.id.navigation_profile
    }

    private fun setupClickListeners() {
        binding.editProfileButton.setOnClickListener {
            // TODO: Implement edit profile functionality
        }

        binding.changePasswordButton.setOnClickListener {
            // TODO: Implement change password functionality
        }

        binding.logoutButton.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_login)
        }

        binding.notificationsButton.setOnClickListener {
            // TODO: Implement notifications functionality
        }

        binding.helpButton.setOnClickListener {
            // TODO: Implement help functionality
        }

        binding.editImageButton.setOnClickListener {
            // TODO: Implement profile image edit functionality
        }
    }

    private fun loadUserProfile() {
        // TODO: Load user profile data from your data source
        // For now, we'll just set some placeholder data
        binding.profileName.text = "John Doe"
        binding.profileEmail.text = "john.doe@example.com"
        binding.phoneNumberInput.setText("+1 (555) 123-4567")
        binding.addressInput.setText("123 Main St, City, State 12345")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 