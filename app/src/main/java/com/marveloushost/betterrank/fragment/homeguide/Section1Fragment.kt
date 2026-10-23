package com.marveloushost.betterrank.fragment.homeguide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.marveloushost.betterrank.databinding.FragmentHomeguideSection1Binding
import com.marveloushost.betterrank.viewModel.HomeGuideViewModel

class Section1Fragment : Fragment() {
    private var _binding: FragmentHomeguideSection1Binding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeGuideViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeguideSection1Binding.inflate(inflater, container, false)
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