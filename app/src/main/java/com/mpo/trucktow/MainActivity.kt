package com.mpo.trucktow

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mpo.trucktow.database.DatabaseHelper

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize session manager and database helper
        sessionManager = SessionManager(this)
        dbHelper = DatabaseHelper(this)

        // Set up the ActionBar
        setSupportActionBar(findViewById(R.id.toolbar))

        // Set up the Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Configure the ActionBar
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.profileFragment,
                R.id.requestTowFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

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

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}