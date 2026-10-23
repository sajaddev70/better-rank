package com.marveloushost.betterrank.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.adapter.ImagePickerAdapter
import com.marveloushost.betterrank.databinding.DialogImagePickerBinding
import java.util.ArrayList

class ImagePickerDialog(
    private val images: List<String>,
    private val onImageSelected: (String) -> Unit,
    private val onPickFromPhone: () -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: DialogImagePickerBinding
    private var title: String? = null
    private var selectedIndex = -1

    fun setTitle(title: String): ImagePickerDialog {
        this.title = title
        return this
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_image_picker, container, false)
        binding.images = ArrayList(images)
        binding.selectedIndex = selectedIndex
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle?.text = title

        // RecyclerView
        val adapter = ImagePickerAdapter(images) { pos ->
            selectedIndex = pos
            binding.selectedIndex = pos
        }
        binding.rvImages.adapter = adapter

        // Buttons
        binding.btnFromPhone.setOnClickListener {
            onPickFromPhone()
            dismiss()
        }

        binding.btnOk.setOnClickListener {
            if (selectedIndex != -1) {
                onImageSelected(images[selectedIndex])
                dismiss()
            }
        }
    }

    override fun getTheme() = R.style.CustomBottomSheetDialogTheme
}