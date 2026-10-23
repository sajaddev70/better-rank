package com.marveloushost.betterrank.ui

import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.databinding.WebBinding
import com.marveloushost.betterrank.viewModel.WebViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Web : BaseActivity() {

    private val binding by lazy { DataBindingUtil.setContentView<WebBinding>(this, R.layout.web) }
    private val viewModel: WebViewModel by viewModels()
    private val htmlContent by lazy { intent.extras?.getString("htmlContent").toString() }
    private var isBackEnabled = true

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
            txtTitle.text=getString(R.string.pdf_preview)
        }

        binding.webView.apply {
            settings.javaScriptEnabled=true
            settings.cacheMode=WebSettings.LOAD_NO_CACHE
            settings.setSupportZoom(true)
        }
    }

    override fun onClickView() {

        viewModel.showMessage.observe(this) {
            showMessage(it)
        }

        binding.baseActionBar.imgStart.setOnClickListener( {
            if (binding.webView.canGoBack()) binding.webView.goBack() else finish()
        })

        onBackPressedDispatcher.addCallback(object :OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                if(isBackEnabled)
                    if (binding.webView.canGoBack()) binding.webView.goBack() else finish()
            }
        })

        binding.webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    }
}