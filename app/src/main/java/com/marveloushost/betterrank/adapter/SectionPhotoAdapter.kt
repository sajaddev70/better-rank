// SectionPhotoAdapter.kt
package com.marveloushost.betterrank.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.marveloushost.betterrank.databinding.ItemImagePickerBinding
import com.marveloushost.betterrank.model.PhotoModel

class SectionPhotoAdapter(
    private val onPhotoClick: (PhotoModel) -> Unit,
    private val onSelectionChanged: (Boolean) -> Unit = {}
) : RecyclerView.Adapter<SectionPhotoAdapter.VH>() {

    private var photos: List<PhotoModel> = emptyList()
    private var selectedPos = -1

    fun submitList(newList: List<PhotoModel>) {
        photos = newList.mapIndexed { index, photo -> photo.copy(isSelected = index == selectedPos) }
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemImagePickerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(photo: PhotoModel, position: Int) {
            Glide.with(binding.root)
                .load(photo.url)
                .centerCrop()
                .into(binding.ivImage)

            val isSelected = (selectedPos == position)
            binding.selected = isSelected

            binding.root.setOnClickListener {
                val previous = selectedPos
                selectedPos = if (selectedPos == position) -1 else position
                if (previous != -1) notifyItemChanged(previous)
                notifyItemChanged(position)

                onSelectionChanged(selectedPos != -1)
                if (selectedPos == position) {
                    onPhotoClick(photo)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemImagePickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(photos[position], position)
    }

    override fun getItemCount() = photos.size
}