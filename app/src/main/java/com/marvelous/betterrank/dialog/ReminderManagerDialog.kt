package com.marvelous.betterrank.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.marvelous.betterrank.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.marvelous.betterrank.databinding.ReminderManagerDialogBinding

class ReminderManagerDialog(val onAddReminder:(startAt:Long)->Unit) : BottomSheetDialogFragment() {

    lateinit var binding:ReminderManagerDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.reminder_manager_dialog, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.post {
            val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        //init layout
        initLayout()
    }

    private fun initLayout()
    {

        binding.imgClose.setOnClickListener {
            dismiss()
        }

        binding.btnApply.setOnClickListener {
            onAddReminder(binding.singleDayPicker.date.time)
            dismiss()
        }

    }
}