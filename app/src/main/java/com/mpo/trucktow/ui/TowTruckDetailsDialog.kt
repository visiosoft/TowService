package com.mpo.trucktow.ui

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.mpo.trucktow.R
import com.mpo.trucktow.databinding.DialogTowTruckDetailsBinding
import com.mpo.trucktow.models.TowTruck

class TowTruckDetailsDialog : DialogFragment() {

    private var _binding: DialogTowTruckDetailsBinding? = null
    private val binding get() = _binding!!
    
    private var towTruck: TowTruck? = null
    private var onReserveClick: ((TowTruck) -> Unit)? = null

    companion object {
        private const val ARG_TOW_TRUCK = "tow_truck"
        
        fun newInstance(towTruck: TowTruck, onReserveClick: (TowTruck) -> Unit): TowTruckDetailsDialog {
            val dialog = TowTruckDetailsDialog()
            dialog.onReserveClick = onReserveClick
            
            val args = Bundle()
            args.putParcelable(ARG_TOW_TRUCK, towTruck)
            dialog.arguments = args
            
            return dialog
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTowTruckDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        // Make dialog wider
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get tow truck from arguments
        arguments?.getParcelable<TowTruck>(ARG_TOW_TRUCK)?.let { truck ->
            towTruck = truck
            populateTruckDetails(truck)
        }
        
        setupClickListeners()
    }

    private fun populateTruckDetails(truck: TowTruck) {
        binding.apply {
            truckCompanyName.text = truck.name
            truckRating.text = "⭐ ${truck.rating}"
            truckDistance.text = "${truck.distance} km away"
            vehicleType.text = truck.vehicleType
            
            // Set availability status
            if (truck.isAvailable) {
                availabilityChip.text = "Available"
                availabilityChip.setChipBackgroundColorResource(R.color.green_500)
            } else {
                availabilityChip.text = "Busy"
                availabilityChip.setChipBackgroundColorResource(R.color.red_500)
            }
            
            // Set vehicle capacity based on type
            val capacity = when {
                truck.vehicleType.contains("Heavy Duty") -> "Up to 25,000 lbs"
                truck.vehicleType.contains("Flatbed") -> "Up to 10,000 lbs"
                truck.vehicleType.contains("Wheel Lift") -> "Up to 8,000 lbs"
                else -> "Up to 6,000 lbs"
            }
            binding.vehicleCapacity.text = "Capacity: $capacity"
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            // Close button
            closeButton.setOnClickListener {
                dismiss()
            }
            
            // Reserve button
            reserveButton.setOnClickListener {
                towTruck?.let { truck ->
                    if (truck.isAvailable) {
                        onReserveClick?.invoke(truck)
                        dismiss()
                    } else {
                        Toast.makeText(context, "This tow truck is currently busy", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 