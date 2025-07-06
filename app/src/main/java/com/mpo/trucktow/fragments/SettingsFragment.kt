package com.mpo.trucktow.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.mpo.trucktow.R
import com.mpo.trucktow.SessionManager
import com.mpo.trucktow.database.DatabaseHelper
import android.widget.CheckBox

class SettingsFragment : Fragment() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sharedPreferences: SharedPreferences
    
    // UI Elements
    private lateinit var userNameText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var accountVerifiedText: TextView
    private lateinit var notificationsSwitch: SwitchMaterial
    private lateinit var locationSwitch: SwitchMaterial
    private lateinit var locationStatusText: TextView
    private lateinit var pushNotificationCheck: CheckBox
    private lateinit var emailNotificationCheck: CheckBox
    private lateinit var smsNotificationCheck: CheckBox
    private lateinit var themeButton: MaterialButton
    private lateinit var appVersionText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize managers and preferences
        sessionManager = SessionManager(requireContext())
        dbHelper = DatabaseHelper(requireContext())
        sharedPreferences = requireContext().getSharedPreferences("TruckTowPrefs", 0)

        // Initialize UI elements
        initializeViews(view)
        
        // Load user data
        loadUserData()
        
        // Load preferences
        loadPreferences()
        
        // Setup click listeners
        setupClickListeners(view)
        
        // Setup switch listeners
        setupSwitchListeners()
    }

    private fun initializeViews(view: View) {
        userNameText = view.findViewById(R.id.userNameText)
        userEmailText = view.findViewById(R.id.userEmailText)
        accountVerifiedText = view.findViewById(R.id.accountVerifiedText)
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch)
        locationSwitch = view.findViewById(R.id.locationSwitch)
        locationStatusText = view.findViewById(R.id.locationStatusText)
        pushNotificationCheck = view.findViewById(R.id.pushNotificationCheck)
        emailNotificationCheck = view.findViewById(R.id.emailNotificationCheck)
        smsNotificationCheck = view.findViewById(R.id.smsNotificationCheck)
        themeButton = view.findViewById(R.id.themeButton)
        appVersionText = view.findViewById(R.id.appVersionText)
    }

    private fun loadUserData() {
        try {
            // Get user credentials from session
            val email = sessionManager.getSavedEmail() ?: ""
            val password = sessionManager.getSavedPassword() ?: ""
            
            if (email.isNotEmpty()) {
                // Get user details from database
                val user = dbHelper.getUserByEmail(email)
                if (user != null) {
                    userNameText.text = user.name
                    userEmailText.text = user.email
                    accountVerifiedText.text = "Verified"
                    accountVerifiedText.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
                } else {
                    // Fallback to session data
                    userNameText.text = "User"
                    userEmailText.text = email
                    accountVerifiedText.text = "Unverified"
                    accountVerifiedText.setTextColor(requireContext().getColor(android.R.color.holo_orange_dark))
                }
            } else {
                // No user data available
                userNameText.text = "Guest User"
                userEmailText.text = "Not signed in"
                accountVerifiedText.text = "Guest"
                accountVerifiedText.setTextColor(requireContext().getColor(android.R.color.darker_gray))
            }
        } catch (e: Exception) {
            // Handle any errors
            userNameText.text = "User"
            userEmailText.text = "user@example.com"
            accountVerifiedText.text = "Error"
            accountVerifiedText.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
        }
    }

    private fun loadPreferences() {
        // Load notification preference
        val notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)
        notificationsSwitch.isChecked = notificationsEnabled
        
        // Load location preference
        val locationEnabled = sharedPreferences.getBoolean("location_enabled", true)
        locationSwitch.isChecked = locationEnabled
        updateLocationStatus(locationEnabled)
        
        // Load notification type preferences
        pushNotificationCheck.isChecked = sharedPreferences.getBoolean("push_notifications", true)
        emailNotificationCheck.isChecked = sharedPreferences.getBoolean("email_notifications", true)
        smsNotificationCheck.isChecked = sharedPreferences.getBoolean("sms_notifications", false)
        
        // Load theme preference
        val isDarkTheme = sharedPreferences.getBoolean("dark_theme", false)
        themeButton.text = if (isDarkTheme) "Dark" else "Light"
        
        // Set app version
        appVersionText.text = "App Version 1.0.0"
    }

    private fun setupClickListeners(view: View) {
        // Edit Profile button
        view.findViewById<MaterialButton>(R.id.editProfileButton)?.setOnClickListener {
            showEditProfileDialog()
        }

        // Support buttons
        view.findViewById<MaterialButton>(R.id.contactSupportButton)?.setOnClickListener {
            showContactSupportDialog()
        }

        view.findViewById<MaterialButton>(R.id.faqButton)?.setOnClickListener {
            showFAQDialog()
        }

        view.findViewById<MaterialButton>(R.id.liveChatButton)?.setOnClickListener {
            showLiveChatDialog()
        }

        // Theme button
        themeButton.setOnClickListener {
            toggleTheme()
        }

        // Logout button
        view.findViewById<MaterialButton>(R.id.logoutButton)?.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Delete Account button
        view.findViewById<MaterialButton>(R.id.deleteAccountButton)?.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun setupSwitchListeners() {
        // Notifications switch listener
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()
            
            // Show feedback
            val message = if (isChecked) {
                "Great! You'll now get updates about your towing requests"
            } else {
                "Notifications turned off - you won't receive updates about your towing requests"
            }
            showToast(message)
        }

        // Location switch listener
        locationSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("location_enabled", isChecked).apply()
            updateLocationStatus(isChecked)
            
            // Show feedback
            val message = if (isChecked) {
                "Perfect! Tow trucks can now find your exact location"
            } else {
                "Location turned off - tow trucks will use approximate location"
            }
            showToast(message)
        }

        // Notification type checkboxes
        pushNotificationCheck.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("push_notifications", isChecked).apply()
        }

        emailNotificationCheck.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("email_notifications", isChecked).apply()
        }

        smsNotificationCheck.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("sms_notifications", isChecked).apply()
        }
    }

    private fun updateLocationStatus(enabled: Boolean) {
        locationStatusText.text = if (enabled) "Enabled" else "Disabled"
        locationStatusText.setTextColor(
            requireContext().getColor(
                if (enabled) android.R.color.holo_green_dark 
                else android.R.color.holo_red_dark
            )
        )
    }

    private fun toggleTheme() {
        val currentTheme = sharedPreferences.getBoolean("dark_theme", false)
        val newTheme = !currentTheme
        sharedPreferences.edit().putBoolean("dark_theme", newTheme).apply()
        themeButton.text = if (newTheme) "Dark" else "Light"
        showToast("Switched to ${if (newTheme) "Dark" else "Light"} theme")
    }

    private fun showEditProfileDialog() {
        val userEmail = userEmailText.text.toString()
        val userName = userNameText.text.toString()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Your Profile")
            .setMessage("We're working on making profile editing even better! For now, here's your current info:\n\nName: $userName\nEmail: $userEmail\n\nYou'll be able to update your details in the next app update.")
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun showContactSupportDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Need Help? We're Here!")
            .setMessage("Having trouble with TruckTow? Our support team is ready to help you 24/7.\n\n" +
                    "📧 Email us: support@trucktow.com\n" +
                    "📞 Emergency line: +1-800-TRUCKTOW\n" +
                    "📱 WhatsApp: +1-555-TRUCKTOW\n\n" +
                    "We typically respond within 5 minutes!")
            .setPositiveButton("Contact Support", null)
            .setNegativeButton("Maybe Later", null)
            .show()
    }

    private fun showFAQDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Frequently Asked Questions")
            .setMessage("Here are answers to some common questions:\n\n" +
                    "Q: How fast can I get a tow truck?\n" +
                    "A: Most of the time, you'll have a truck on the way within 15-20 minutes.\n\n" +
                    "Q: What if I'm stuck in an emergency?\n" +
                    "A: Call our emergency hotline for immediate help - we'll get someone to you right away.\n\n" +
                    "Q: How do I pay for the service?\n" +
                    "A: You can pay with cash, card, or we can bill your insurance directly.")
            .setPositiveButton("See More FAQ", null)
            .setNegativeButton("That's All", null)
            .show()
    }

    private fun showLiveChatDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Live Chat Coming Soon!")
            .setMessage("We're excited to bring you live chat support! It's currently being developed and will be available in our next update.\n\n" +
                    "For now, you can reach us at:\n" +
                    "📞 Emergency line: +1-800-TRUCKTOW\n" +
                    "📧 Email: support@trucktow.com")
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sign Out of TruckTow")
            .setMessage("Are you sure you want to sign out? You'll need to sign back in to request towing services and access your account.")
            .setPositiveButton("Yes, Sign Out") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Stay Signed In", null)
            .show()
    }

    private fun showDeleteAccountDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Your Account")
            .setMessage("This will permanently remove your account and all your data. This action cannot be undone - are you absolutely sure?")
            .setPositiveButton("Yes, Delete My Account") { _, _ ->
                performDeleteAccount()
            }
            .setNegativeButton("Keep My Account", null)
            .show()
    }

    private fun performLogout() {
        try {
            // Clear session data
            sessionManager.logout()
            
            // Clear preferences
            sharedPreferences.edit().clear().apply()
            
            // Show success message
            showToast("Successfully signed out. Thanks for using TruckTow!")
            
            // Navigate to login fragment with proper navigation
            findNavController().navigate(R.id.action_settings_to_login)
        } catch (e: Exception) {
            // Handle any errors during logout
            showToast("Error during sign out: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun performDeleteAccount() {
        try {
            // Delete user from database
            val email = sessionManager.getSavedEmail()
            if (!email.isNullOrEmpty()) {
                // TODO: Implement account deletion in DatabaseHelper
                showToast("Account deletion feature will be available in the next update")
            }
            
            // Clear session data
            sessionManager.logout()
            sharedPreferences.edit().clear().apply()
            
            showToast("Account deletion feature will be available in the next update")
            
        } catch (e: Exception) {
            showToast("Error during account deletion: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up any resources if needed
    }
} 