package com.mpo.trucktow.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
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
import com.google.android.material.textfield.TextInputLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class SignupFragment : Fragment() {
    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var signupButton: MaterialButton
    private lateinit var googleSignUpButton: MaterialButton
    private lateinit var loginTextView: View
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
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        dbHelper = DatabaseHelper(requireContext())
        sessionManager = SessionManager(requireContext())
        
        // Initialize views
        nameEditText = view.findViewById(R.id.nameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        phoneEditText = view.findViewById(R.id.phoneEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText)
        passwordLayout = view.findViewById(R.id.passwordLayout)
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordLayout)
        signupButton = view.findViewById(R.id.signupButton)
        googleSignUpButton = view.findViewById(R.id.googleSignUpButton)
        loginTextView = view.findViewById(R.id.loginTextView)

        // Initialize Google Sign-In client
        mGoogleSignInClient = (requireActivity() as com.mpo.trucktow.MainActivity).getGoogleSignInClient()

        // Setup password toggle functionality
        setupPasswordToggle()

        // Set click listeners
        signupButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()
            val phone = phoneEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (validateInput(name, email, phone, password, confirmPassword)) {
                if (dbHelper.isEmailExists(email)) {
                    Toast.makeText(context, "Email already exists", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val result = dbHelper.addUser(email, password, name, phone)
                if (result != -1L) {
                    Toast.makeText(context, "Signup successful", Toast.LENGTH_SHORT).show()
                    // Navigate back to login
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(context, "Signup failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Set Google Sign-Up button click listener
        googleSignUpButton.setOnClickListener {
            startGoogleSignIn()
        }

        loginTextView.setOnClickListener {
            // Navigate back to login
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupPasswordToggle() {
        // Setup password toggle for password field
        passwordLayout.setEndIconOnClickListener {
            if (passwordEditText.transformationMethod is PasswordTransformationMethod) {
                // Show password
                passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                passwordLayout.endIconDrawable = requireContext().getDrawable(android.R.drawable.ic_menu_view)
            } else {
                // Hide password
                passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                passwordLayout.endIconDrawable = requireContext().getDrawable(android.R.drawable.ic_menu_view)
            }
            // Move cursor to end
            passwordEditText.setSelection(passwordEditText.text?.length ?: 0)
        }

        // Setup password toggle for confirm password field
        confirmPasswordLayout.setEndIconOnClickListener {
            if (confirmPasswordEditText.transformationMethod is PasswordTransformationMethod) {
                // Show password
                confirmPasswordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                confirmPasswordLayout.endIconDrawable = requireContext().getDrawable(android.R.drawable.ic_menu_view)
            } else {
                // Hide password
                confirmPasswordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                confirmPasswordLayout.endIconDrawable = requireContext().getDrawable(android.R.drawable.ic_menu_view)
            }
            // Move cursor to end
            confirmPasswordEditText.setSelection(confirmPasswordEditText.text?.length ?: 0)
        }
    }

    private fun startGoogleSignIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Google Sign-In successful
            val name = account.displayName ?: "User"
            val email = account.email ?: ""
            val phone = "" // Google doesn't provide phone number

            // Check if user already exists
            if (dbHelper.isEmailExists(email)) {
                Toast.makeText(context, "Account already exists. Please sign in instead.", Toast.LENGTH_LONG).show()
                // Navigate back to login
                parentFragmentManager.popBackStack()
                return
            }

            // Add user to database
            val result = dbHelper.addUser(email, "google_sign_in", name, phone)
            if (result != -1L) {
                // Save user info to session
                sessionManager.saveLoginCredentials(email, "google_sign_in")
                
                // Show success message
                Toast.makeText(context, "Google Sign-Up successful! Welcome $name", Toast.LENGTH_SHORT).show()
                
                // Navigate to home using Navigation component
                findNavController().navigate(R.id.action_signup_to_home)
            } else {
                Toast.makeText(context, "Signup failed", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: ApiException) {
            // Google Sign-In failed
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.statusCode)
            Toast.makeText(context, "Google Sign-In failed: ${e.statusMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInput(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (name.isEmpty()) {
            nameEditText.error = "Name is required"
            return false
        }
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            return false
        }
        if (phone.isEmpty()) {
            phoneEditText.error = "Phone number is required"
            return false
        }
        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            return false
        }
        if (confirmPassword.isEmpty()) {
            confirmPasswordEditText.error = "Please confirm your password"
            return false
        }
        if (password != confirmPassword) {
            confirmPasswordEditText.error = "Passwords do not match"
            return false
        }
        return true
    }
} 