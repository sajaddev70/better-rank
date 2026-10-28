package com.marvelous.betterrank.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.marvelous.betterrank.R
import com.marvelous.betterrank.databinding.PhotographyGuideBinding
import com.marvelous.betterrank.databinding.WebBinding
import com.marvelous.betterrank.viewModel.PhotographyGuideViewModel
import com.marvelous.betterrank.viewModel.WebViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhotographyGuide : BaseActivity() {
    private var fullscreenLoading: View? = null
    private val binding: PhotographyGuideBinding by lazy { DataBindingUtil.setContentView(this, R.layout.photography_guide) }
    private val viewModel: PhotographyGuideViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()
        onClickView()
    }

    override fun initView() {

        //ViewModel
        binding.viewModel=viewModel
        binding.lifecycleOwner=this

        //ActionBar
        binding.baseActionBar.apply {
            imgStart.apply {
                setImageResource(R.drawable.ic_arrow_left)
                visibility=View.VISIBLE
            }
            txtTitle.text= getString(R.string.photography_guide)
        }
    }

    override fun onClickView() {

        viewModel.showMessage.observe(this) {
            showMessage(it)
        }

        binding.baseActionBar.imgStart.setOnClickListener( {
            finish()
        })

        binding.btnStartNow.setOnClickListener {
            val homeId = intent.getStringExtra("HOME_ID") ?: return@setOnClickListener

            val intent = Intent(this, PhotographyHome::class.java).apply {
                putExtra("HOME_ID", homeId)
            }
            startActivity(intent)
            finish()
        }

    }
    private fun showFullscreenLoading() {
        if (fullscreenLoading != null) return

        fullscreenLoading = layoutInflater.inflate(R.layout.loading_fullscreen, null)
        (window.decorView as ViewGroup).addView(fullscreenLoading)
    }

    private fun hideFullscreenLoading() {
        fullscreenLoading?.let {
            (window.decorView as ViewGroup).removeView(it)
            fullscreenLoading = null
        }
    }
}