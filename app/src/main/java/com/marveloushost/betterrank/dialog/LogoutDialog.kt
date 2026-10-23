package com.marveloushost.betterrank.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.databinding.SelectImageDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.marveloushost.betterrank.databinding.LogoutDialogBinding

class LogoutDialog : BottomSheetDialogFragment() {

    lateinit var binding: LogoutDialogBinding
    lateinit var onLogoutDialogDialogListener : OnLogoutDialogDialogListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.logout_dialog, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //init layout
        initLayout()
    }

    private fun initLayout()
    {
        binding.imgClose.setOnClickListener { dismiss() }
        binding.btnLogout.setOnClickListener {
            onLogoutDialogDialogListener.onLogout()
            dismiss()
        }
        binding.btnDelete.setOnClickListener {
            onLogoutDialogDialogListener.onDelete()
            dismiss()
        }
    }

    interface OnLogoutDialogDialogListener{
        fun onLogout()
        fun onDelete()
    }
}