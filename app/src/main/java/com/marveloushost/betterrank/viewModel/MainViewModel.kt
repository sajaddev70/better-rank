package com.marveloushost.betterrank.viewModel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.marveloushost.betterrank.repository.AppRepository

@HiltViewModel
class MainViewModel @Inject constructor(appRepository: AppRepository, application: Application) :BaseViewModel(appRepository, application) {
    var whichFragment= MutableLiveData(1)

}