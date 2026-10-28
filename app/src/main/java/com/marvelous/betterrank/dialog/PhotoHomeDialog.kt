package com.marvelous.betterrank.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.marvelous.betterrank.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.marvelous.betterrank.databinding.PhotoHomeDialogBinding

class PhotoHomeDialog(
    val title:String,
    val onCamera:()->Unit,
//    val onGallery:()->Unit,
    val onShowImagePicker: () -> Unit
) : BottomSheetDialogFragment() {

    lateinit var binding:PhotoHomeDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.photo_home_dialog, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //init layout
        initLayout()
    }

    private fun initLayout()
    {
        binding.title=title

        binding.imgClose.setOnClickListener {
            dismiss()
        }

        binding.btnCamera.setOnClickListener {
            onCamera()
            dismiss()
        }

        binding.btnGallery.setOnClickListener {
            onShowImagePicker()
            dismiss()
        }
    }
}