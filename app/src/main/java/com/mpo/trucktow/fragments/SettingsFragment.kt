package com.mpo.trucktow.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.mpo.trucktow.R

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
    }
} 