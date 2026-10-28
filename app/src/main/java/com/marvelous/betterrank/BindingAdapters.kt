package com.marvelous.betterrank

import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.marvelous.betterrank.R

@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, url: String?) {
    if (url.isNullOrEmpty()) {
        view.setImageResource(R.drawable.ic_image) // optional
    } else {
        Glide.with(view.context)
            .load(url)
            .centerCrop()
            .into(view)
    }
}

@BindingAdapter("selectedOverlay")
fun setSelectedOverlay(view: View, isSelected: Boolean) {
    view.setBackgroundResource(if (isSelected) R.drawable.selected_overlay else 0)
}