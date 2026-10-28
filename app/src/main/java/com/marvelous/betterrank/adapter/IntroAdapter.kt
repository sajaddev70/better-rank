package com.marvelous.betterrank.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.marvelous.betterrank.R
import com.marvelous.betterrank.databinding.IntroAdapterBinding
import com.marvelous.betterrank.model.IntroModel

class IntroAdapter (private val items: List<IntroModel>) : RecyclerView.Adapter<IntroAdapter.IntroHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): IntroHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<IntroAdapterBinding>(inflater, R.layout.intro_adapter,parent,false)
        return IntroHolder(binding)
    }

    override fun getItemCount()=items.size

    override fun onBindViewHolder(holder: IntroHolder, position: Int) {
        holder.bindingData(items[position])
    }

    inner class IntroHolder(val binding: IntroAdapterBinding) : RecyclerView.ViewHolder(binding.root){

        fun bindingData(introModel: IntroModel) {
            binding.intro=introModel
            binding.imgImage.setImageResource(introModel.imageRes)
            binding.txtTitle.text= HtmlCompat.fromHtml(
                introModel.title,
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
        }
    }
}