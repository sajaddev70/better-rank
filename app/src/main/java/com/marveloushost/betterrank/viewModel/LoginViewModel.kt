package com.marveloushost.betterrank.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.marveloushost.betterrank.hilt.modual.HiltSharedPrefModule
import com.marveloushost.betterrank.model.UserModel
import com.marveloushost.betterrank.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(appRepository: AppRepository, application: Application) :BaseViewModel(appRepository, application) {

    var userModel = UserModel()
    val isExistingUser = MutableLiveData<Boolean>()
    val isExistingHome = MutableLiveData<Boolean>()

    fun loginWithGoogle(idToken: String) {
        isLoading.value = true
        viewModelScope.launch {
            try {
                val firebaseUser = appRepository.authenticateWithGoogle(idToken)
                val uid = firebaseUser.uid
                val name = firebaseUser.displayName
                val email = firebaseUser.email
                val photoUrl = firebaseUser.photoUrl?.toString()
                getUser(uid,name,email,photoUrl)
                isLoading.value = false

            } catch (e: Exception) {
//                Log.e("BetterRankException",e.toString())
                isLoading.value = false
            }
        }
    }

    private fun getUser(uid: String, name: String?, email: String?, photoUrl: String?){
        isLoading.value = true
        viewModelScope.launch {
            try {
                val user = appRepository.getUser(uid)
                if (user != null) {
                    appRepository.saveDataSharedPref(HiltSharedPrefModule.UID,uid)
                    userModel = user
                    isExistingUser.value = true
                } else {
                    userModel= UserModel(
                        uid=uid,
                        name=name,
                        email=email,
                        photoUrl=photoUrl
                    )
                    isExistingUser.value = false
                }

            }catch (e:Exception){
//                Log.e("BetterRankException",e.toString())
            }finally {
                isLoading.value = false
            }
        }
    }

    fun getHomes(uid: String)
    {
        isLoading.value = true
        viewModelScope.launch {
            try {
                val homes=appRepository.getHomes(uid)
                isExistingHome.value=homes.isNotEmpty()
            }catch (e:Exception){
//                Log.e("BetterRankException",e.toString())
            }finally {
                isLoading.value = false
            }
        }
    }
}