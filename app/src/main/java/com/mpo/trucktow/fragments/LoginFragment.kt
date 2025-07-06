package com.mpo.trucktow.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mpo.trucktow.R
import com.mpo.trucktow.database.DatabaseHelper
import com.mpo.trucktow.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class LoginFragment : Fragment() {
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var googleSignInButton: MaterialButton
    private lateinit var signupTextView: View
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    // Google Sign-In result launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleGoogleSignInResult(task)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        dbHelper = DatabaseHelper(requireContext())
        sessionManager = SessionManager(requireContext())
        
        // Initialize views
        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        loginButton = view.findViewById(R.id.loginButton)
        googleSignInButton = view.findViewById(R.id.googleSignInButton)
        signupTextView = view.findViewById(R.id.signupTextView)

        // Initialize Google Sign-In client
        mGoogleSignInClient = (requireActivity() as com.mpo.trucktow.MainActivity).getGoogleSignInClient()

        // Set click listeners
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (validateInput(email, password)) {
                if (dbHelper.checkUser(email, password)) {
                    // Login successful - save credentials for auto-login
                    sessionManager.saveLoginCredentials(email, password)
                    Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                    // Navigate to home using Navigation component
                    findNavController().navigate(R.id.action_login_to_home)
                } else {
                    Toast.makeText(context, "Invalid email or password", Toast.LENGTH_SHORT).show()
                }
            }
        }

        signupTextView.setOnClickListener {
            // Navigate to signup using Navigation component
            findNavController().navigate(R.id.action_login_to_signup)
        }

        // Set Google Sign-In button click listener
        googleSignInButton.setOnClickListener {
            startGoogleSignIn()
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            return false
        }
        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            return false
        }
        return true
    }

    private fun startGoogleSignIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Google Sign-In successful
            val name = account.displayName
            val email = account.email
            val photoUrl = account.photoUrl

            // Save user info to session
            sessionManager.saveLoginCredentials(email ?: "", "google_sign_in")
            
            // Show success message
            Toast.makeText(context, "Google Sign-In successful! Welcome $name", Toast.LENGTH_SHORT).show()
            
            // Navigate to home
            findNavController().navigate(R.id.action_login_to_home)
            
        } catch (e: ApiException) {
            // Google Sign-In failed
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.statusCode)
            Toast.makeText(context, "Google Sign-In failed: ${e.statusMessage}", Toast.LENGTH_SHORT).show()
        }
    }
} 