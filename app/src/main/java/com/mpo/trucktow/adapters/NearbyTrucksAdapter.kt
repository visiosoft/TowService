package com.mpo.trucktow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mpo.trucktow.R
import com.mpo.trucktow.models.TowTruck

class NearbyTrucksAdapter(
    private val trucks: List<TowTruck>,
    private val onTruckSelected: (TowTruck) -> Unit,
    private val onTruckClicked: (TowTruck) -> Unit
) : RecyclerView.Adapter<NearbyTrucksAdapter.TruckViewHolder>() {

    class TruckViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val truckName: TextView = view.findViewById(R.id.truckName)
        val truckDistance: TextView = view.findViewById(R.id.truckDistance)
        val truckRating: TextView = view.findViewById(R.id.truckRating)
        val selectButton: View = view.findViewById(R.id.selectButton)
        val itemView: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TruckViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nearby_truck, parent, false)
        return TruckViewHolder(view)
    }

    override fun onBindViewHolder(holder: TruckViewHolder, position: Int) {
        val truck = trucks[position]
        holder.truckName.text = truck.name
        holder.truckDistance.text = "${truck.distance} km away • ${truck.vehicleType}"
        holder.truckRating.text = "⭐ ${truck.rating}"
        
        holder.selectButton.setOnClickListener {
            onTruckSelected(truck)
        }
        
        // Make the entire item clickable to show details
        holder.itemView.setOnClickListener {
            onTruckClicked(truck)
        }
    }

    override fun getItemCount() = trucks.size
} 