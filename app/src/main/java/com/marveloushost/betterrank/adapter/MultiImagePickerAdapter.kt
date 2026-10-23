package com.marveloushost.betterrank.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.databinding.ItemMultiImagePickerBinding

class MultiImagePickerAdapter(
    private val images: List<String>,
    private val onSelectionChanged: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<MultiImagePickerAdapter.VH>() {

    private val selectedPositions = mutableSetOf<Int>()

    inner class VH(val binding: ItemMultiImagePickerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMultiImagePickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val url = images[position]
        val isSelected = selectedPositions.contains(position)

        Glide.with(holder.itemView.context)
            .load(url)
            .centerCrop()
            .into(holder.binding.imgAdd)

        holder.binding.checkbox.isChecked = isSelected
        holder.binding.root.setOnClickListener {
            val newState = !isSelected
            if (newState) selectedPositions.add(position) else selectedPositions.remove(position)
            holder.binding.checkbox.isChecked = newState
            onSelectionChanged(position, newState)
        }
    }

    override fun getItemCount() = images.size
}