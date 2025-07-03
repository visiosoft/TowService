package com.mpo.trucktow.models

import android.os.Parcel
import android.os.Parcelable
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
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        LatLng(parcel.readDouble(), parcel.readDouble()),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readByte() != 0.toByte(),
        parcel.readString() ?: "",
        parcel.readString() ?: "Standard Tow Truck",
        parcel.readByte() != 0.toByte(),
        parcel.readLong(),
        if (parcel.readByte() == 0.toByte()) null else parcel.readLong(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readByte() != 0.toByte(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeDouble(location.latitude)
        parcel.writeDouble(location.longitude)
        parcel.writeDouble(distance)
        parcel.writeDouble(rating)
        parcel.writeByte(if (isAvailable) 1 else 0)
        parcel.writeString(phoneNumber)
        parcel.writeString(vehicleType)
        parcel.writeByte(if (isTrackingEnabled) 1 else 0)
        parcel.writeLong(lastLocationUpdate)
        if (estimatedArrivalTime == null) {
            parcel.writeByte(0)
        } else {
            parcel.writeByte(1)
            parcel.writeLong(estimatedArrivalTime)
        }
        parcel.writeFloat(currentSpeed)
        parcel.writeFloat(heading)
        parcel.writeByte(if (isOnTrip) 1 else 0)
        parcel.writeString(tripId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TowTruck> {
        override fun createFromParcel(parcel: Parcel): TowTruck {
            return TowTruck(parcel)
        }

        override fun newArray(size: Int): Array<TowTruck?> {
            return arrayOfNulls(size)
        }
    }
} 