package com.marveloushost.betterrank.viewModel

import android.app.Application
import com.marveloushost.betterrank.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class AiAssistantViewModel @Inject constructor(appRepository: AppRepository, application: Application) :BaseViewModel(appRepository, application) {
}