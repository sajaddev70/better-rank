package com.marvelous.betterrank.ui

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import com.marvelous.betterrank.R
import com.marvelous.betterrank.hilt.modual.HiltNetworkModule
import dagger.hilt.android.AndroidEntryPoint

// Edge-to-edge is enforced from targetSdk 35+, so pad the content root by the system bars and IME.
fun AppCompatActivity.applySystemBarInsets() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
        val sysInsets = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
        )
        view.setPadding(sysInsets.left, sysInsets.top, sysInsets.right, sysInsets.bottom)
        WindowInsetsCompat.CONSUMED
    }
}

@AndroidEntryPoint
abstract  class BaseActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarInsets()
    }

    abstract fun initView()
    abstract fun onClickView()

    fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    fun showSnackBar(contextView: View, message:String)
    {
        val snackBar= Snackbar.make(contextView,message,Snackbar.LENGTH_SHORT)
        snackBar.show()
    }

    fun AppCompatActivity.hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus ?: View(this) // ویویی که الان فوکوس داره
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    object DataBindingAdapter {

        @JvmStatic
        @BindingAdapter("imageLoadByUrl")
        fun imageLoadByUrl(image: ImageView, url:String?)
        {
            Glide.with(image)
                .load(url)
                .apply(
                    RequestOptions()
                        .centerCrop()
                )
                .into(image)

        }

        @JvmStatic
        @BindingAdapter("imageLoad")
        fun imageLoad(image: ImageView, url:String?)
        {
            Glide.with(image)
                .load(HiltNetworkModule.BASEURL+url)
                .apply(
                    RequestOptions()
                        .centerCrop()
                )
                .into(image)

        }

        @JvmStatic
        @BindingAdapter("htmlLoad")
        fun htmlLoad(text:TextView,content:String){

            text.text= HtmlCompat.fromHtml(
                content,
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
        }

        @JvmStatic
        @BindingAdapter("approvedView")
        fun approvedView(container: ViewGroup,approved:Boolean){

            val resources=container.context.resources

            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (approved){
                    when(child)
                    {
                        is ConstraintLayout->{child.setBackgroundResource(R.drawable.secondary_container_background2)}
                        is AppCompatImageView->{
                            if (child.tag=="icAi"){
                                child.visibility=View.VISIBLE
                            }
                            ImageViewCompat.setImageTintList(
                                child,
                                ColorStateList.valueOf(resources.getColor(R.color.colorOnSecondaryContainer,null))
                            )
                        }
                        is AppCompatTextView->{
                            if (child.tag=="textApproved"){
                                child.setBackgroundResource(R.drawable.secondary_container_background2)
                                child.text=resources.getString(R.string.approved)
                            }
                            val color = ContextCompat.getColor(container.context, R.color.colorOnSecondaryContainer)
                            child.setTextColor(color)
                        }
                    }
                }
                else
                {
                    when(child)
                    {
                        is ConstraintLayout->{child.setBackgroundResource(R.drawable.primary_background_gradient2)}
                        is AppCompatImageView->{
                            if (child.tag=="icAi"){
                                child.visibility=View.GONE
                            }
                            ImageViewCompat.setImageTintList(
                                child,
                                ColorStateList.valueOf(resources.getColor(R.color.colorOnPrimary,null))
                            )
                        }
                        is AppCompatTextView->{
                            if (child.tag=="textApproved"){
                                child.setBackgroundResource(R.drawable.bg_container_background)
                                child.text=resources.getString(R.string.not_done)
                                val color = ContextCompat.getColor(container.context, R.color.colorOnBackgroundContainer)
                                child.setTextColor(color)
                            }
                            else
                            {
                                val color = ContextCompat.getColor(container.context, R.color.colorOnPrimary)
                                child.setTextColor(color)
                            }
                        }
                    }
                }
            }

        }
    }
}