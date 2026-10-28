package com.marvelous.betterrank.fragment.homeguide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.marvelous.betterrank.databinding.FragmentHomeguideSection5Binding
import com.marvelous.betterrank.viewModel.HomeGuideViewModel

class Section5Fragment : Fragment() {
    private var _binding: FragmentHomeguideSection5Binding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeGuideViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeguideSection5Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}