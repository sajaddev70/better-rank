package com.marvelous.betterrank.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.marvelous.betterrank.R
import com.marvelous.betterrank.adapter.ReviewAdapter
import com.marvelous.betterrank.databinding.ReviewsBinding
import com.marvelous.betterrank.model.AirBnbResponseModel
import com.marvelous.betterrank.viewModel.ReviewsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Reviews : BaseActivity() {

    private val viewModel: ReviewsViewModel by viewModels()
    private val binding: ReviewsBinding by lazy { DataBindingUtil.setContentView(this, R.layout.reviews) }
    private val airbnbModel: AirBnbResponseModel? by lazy {
        IntentCompat.getSerializableExtra(intent, "airBnbResponse", AirBnbResponseModel::class.java)
    }
    private val homeId: String by lazy { intent.getStringExtra("homeId").orEmpty() }
    private lateinit var reviewAdapter: ReviewAdapter
    private var dummyTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        onClickView()
    }

    override fun initView() {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        binding.baseActionBar.txtTitle.text = getString(R.string.reviews)
        binding.baseActionBar.imgStart.visibility = View.GONE

        val reviews = airbnbModel?.reviewsDetails
            ?: airbnbModel?.reviews?.map { AirBnbResponseModel.ReviewDetail(reviewText = it) }
            ?: emptyList()

        reviewAdapter = ReviewAdapter(reviews)
        binding.recyclerReviews.apply {
            adapter = reviewAdapter
            layoutManager = LinearLayoutManager(this@Reviews)
        }
    }

    override fun onClickView() {
        viewModel.showMessage.observe(this) { showMessage(it) }

        binding.baseActionBar.imgStart.setOnClickListener { finish() }
        binding.btnBack.setOnClickListener { finish() }

        binding.btnAnalyzeReviews.setOnClickListener {
            if (homeId.isBlank()) {
                showMessage("Home ID missing")
                return@setOnClickListener
            }
//            Log.d("REVANL", "Analyze Reviews button clicked → homeId: $homeId")
            showLoading()
            viewModel.analyzeReviews(homeId)
        }

        viewModel.navigateToAnalysis.observe(this) { id ->
//            Log.d("REVANL", "Navigating to ReviewAnalysisActivity with homeId: $id")
            startActivity(Intent(this, ReviewAnalysisActivity::class.java).apply {
                putExtra("homeId", id)
            })
        }
    }
    private fun showLoading() {
        binding.baseLoading.txtTitle.visibility = View.VISIBLE

        val tv = binding.baseLoading.txtText
        val title = binding.baseLoading.txtTitle
        tv.visibility = View.VISIBLE

        dummyTimer?.cancel()
        dummyTimer = object : CountDownTimer(11000, 1000) {
            override fun onTick(duration: Long) {
                tv.text = "${duration / 1000}"
                title.text ="Analyzing..."
            }

            override fun onFinish() {
                binding.baseLoading.root.visibility = View.GONE
                dummyTimer?.cancel()
            }

        }.start()
    }


//    private fun hideLoading() {
//        binding.baseLoading.root.visibility = View.GONE
//        dummyTimer?.cancel()
//    }
}