package com.marveloushost.betterrank.dialog

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.marveloushost.betterrank.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.marveloushost.betterrank.databinding.PhotoHomeResultDialogBinding
import com.marveloushost.betterrank.model.ApprovedModel
import com.marveloushost.betterrank.model.PhotoModel

class PhotoHomeResultDialog(
    private val photo: PhotoModel,
    private val approved: ApprovedModel,
    private val onUseThisPhoto: () -> Unit,
    private val onSkip: () -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: PhotoHomeResultDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.photo_home_result_dialog, container, false)
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

        initLayout()
    }

    private fun initLayout() {
        binding.photo = photo
        binding.approved = approved
        binding.percent = 50f

        binding.imgClose.setOnClickListener {
            onSkip()
            dismiss()
        }

        binding.btnUseThis.setOnClickListener {
            showLoading()    // ⬅️ SHOW LOADING OVERLAY
            onUseThisPhoto() // activity does Firestore save
        }
    }
    private fun showLoading() {
        binding.progressOverlay.visibility = View.VISIBLE
        binding.progressLoader.visibility = View.VISIBLE
    }

    internal fun hideLoading() {
        binding.progressOverlay.visibility = View.GONE
        binding.progressLoader.visibility = View.GONE
    }
    override fun onCancel(dialog: android.content.DialogInterface) {
        // Prevent back-press dismissing the dialog while loading
        if (binding.progressOverlay.visibility == View.VISIBLE) {
            return
        }
        super.onCancel(dialog)
    }
    override fun onStart() {
        super.onStart()

        dialog?.setOnKeyListener { _, keyCode, _ ->
            val loading = binding.progressOverlay.visibility == View.VISIBLE
            loading && keyCode == android.view.KeyEvent.KEYCODE_BACK
        }
    }



}