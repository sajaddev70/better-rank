package com.marveloushost.betterrank.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.databinding.ActivityReviewAnalysisBinding
import com.marveloushost.betterrank.viewModel.ReviewAnalysisViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReviewAnalysisActivity : BaseActivity() {

    private val binding by lazy { DataBindingUtil.setContentView<ActivityReviewAnalysisBinding>(this, R.layout.activity_review_analysis) }
    private val viewModel: ReviewAnalysisViewModel by viewModels()
    private val homeId: String by lazy { intent.getStringExtra("homeId").orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
//        Log.d("REVANL", "ReviewAnalysisActivity CREATED — homeId: $homeId")
        binding.baseActionBar.txtTitle.text = "Review Analysis"
        binding.baseActionBar.imgStart.visibility = View.GONE
        binding.baseActionBar.imgStart.setOnClickListener { finish() }
        binding.btnBack.setOnClickListener { finish() }
        viewModel.loadAnalysis(homeId)

    }

    override fun initView() {
        TODO("Not yet implemented")
    }

    override fun onClickView() {
        TODO("Not yet implemented")
    }
}