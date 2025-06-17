package com.mpo.trucktow.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mpo.trucktow.databinding.ItemPastRequestBinding
import com.mpo.trucktow.models.PastRequest

class PastRequestsAdapter(private val requests: List<PastRequest>) :
    RecyclerView.Adapter<PastRequestsAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemPastRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(request: PastRequest) {
            binding.requestDate.text = request.date
            binding.requestDescription.text = request.description
            binding.requestStatus.text = request.status
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPastRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(requests[position])
    }

    override fun getItemCount() = requests.size
} 