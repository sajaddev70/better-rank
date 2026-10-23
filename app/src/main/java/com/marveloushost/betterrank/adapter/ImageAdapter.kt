package com.marveloushost.betterrank.adapter

import android.content.Context
import android.graphics.*
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.databinding.ImageAdapterBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import android.graphics.drawable.Drawable
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import java.security.MessageDigest

class ImageAdapter(private val items: List<String>) : RecyclerView.Adapter<ImageAdapter.ImageHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): ImageHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ImageAdapterBinding>(inflater, R.layout.image_adapter, parent, false)
        return ImageHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ImageHolder, position: Int) {
        val imageUrl = items[position]
        holder.bindingData(imageUrl)
        loadImageWithTransformation(holder, holder.itemView.context, imageUrl)
    }

    private fun loadImageWithTransformation(holder: ImageHolder, context: Context, imageUrl: String) {
        val targetWidth = holder.itemView.width // e.g., 720px
        val targetHeight = 360 // 180dp in pixels

        val requestOptions = RequestOptions()
            .override(targetWidth, targetHeight) // Set target size
            .transform(CustomWidthFitHeightCrop()) // Apply custom transformation
            .dontTransform() // Disable default transformations

        Glide.with(context)
            .load(imageUrl)
            .apply(requestOptions)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    // Transformation is applied by Glide; adjust matrix if needed
                    val bitmap = (resource as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        val scale = targetWidth.toFloat() / bitmap.width.toFloat()
                        val scaledHeight = bitmap.height * scale
                        val translateY = if (scaledHeight > targetHeight) (targetHeight - scaledHeight) / 2 else 0f
                        val matrix = Matrix()
                        matrix.setScale(scale, scale)
                        matrix.postTranslate(0f, if (translateY < 0) -translateY else translateY)
                        holder.binding.imgImage.imageMatrix = matrix // Apply matrix for ScaleType
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    holder.binding.imgImage.imageMatrix = Matrix() // Reset on failure
                }
            })
    }

    // Custom Transformation to fit width and crop height equally
    private class CustomWidthFitHeightCrop : Transformation<Bitmap> {
        override fun transform(context: Context, resource: Resource<Bitmap>, outWidth: Int, outHeight: Int): Resource<Bitmap> {
            val bitmapPool = BitmapPoolAdapter(context)
            val toTransform = resource.get()
            val targetWidth = outWidth // 720px
            val targetHeight = outHeight // 360px

            val scale = targetWidth.toFloat() / toTransform.width.toFloat()
            val scaledHeight = toTransform.height * scale
            val translateY = (targetHeight - scaledHeight) / 2

            val matrix = Matrix()
            matrix.setScale(scale, scale)
            matrix.postTranslate(0f, if (translateY < 0) -translateY else translateY)

            val result = Bitmap.createBitmap(toTransform, 0, 0, toTransform.width, toTransform.height, matrix, true)
            if (result != toTransform) {
                toTransform.recycle() // Recycle the original if a new bitmap is created
            }

            val croppedBitmap = Bitmap.createBitmap(result, 0, (if (translateY < 0) -translateY else 0).toInt(), targetWidth, targetHeight)
            return BitmapResource.obtain(croppedBitmap, bitmapPool)!!
        }

        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            messageDigest.update("CustomWidthFitHeightCrop".toByteArray())
        }

        override fun equals(other: Any?): Boolean {
            return other is CustomWidthFitHeightCrop
        }

        override fun hashCode(): Int {
            return "CustomWidthFitHeightCrop".hashCode()
        }
    }

    // Helper class to provide BitmapPool to Transformation
    private class BitmapPoolAdapter(private val context: Context) : BitmapPool {
        override fun get(width: Int, height: Int, config: Bitmap.Config): Bitmap {
            return Bitmap.createBitmap(width, height, config)
        }

        override fun put(bitmap: Bitmap) {
            // No-op, not recycling for simplicity
        }

        override fun getDirty(width: Int, height: Int, config: Bitmap.Config): Bitmap {
            return Bitmap.createBitmap(width, height, config)
        }

        override fun clearMemory() {
            // No-op
        }

        override fun trimMemory(level: Int) {
            // No-op
        }

        override fun getMaxSize(): Long {
            return 0L // Return a default value, as we’re not managing a real pool
        }
        override fun setSizeMultiplier(sizeMultiplier: Float) {
            // No-op, as we’re not managing a dynamic pool size
        }
    }

    inner class ImageHolder(val binding: ImageAdapterBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindingData(image: String) {
            binding.image = image // Binds the URL or drawable path
            binding.executePendingBindings()
        }
    }
}