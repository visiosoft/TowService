package com.mpo.trucktow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}