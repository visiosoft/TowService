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
    val vehicleType: String = "Standard Tow Truck"
) 