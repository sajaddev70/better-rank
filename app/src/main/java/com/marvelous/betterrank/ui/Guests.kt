package com.marvelous.betterrank.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.marvelous.betterrank.BR.guest
import com.marvelous.betterrank.R
import com.marvelous.betterrank.databinding.GuestsBinding
import com.marvelous.betterrank.dialog.GuestAddBookingBottomSheet
import com.marvelous.betterrank.fragment.BaseFragment
import com.marvelous.betterrank.fragment.ByGuestsFragment
import com.marvelous.betterrank.fragment.CalendarFragment
import com.marvelous.betterrank.viewModel.GuestsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Guests : BaseFragment() {

    private var _binding: GuestsBinding? = null
    private val binding get() = _binding!!

    val viewModel: GuestsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = GuestsBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // ---- NEW: Set initial pill position immediately ----
        val density = resources.displayMetrics.density
        val pillWidthDp = 76f  // From XML
        binding.slidingPill.translationX = pillWidthDp * density  // Move to right (By Guests)

        // Set initial text colors too
        binding.tvCalendar.setTextColor(Color.BLACK)  // Inactive
        binding.tvByGuests.setTextColor(Color.WHITE)  // Active

        return binding.root
    }

    private lateinit var byGuestsFragment: ByGuestsFragment
    private lateinit var calendarFragment: CalendarFragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Create once
        byGuestsFragment = ByGuestsFragment()
        calendarFragment = CalendarFragment()

        // Show initial
        childFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, byGuestsFragment)
            .commitNow()

        setupSegmentedControl()
        observeViewModel()

        viewModel.openAddGuestScreen.observe(viewLifecycleOwner) {
            startActivity(Intent(requireContext(), AddGuestActivity::class.java))
        }
        viewModel.navigateToEdit.observe(viewLifecycleOwner) { guestId ->
//            Log.d("Guests", "navigateToEdit: $guestId")
            guestId?.let {
                val intent = Intent(requireContext(), EditGuestActivity::class.java).apply {
                    putExtra("GUEST_ID", it)  // ← ONLY ID
                }
                startActivity(intent)
                viewModel.clearNavigateToEdit()

            }
        }
        viewModel.navigateToMessage.observe(viewLifecycleOwner) { guestObject ->
            guestObject?.let {
                val intent = Intent(requireContext(), MessagingActivity::class.java)
                intent.putExtra("GUEST_OBJECT", it)   // ارسال کامل مهمان
                startActivity(intent)
                viewModel.clearNavigateToMessage()
            }
        }
        binding.addGuestCards.btnAddBooking.setOnClickListener {
            GuestAddBookingBottomSheet().show(parentFragmentManager, "AddBookingSheet")
        }


    }

    private fun setupSegmentedControl() {
        val pill = binding.slidingPill
        val container = binding.segmentContainer

        fun switchTo(mode: GuestsViewModel.ViewMode) {
            val fragment = if (mode == GuestsViewModel.ViewMode.BY_GUESTS) byGuestsFragment else calendarFragment

            val transaction = childFragmentManager.beginTransaction()

            if (mode == GuestsViewModel.ViewMode.BY_GUESTS) {
                // Going RIGHT → show ByGuests (slide from right)
                transaction.setCustomAnimations(
                    R.anim.slide_in_right,   // new enters from right
                    R.anim.slide_out_left,   // old exits to left
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
            } else {
                // Going LEFT → show Calendar (slide from left)
                transaction.setCustomAnimations(
                    R.anim.slide_in_left,    // new enters from left
                    R.anim.slide_out_right,  // old exits to right
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            }

            transaction.replace(R.id.fragmentContainer, fragment).commit()


            // ---- ALTERNATIVE: Hardcode slide distance in dp ----
            val density = resources.displayMetrics.density
            val slideDistanceDp = 76f  // Half of 160dp container minus margins
            val targetX = if (mode == GuestsViewModel.ViewMode.BY_GUESTS) slideDistanceDp * density else 0f

            pill.animate()
                .translationX(targetX)
                .setDuration(280)
                .start()

            binding.tvCalendar.setTextColor(if (mode == GuestsViewModel.ViewMode.BY_GUESTS) Color.BLACK else Color.WHITE)
            binding.tvByGuests.setTextColor(if (mode == GuestsViewModel.ViewMode.BY_GUESTS) Color.WHITE else Color.BLACK)
        }

        // Touch listener (unchanged)
        container.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val isLeftTap = event.x < v.width / 2f
                viewModel.setViewMode(if (isLeftTap) GuestsViewModel.ViewMode.CALENDAR else GuestsViewModel.ViewMode.BY_GUESTS)
                v.performClick()
                true
            } else false
        }

        // ---- SIMPLIFIED: Observe without post{} ----
        viewModel.viewMode.observe(viewLifecycleOwner) { mode ->
            switchTo(mode)
        }

        // No need for post{} or manual initial call — position is already set!
    }

    private fun observeViewModel() {
        viewModel.showMessage.observe(viewLifecycleOwner) { message ->
            showMessage(message.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        TODO("Not yet implemented")
    }

    override fun onClick() {
        TODO("Not yet implemented")
    }


}