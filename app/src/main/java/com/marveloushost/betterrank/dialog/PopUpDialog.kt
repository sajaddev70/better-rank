package com.marveloushost.betterrank.dialog

import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.databinding.PopupDialogBinding


class PopUpDialog: DialogFragment()
{
    private lateinit var binding: PopupDialogBinding
    lateinit var onPopUpDialogListener: OnPopUpDialogListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog= super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.dialog_background_border))
        dialog.setContentView(R.layout.popup_dialog)
        dialog.window!!.attributes.windowAnimations=R.style.PauseDialogAnimation
        dialog.setCanceledOnTouchOutside(false)

        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        binding=DataBindingUtil.inflate(inflater,R.layout.popup_dialog,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initLayout()
        onClickView()
    }

    private fun initLayout() {


        //set size
        val displayMetrics = DisplayMetrics()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // For API 30 and above
            val windowMetrics = requireActivity().windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            val widthPixels = bounds.width()

            binding.root.layoutParams.width = widthPixels - 100
        } else {
            // For API below 30
            @Suppress("DEPRECATION")
            requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
            binding.root.layoutParams.width = displayMetrics.widthPixels - 100
        }
    }

    private fun onClickView() {

    }

    interface OnPopUpDialogListener{

    }

}