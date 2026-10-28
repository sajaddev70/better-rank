package com.marvelous.betterrank.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.viewpager2.widget.ViewPager2
import com.marvelous.betterrank.R
import com.marvelous.betterrank.adapter.IntroAdapter
import com.marvelous.betterrank.databinding.IntroBinding
import com.marvelous.betterrank.hilt.modual.HiltSharedPrefModule
import com.marvelous.betterrank.model.IntroModel
import com.marvelous.betterrank.viewModel.IntroViewModel  // ← CORRECT IMPORT
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Intro : BaseActivity() {

    private val viewModel: IntroViewModel by viewModels()
    private val binding: IntroBinding by lazy { DataBindingUtil.setContentView(this, R.layout.intro) }
    private lateinit var adapter: IntroAdapter

    private val pages by lazy {
        listOf(
            IntroModel(getString(R.string.intro_title1), getString(R.string.intro_subtitle1), R.drawable.intro1),
            IntroModel(getString(R.string.intro_title2), getString(R.string.intro_subtitle2), R.drawable.intro2),
            IntroModel(getString(R.string.intro_title3), getString(R.string.intro_subtitle3), R.drawable.intro3)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        onClickView()

        viewModel.setOnCompleteListener {
            startApp()
        }
    }

    override fun initView() {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setupPager()
    }

    override fun onClickView() {
        viewModel.showMessage.observe(this) { message ->
            showMessage(message)
        }

        viewModel.currentPosition.observe(this) { position ->
            binding.pager.currentItem = position
        }
    }

    private fun setupPager() {
        adapter = IntroAdapter(pages)
        binding.pager.adapter = adapter
        binding.dotsIndicator.attachTo(binding.pager)
        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.currentPosition.value = position
            }
        })
    }

    private fun startApp() {
        viewModel.appRepository.saveDataSharedPref(HiltSharedPrefModule.INTRO_STATUS, "1")
        startActivity(Intent(this, Login::class.java))
        finish()
    }
}