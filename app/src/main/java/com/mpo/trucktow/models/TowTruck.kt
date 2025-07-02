package com.mpo.trucktow.models

import com.google.android.gms.maps.model.LatLng

data class TowTruck(
    val id: String,
    val name: String,
    val location: LatLng,
    val distance: Double,
    val rating: Double,
    val isAvailable: Boolean = true,
    val phoneNumber: String = "",
    val vehicleType: String = "Standard Tow Truck",
    val isTrackingEnabled: Boolean = false,
    val lastLocationUpdate: Long = System.currentTimeMillis(),
    val estimatedArrivalTime: Long? = null,
    val currentSpeed: Float = 0f,
    val heading: Float = 0f,
    val isOnTrip: Boolean = false,
    val tripId: String? = null
) 