package com.mpo.trucktow.models

import com.google.android.gms.maps.model.LatLng

data class Driver(
    val id: String,
    val name: String,
    val rating: Float,
    val phoneNumber: String,
    val imageUrl: String,
    val currentLocation: LatLng? = null,
    val isOnline: Boolean = false,
    val lastActiveTime: Long = System.currentTimeMillis(),
    val currentSpeed: Float = 0f,
    val heading: Float = 0f,
    val isOnTrip: Boolean = false,
    val tripId: String? = null,
    val vehicleId: String? = null
) 