package com.marvelous.betterrank.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.marvelous.betterrank.R
import com.marvelous.betterrank.adapter.ChoseAvatarAdapter
import com.marvelous.betterrank.databinding.ChangeProfileDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.marvelous.betterrank.databinding.CheckHomeInfoDialogBinding
import com.marvelous.betterrank.model.AirBnbRequestModel
import com.marvelous.betterrank.model.AirBnbResponseModel
import com.marvelous.betterrank.ui.BaseActivity

class CheckHomeInfoDialog(private val airBnbResponseModel: AirBnbResponseModel) : BottomSheetDialogFragment() {

    lateinit var binding:CheckHomeInfoDialogBinding
    lateinit var onCheckHomeInfoDialogListener : OnCheckHomeInfoDialogListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.check_home_info_dialog, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //init layout
        initLayout()
    }

    private fun initLayout()
    {
        //Binding
        binding.home=airBnbResponseModel

        binding.imgClose.setOnClickListener {
            dismiss()
        }

        binding.btnAddHome.setOnClickListener {
            onCheckHomeInfoDialogListener.onAddHome(airBnbResponseModel)
            dismiss()
        }
    }

    interface OnCheckHomeInfoDialogListener{
        fun onAddHome(airBnbResponseModel: AirBnbResponseModel)
    }
}