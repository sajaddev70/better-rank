package com.marveloushost.betterrank.adapter

import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.databinding.PhotoHomeAdapterBinding
import com.marveloushost.betterrank.model.PhotoModel
import com.marveloushost.betterrank.ui.BaseActivity

class PhotoHomeAdapter(
    private val list: List<PhotoModel>,
    private val selectedItems: MutableSet<PhotoModel>,
    private val onItemToggled: (PhotoModel, Boolean) -> Unit,
    private val onDownload: (PhotoModel) -> Unit
) : RecyclerView.Adapter<PhotoHomeAdapter.PhotoHomeHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHomeHolder {
        val binding = DataBindingUtil.inflate<PhotoHomeAdapterBinding>(
            LayoutInflater.from(parent.context),
            R.layout.photo_home_adapter,
            parent,
            false
        )
        return PhotoHomeHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoHomeHolder, position: Int) {
        val photo = list[position]
        holder.bind(photo, selectedItems.contains(photo))
    }

    override fun getItemCount() = list.size

    inner class PhotoHomeHolder(val binding: PhotoHomeAdapterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photoModel: PhotoModel, isSelected: Boolean) {
            binding.photo = photoModel
            binding.isSelected = isSelected

            binding.root.setOnClickListener {
                val current = selectedItems.contains(photoModel)   // <-- always fresh
                onItemToggled(photoModel, !current)
            }

            binding.executePendingBindings()
        }
    }

}