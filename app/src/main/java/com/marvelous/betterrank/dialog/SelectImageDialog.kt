package com.marvelous.betterrank.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.marvelous.betterrank.R
import com.marvelous.betterrank.databinding.SelectImageDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectImageDialog : BottomSheetDialogFragment() {

    lateinit var binding:SelectImageDialogBinding
    lateinit var onSelectImageDialogListener : OnSelectImageDialogListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.select_image_dialog, container, false)
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
        binding.btnCamera.setOnClickListener {
            onSelectImageDialogListener.onCamera()
            dismiss()
        }
        binding.btnGallery.setOnClickListener {
            onSelectImageDialogListener.onGallery()
            dismiss()
        }
    }

    interface OnSelectImageDialogListener{
        fun onCamera()
        fun onGallery()
    }
}