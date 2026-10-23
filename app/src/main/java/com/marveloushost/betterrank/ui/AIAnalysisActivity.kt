package com.marveloushost.betterrank.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.databinding.AiAnalysisBinding
import com.marveloushost.betterrank.viewModel.AIAnalysisViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AIAnalysisActivity : BaseActivity() {

    private lateinit var binding: AiAnalysisBinding
    private val viewModel: AIAnalysisViewModel by viewModels()
    private val homeId: String by lazy { intent.getStringExtra("homeId").orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.ai_analysis)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

//        Log.d("AI_ACT", "AIAnalysisActivity created, homeId: $homeId")

        viewModel.loadaiAnalysis(homeId)

        viewModel.analysis.observe(this) { analysis ->
//            Log.d("AI_ACT", "Analysis received: ${analysis.take(200)}...")
            binding.tvAnalysis.text = analysis
        }
        binding.btnOk.setOnClickListener { finish() }


    }

    override fun initView() {}
    override fun onClickView() {}
}