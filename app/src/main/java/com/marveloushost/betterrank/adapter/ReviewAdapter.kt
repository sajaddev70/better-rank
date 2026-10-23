package com.marveloushost.betterrank.adapter

import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.databinding.ReviewAdapterBinding
import com.marveloushost.betterrank.model.AirBnbResponseModel

//class ReviewAdapter (private var list: List<String>) : RecyclerView.Adapter<ReviewAdapter.ReviewHolder>() {
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewHolder {
//
//        val inflater= LayoutInflater.from(parent.context)
//        val binding= DataBindingUtil.inflate<ReviewAdapterBinding>(inflater, R.layout.review_adapter,parent,false)
//        return ReviewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: ReviewHolder, position: Int) {
//        val review = list[position]
//        holder.bindingData(review)
//    }
//
//    override fun getItemCount(): Int {
//        return list.size
//    }
//
//    inner class ReviewHolder(val binding: ReviewAdapterBinding) : RecyclerView.ViewHolder(binding.root)
//    {
//
//        fun bindingData(review: String) {
//            binding.review=review
//            binding.executePendingBindings()
//        }
//    }
//}
class ReviewAdapter(private val list: List<AirBnbResponseModel.ReviewDetail>) :
    RecyclerView.Adapter<ReviewAdapter.ReviewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewHolder {
        val binding = DataBindingUtil.inflate<ReviewAdapterBinding>(
            LayoutInflater.from(parent.context),
            R.layout.review_adapter,
            parent,
            false
        )
        return ReviewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size

    inner class ReviewHolder(val binding: ReviewAdapterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(review: AirBnbResponseModel.ReviewDetail) {
            binding.review = review
            binding.executePendingBindings()
        }
    }
}