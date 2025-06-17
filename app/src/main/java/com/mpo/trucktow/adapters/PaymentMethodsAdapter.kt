package com.mpo.trucktow.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mpo.trucktow.databinding.ItemPaymentMethodBinding
import com.mpo.trucktow.models.PaymentMethod

class PaymentMethodsAdapter(private val paymentMethods: List<PaymentMethod>) :
    RecyclerView.Adapter<PaymentMethodsAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemPaymentMethodBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(paymentMethod: PaymentMethod) {
            binding.paymentMethodName.text = paymentMethod.displayName
            binding.paymentMethodType.text = paymentMethod.type
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPaymentMethodBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(paymentMethods[position])
    }

    override fun getItemCount() = paymentMethods.size
} 