package com.marveloushost.betterrank.adapter

import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.databinding.BaseAdapterBinding
import com.marveloushost.betterrank.model.BaseModel

class BaseAdapter (private var countries: List<BaseModel>) : RecyclerView.Adapter<BaseAdapter.BaseHolder>() {

    lateinit var onBaseAdapterListener: OnBaseAdapterListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseHolder {

        val inflater= LayoutInflater.from(parent.context)
        val binding= DataBindingUtil.inflate<BaseAdapterBinding>(inflater, R.layout.base_adapter,parent,false)
        return BaseHolder(binding)
    }

    override fun onBindViewHolder(holder: BaseHolder, position: Int) {
        val country = countries[position]
        holder.bindingData(country)
    }

    override fun getItemCount(): Int {
        return countries.size
    }

    inner class BaseHolder(val binding: BaseAdapterBinding) : RecyclerView.ViewHolder(binding.root)
    {

        fun bindingData(base: BaseModel) {


        }
    }
    
    interface OnBaseAdapterListener{
        

    }
}