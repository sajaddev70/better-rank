package com.marveloushost.betterrank.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.marveloushost.betterrank.databinding.ItemGuestCardBinding
import com.marveloushost.betterrank.model.GuestModel
import com.marveloushost.betterrank.viewModel.GuestsViewModel


class GuestAdapter(
    private val viewModel: GuestsViewModel
) : ListAdapter<GuestModel, GuestAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGuestCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.viewModel = viewModel
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), viewModel)
    }

     class ViewHolder(private val binding: ItemGuestCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(guest: GuestModel, viewModel: GuestsViewModel) {
            binding.guest = guest
            binding.viewModel = viewModel
            binding.executePendingBindings()  // ← THIS MAKES IT WORK
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<GuestModel>() {
        override fun areItemsTheSame(old: GuestModel, new: GuestModel) =
            old.id == new.id

        override fun areContentsTheSame(old: GuestModel, new: GuestModel) =
            old == new
    }
}