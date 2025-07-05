package com.mpo.trucktow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mpo.trucktow.database.DatabaseHelper

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: DatabaseHelper

    // Permission request launcher
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Location permissions granted
                Toast.makeText(this, "Location permissions granted", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Location permissions denied
                Toast.makeText(this, "Location permissions are required for full functionality", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request permissions at startup
        requestLocationPermissions()

        // Initialize session manager and database helper
        sessionManager = SessionManager(this)
        dbHelper = DatabaseHelper(this)

        // Set up the Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Set up the Bottom Navigation
        findViewById<BottomNavigationView>(R.id.bottom_navigation)?.let { bottomNav ->
            bottomNav.setupWithNavController(navController)
            
            // Hide bottom navigation on login and signup screens
            navController.addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.loginFragment, R.id.signupFragment -> bottomNav.visibility = android.view.View.GONE
                    else -> bottomNav.visibility = android.view.View.VISIBLE
                }
            }
        }

        // Check for auto-login
        checkAutoLogin()
    }

    private fun requestLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        // Check if permissions are already granted
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            locationPermissionRequest.launch(permissionsToRequest)
        }
    }

    private fun checkAutoLogin() {
        if (sessionManager.isAutoLoginEnabled() && sessionManager.isLoggedIn()) {
            val savedEmail = sessionManager.getSavedEmail()
            val savedPassword = sessionManager.getSavedPassword()
            
            if (savedEmail != null && savedPassword != null) {
                // Verify credentials with database
                if (dbHelper.checkUser(savedEmail, savedPassword)) {
                    // Auto-login successful
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                    navController.navigate(R.id.homeFragment)
                } else {
                    // Saved credentials are invalid, clear them
                    sessionManager.logout()
                    Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // No saved credentials
                sessionManager.logout()
            }
        } else {
            // First time app launch - auto-login with default credentials
            if (dbHelper.checkUser("i@gmail.com", "sajid123")) {
                sessionManager.saveLoginCredentials("i@gmail.com", "sajid123")
                Toast.makeText(this, "Auto-login successful!", Toast.LENGTH_SHORT).show()
                navController.navigate(R.id.homeFragment)
            }
        }
    }


}