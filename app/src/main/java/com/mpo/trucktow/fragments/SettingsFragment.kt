package com.mpo.trucktow.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mpo.trucktow.R
import com.mpo.trucktow.SessionManager

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup click listeners for settings options
        view.findViewById<MaterialCardView>(R.id.notificationsCard)?.setOnClickListener {
            // TODO: Navigate to notifications settings
        }

        view.findViewById<MaterialCardView>(R.id.privacyCard)?.setOnClickListener {
            // TODO: Navigate to privacy settings
        }

        view.findViewById<MaterialCardView>(R.id.helpCard)?.setOnClickListener {
            // TODO: Navigate to help section
        }

        view.findViewById<MaterialCardView>(R.id.aboutCard)?.setOnClickListener {
            // TODO: Navigate to about section
        }

        // Setup logout functionality
        view.findViewById<MaterialCardView>(R.id.logoutCard)?.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout? You will need to sign in again.")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        try {
            // Clear session data
            val sessionManager = SessionManager(requireContext())
            sessionManager.logout()

            // Navigate to login fragment with proper navigation
            findNavController().navigate(R.id.action_settings_to_login)
        } catch (e: Exception) {
            // Handle any errors during logout
            e.printStackTrace()
        }
    }
} 