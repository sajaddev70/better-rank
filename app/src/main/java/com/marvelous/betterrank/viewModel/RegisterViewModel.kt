package com.marvelous.betterrank.viewModel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.marvelous.betterrank.hilt.modual.HiltSharedPrefModule
import com.marvelous.betterrank.model.UserModel
import com.marvelous.betterrank.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class RegisterViewModel @Inject constructor(appRepository: AppRepository, application: Application) :BaseViewModel(appRepository, application) {

    val userModel = MutableLiveData<UserModel?>()

    fun validationAndCreateUser(uid:String,name :String,email:String,photoUrl:String?,selectedImageUri: Uri?){
        val validationResult = validator.registerValidation(name)
        if (validationResult.isValid){
            createUser(uid,name,email,photoUrl,selectedImageUri)
        }else{
            showMessage.value = validationResult.error
        }
    }

    private fun createUser(uid:String, name :String, email:String, photoUrl:String?, selectedImageUri: Uri?){

        isLoading.value = true
        viewModelScope.launch {
            try {
                val newUser = appRepository.createUser(
                    uid = uid,
                    name = name,
                    email = email,
                    photoUrl = photoUrl,
                    selectedImageUri = selectedImageUri
                )
                appRepository.saveDataSharedPref(HiltSharedPrefModule.UID, newUser.uid!!)
                userModel.value = newUser
            } catch (e: Exception) {
//                Log.e("BetterRankException",e.toString())
            }finally {
                isLoading.value = false
            }
        }
    }
}