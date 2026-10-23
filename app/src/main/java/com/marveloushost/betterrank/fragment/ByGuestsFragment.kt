package com.marveloushost.betterrank.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.marveloushost.betterrank.adapter.GuestAdapter
import com.marveloushost.betterrank.databinding.FragmentByGuestsBinding
import com.marveloushost.betterrank.ui.EditGuestActivity
import com.marveloushost.betterrank.ui.Guests
import com.marveloushost.betterrank.viewModel.GuestsViewModel

class ByGuestsFragment : Fragment() {

    private var _binding: FragmentByGuestsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GuestsViewModel by activityViewModels()
    private lateinit var adapter: GuestAdapter

//    fun setViewModel(viewModel: GuestsViewModel) {
//        this.viewModel = viewModel
//        this.adapter = GuestAdapter(viewModel)
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentByGuestsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.viewModel = viewModel

        adapter = GuestAdapter(viewModel)
        binding.recyclerGuests.adapter = adapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        binding.recyclerGuests.adapter = adapter
        binding.recyclerGuests.apply {
            layoutManager = LinearLayoutManager(requireContext())
        }
        viewModel.navigateToEdit.observe(viewLifecycleOwner) { guestId ->
//            Log.d("ByGuests", "navigateToEdit received: $guestId")
            guestId?.let {
                val intent = Intent(requireContext(), EditGuestActivity::class.java).apply {
                    putExtra("GUEST_ID", it)
                }
                startActivity(intent)
            }
        }

        // Observe ONLY ONCE
        viewModel.filteredGuests.observe(viewLifecycleOwner) { filtered ->
            adapter.submitList(filtered)
            binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}