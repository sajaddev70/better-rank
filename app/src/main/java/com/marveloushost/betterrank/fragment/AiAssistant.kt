package com.marveloushost.betterrank.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.databinding.AiAssistantBinding
import com.marveloushost.betterrank.viewModel.AiAssistantViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AiAssistant:BaseFragment() {

    lateinit var binding: AiAssistantBinding
    val viewModel:AiAssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        binding= DataBindingUtil.inflate(layoutInflater, R.layout.ai_assistant,container,false)

        initView()
        onClick()

        return binding.root
    }

    override fun initView() {

        //ViewModel
        binding.viewModel=viewModel
        binding.lifecycleOwner=this
    }

    override fun onClick() {

        viewModel.showMessage.observe(viewLifecycleOwner){
            showMessage(it)
        }
    }
}