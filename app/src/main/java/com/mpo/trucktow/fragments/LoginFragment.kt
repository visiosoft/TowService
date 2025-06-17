package com.mpo.trucktow.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mpo.trucktow.R
import com.mpo.trucktow.database.DatabaseHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginFragment : Fragment() {
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var signupTextView: View
    private lateinit var dbHelper: DatabaseHelper

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
        
        // Initialize views
        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        loginButton = view.findViewById(R.id.loginButton)
        signupTextView = view.findViewById(R.id.signupTextView)

        // Set click listeners
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (validateInput(email, password)) {
                if (dbHelper.checkUser(email, password)) {
                    // Login successful
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
} 