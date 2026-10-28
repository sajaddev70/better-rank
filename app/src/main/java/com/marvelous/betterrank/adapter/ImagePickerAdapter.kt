package com.marvelous.betterrank.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.marvelous.betterrank.databinding.ItemImagePickerBinding

class ImagePickerAdapter(
    private val images: List<String>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<ImagePickerAdapter.VH>() {

    private var selectedPos = -1

    inner class VH(val binding: ItemImagePickerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(url: String, pos: Int) {
            Glide.with(binding.root).load(url).into(binding.ivImage)

            // Pass selected state to binding
            binding.selected = (selectedPos == pos)

            binding.root.setOnClickListener {
                val old = selectedPos
                selectedPos = pos
                notifyItemChanged(old)
                notifyItemChanged(pos)
                onItemClick(pos)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemImagePickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(images[position], position)
    override fun getItemCount() = images.size
}