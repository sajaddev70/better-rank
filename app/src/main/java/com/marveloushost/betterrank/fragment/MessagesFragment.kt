package com.marveloushost.betterrank.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.marveloushost.betterrank.adapter.MessagesAdapter
import com.marveloushost.betterrank.databinding.FragmentMessagesBinding
import com.marveloushost.betterrank.ui.MessagingActivity
import com.marveloushost.betterrank.viewModel.GuestsViewModel
import com.marveloushost.betterrank.viewModel.MessagesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MessagesFragment : Fragment() {


    private lateinit var binding: FragmentMessagesBinding
    private val vm: MessagesViewModel by viewModels()
    private val guestsVM: GuestsViewModel by activityViewModels()
    private lateinit var adapter: MessagesAdapter





    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        adapter = MessagesAdapter { summary ->
            val intent = Intent(requireContext(), MessagingActivity::class.java)
            intent.putExtra("GUEST_OBJECT", summary.guest)
            startActivity(intent)
        }
        binding.rvMessages.adapter = adapter

        // --- SEARCH MODE ---
        binding.imgSearch.setOnClickListener {
            binding.clActionBar.visibility = View.GONE
            binding.clSearchBar.visibility = View.VISIBLE
            binding.etSearch.requestFocus()
        }

        binding.btnCloseSearch.setOnClickListener {
            binding.clSearchBar.visibility = View.GONE
            binding.clActionBar.visibility = View.VISIBLE
            binding.etSearch.setText("")
            vm.onSearchTextChanged("")
        }

        // Search text listener
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                vm.onSearchTextChanged(s.toString())
            }
        })

        // Load messages when guests list changes
        guestsVM.guests.observe(viewLifecycleOwner) {
            vm.loadMessages(it)
        }

        // Observe filtered list
        vm.filteredMessages.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

    }
}
