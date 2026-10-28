//package com.marvelous.betterrank.fragment.homeguide
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.activityViewModels
//import com.marvelous.betterrank.databinding.FragmentHomeGuideBinding
//import com.marvelous.betterrank.viewModel.HomeGuideViewModel
//
//class HomeGuideFragment : Fragment() {
//
//    private var _binding: FragmentHomeGuideBinding? = null
//    private val binding get() = _binding!!
//    private val viewModel: HomeGuideViewModel by activityViewModels()
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentHomeGuideBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        binding.viewModel = viewModel
//        binding.lifecycleOwner = viewLifecycleOwner
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}