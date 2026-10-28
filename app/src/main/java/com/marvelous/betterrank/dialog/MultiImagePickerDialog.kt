package com.marvelous.betterrank.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.marvelous.betterrank.R
import com.marvelous.betterrank.adapter.MultiImagePickerAdapter
import com.marvelous.betterrank.databinding.DialogMultiImagePickerBinding
import java.util.ArrayList

class MultiImagePickerDialog(
    private val images: List<String>,
    private val guideText: String,
    private val onImagesSelected: (List<String>) -> Unit,
    private val onTakePhoto: () -> Unit,      // ← NEW: Camera
    private val onPickFromGallery: () -> Unit // ← NEW: Gallery
) : BottomSheetDialogFragment() {

    private lateinit var binding: DialogMultiImagePickerBinding
    private val selectedPositions = mutableSetOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_multi_image_picker, container, false)
        binding.images = ArrayList(images)
        binding.selectedPositions = selectedPositions
        binding.guideText = guideText
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = MultiImagePickerAdapter(images) { position, isSelected ->
            if (isSelected) selectedPositions.add(position)
            else selectedPositions.remove(position)
            binding.btnOk.isEnabled = selectedPositions.isNotEmpty()
        }
        binding.rvImages.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            this.adapter = adapter
        }

        binding.btnTakePhoto.setOnClickListener { onTakePhoto(); dismiss() }
        binding.btnGallery.setOnClickListener { onPickFromGallery(); dismiss() }
        binding.btnOk.setOnClickListener {
            onImagesSelected(selectedPositions.map { pos -> images[pos] })
            dismiss()
        }
    }

    override fun getTheme() = R.style.CustomBottomSheetDialogTheme
}