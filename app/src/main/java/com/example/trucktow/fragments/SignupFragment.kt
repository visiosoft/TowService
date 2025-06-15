package com.mpo.trucktow.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.mpo.trucktow.R
import com.mpo.trucktow.database.DatabaseHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SignupFragment : Fragment() {
    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var signupButton: MaterialButton
    private lateinit var loginTextView: View
    private lateinit var dbHelper: DatabaseHelper

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
        
        // Initialize views
        nameEditText = view.findViewById(R.id.nameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        phoneEditText = view.findViewById(R.id.phoneEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText)
        signupButton = view.findViewById(R.id.signupButton)
        loginTextView = view.findViewById(R.id.loginTextView)

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

        loginTextView.setOnClickListener {
            // Navigate back to login
            parentFragmentManager.popBackStack()
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