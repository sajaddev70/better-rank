package com.marveloushost.betterrank.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.marveloushost.betterrank.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(appRepository: AppRepository, application: Application) :BaseViewModel(appRepository, application) {
    var pingGoogleResult = MutableLiveData<Int>()
    val isExistingHome = MutableLiveData<Boolean>()


    fun pingGoogle(){
        viewModelScope.launch {
            isLoading.value=true
            appRepository.pingGoogle().fold(
                onSuccess = {response->
                    pingGoogleResult.value=1
                },
                onFailure = {error->
                    pingGoogleResult.value=2
                }
            )
            isLoading.value=false
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