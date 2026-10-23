package com.marveloushost.betterrank.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.databinding.ChoseAvatarAdapterBinding

class ChoseAvatarAdapter (private val items: List<Int>) : RecyclerView.Adapter<ChoseAvatarAdapter.ChoseAvatarHolder>() {

    lateinit var onChoseAvatarAdapterListener:OnChoseAvatarAdapterListener

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): ChoseAvatarHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ChoseAvatarAdapterBinding>(inflater, R.layout.chose_avatar_adapter,parent,false)
        return ChoseAvatarHolder(binding)
    }

    override fun getItemCount()=items.size

    override fun onBindViewHolder(holder: ChoseAvatarHolder, position: Int) {
        holder.bindingData(items[position])
    }

    inner class ChoseAvatarHolder(val binding: ChoseAvatarAdapterBinding) : RecyclerView.ViewHolder(binding.root){

        fun bindingData(image: Int) {
            binding.imgImage.setImageResource(image)
            binding.imgImage.setOnClickListener {
                onChoseAvatarAdapterListener.onItemChoseAvatar(bindingAdapterPosition)
            }
        }
    }

    interface OnChoseAvatarAdapterListener{
        fun onItemChoseAvatar(position: Int)
    }
}