// AirbnbPhotoAdapter.kt
package com.marveloushost.betterrank.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.marveloushost.betterrank.databinding.ItemImagePickerBinding

class AirbnbPhotoAdapter(
    private val onPhotoSelected: (String) -> Unit,
    private val onPhotoCleared: () -> Unit,
    private val onSelectionChanged: (Boolean) -> Unit = {}
) : RecyclerView.Adapter<AirbnbPhotoAdapter.VH>() {

    private var images: List<String> = emptyList()
    private var selectedPos = -1

    fun submitList(newList: List<String>) {
        images = newList
        selectedPos = -1
        notifyDataSetChanged()
        onSelectionChanged(false)
    }

    inner class VH(val binding: ItemImagePickerBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(url: String, position: Int) {
            Glide.with(binding.root)
                .load(url)
                .fitCenter()
                .into(binding.ivImage)

            binding.selected = (selectedPos == position)

            binding.root.setOnClickListener {

                val previous = selectedPos

                val wasSelected = (selectedPos == position)
                selectedPos = if (wasSelected) -1 else position

                if (previous != -1) notifyItemChanged(previous)
                notifyItemChanged(position)

                val hasSelection = selectedPos != -1
                onSelectionChanged(hasSelection)

                if (hasSelection) {
                    onPhotoSelected(url)
                } else {
                    onPhotoCleared()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemImagePickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(images[position], position)
    }

    override fun getItemCount() = images.size
}
