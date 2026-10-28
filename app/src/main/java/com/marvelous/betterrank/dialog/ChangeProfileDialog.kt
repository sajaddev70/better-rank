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

class ChangeProfileDialog : BottomSheetDialogFragment(),
    ChoseAvatarAdapter.OnChoseAvatarAdapterListener {

    lateinit var binding:ChangeProfileDialogBinding
    lateinit var onChangeProfileDialogListener : OnChangeProfileDialogListener
    private lateinit var adapter:ChoseAvatarAdapter
    private val avatars by lazy {
        listOf(
            R.drawable.avatar_1,R.drawable.avatar_2,R.drawable.avatar_3, R.drawable.avatar_1,R.drawable.avatar_5,
            R.drawable.avatar_6,R.drawable.avatar_7,R.drawable.avatar_8,R.drawable.avatar_9,R.drawable.avatar_10,
            R.drawable.avatar_11,R.drawable.avatar_12,R.drawable.avatar_13,R.drawable.avatar_14,R.drawable.avatar_15,
            R.drawable.avatar_16,R.drawable.avatar_17,R.drawable.avatar_18,R.drawable.avatar_19,R.drawable.avatar_20
            )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.change_profile_dialog, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //init layout
        initLayout()
    }

    private fun initLayout()
    {
        //RecyclerView
        adapter=ChoseAvatarAdapter(avatars)
        binding.recyclerAvatars.adapter=adapter
        adapter.onChoseAvatarAdapterListener=this


        binding.imgClose.setOnClickListener { dismiss() }
        binding.btnCamera.setOnClickListener {
            onChangeProfileDialogListener.onCamera()
            dismiss()
        }
        binding.btnGallery.setOnClickListener {
            onChangeProfileDialogListener.onGallery()
            dismiss()
        }
    }

    interface OnChangeProfileDialogListener{

        fun onCamera()
        fun onGallery()
        fun onAvatar(image: Int)
    }

    override fun onItemChoseAvatar(position: Int) {
        onChangeProfileDialogListener.onAvatar(avatars[position])
        dismiss()
    }
}