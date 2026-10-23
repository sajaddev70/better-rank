package com.marveloushost.betterrank.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.databinding.DataBindingUtil
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.databinding.MainBinding
import com.marveloushost.betterrank.viewModel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import com.marveloushost.betterrank.fragment.AiAssistant
import com.marveloushost.betterrank.fragment.Home
import com.marveloushost.betterrank.fragment.MessagesFragment
import com.marveloushost.betterrank.fragment.Notes
import com.marveloushost.betterrank.permissionManager.PermissionManager

@AndroidEntryPoint
class Main : BaseActivity() {

    val viewModel:MainViewModel by viewModels()
    val binding: MainBinding by lazy { DataBindingUtil.setContentView(this, R.layout.main) }
    private  val home by lazy { Home() }
    private  val guests by lazy { Guests() }
    private  val aiAssistant by lazy { AiAssistant() }
    private  val messagesFragment by lazy { MessagesFragment() }
    private  val notes by lazy { Notes() }
    private val transaction by lazy { supportFragmentManager.beginTransaction() }
    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()
        onClickView()
        checkAndRequestNotificationPermission()
    }

    override fun initView() {

        //ViewModel
        binding.viewModel=viewModel
        binding.lifecycleOwner=this

        //Fragment
        transaction.setReorderingAllowed(true)
        transaction.replace(R.id.fl_home,home,"Home")
//        transaction.replace(R.id.fl_aiAssistant,aiAssistant,"AiAssistant")
        transaction.replace(R.id.fl_guests,guests,"Guests")
        transaction.replace(R.id.fl_messages,messagesFragment,"Messages")
        transaction.replace(R.id.fl_notes,notes,"Notes")
        transaction.commit()

        //BottomNavigation
        binding.bottomNavigation.selectedItemId= R.id.fl_home
    }

    override fun onClickView() {

        viewModel.showMessage.observe(this) { value ->
            showMessage(value)
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            changeFragment(item.itemId)
            true
        }
    }

    private fun changeFragment(id:Int){

        when(id) {
            R.id.home -> {
                viewModel.whichFragment.value=1
            }
//
            R.id.guests -> {
                viewModel.whichFragment.value=2
            }
            R.id.messages -> {
                viewModel.whichFragment.value=3
            }
            R.id.notes -> {
                viewModel.whichFragment.value=4
                notes.getData()
            }
        }
    }
    override fun onBackPressed() {

        // If not in home move to home
        if (viewModel.whichFragment.value != 1) {
            viewModel.whichFragment.value = 1
            binding.bottomNavigation.selectedItemId = R.id.home
            return
        }

        // If in home — first back press
        if (!doubleBackToExitPressedOnce) {
            doubleBackToExitPressedOnce = true
            showMessage("Press back again to exit")

            Handler(mainLooper).postDelayed({
                doubleBackToExitPressedOnce = false
            }, 2000)
            return
        }

        // Show exit confirmation dialog on second press
        showExitDialog()
    }
    private fun showExitDialog() {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Do you want to exit?")
            .setPositiveButton("Confirm") { _, _ ->
                finishAffinity()  // exit app
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }



    private fun checkAndRequestNotificationPermission(){
        if(!PermissionManager.isPermissionGranted(this, Manifest.permission.POST_NOTIFICATIONS))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionManager.requestPermission(this, Manifest.permission.POST_NOTIFICATIONS, PermissionManager.PERMISSION_REQUEST_CODE)
            }
    }
}