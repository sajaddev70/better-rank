package com.marveloushost.betterrank.hilt.modual

import android.app.Application
import android.content.SharedPreferences
import com.marveloushost.betterrank.repository.AppRepository
import com.marveloushost.betterrank.retrofit.RequestApi
import com.marveloushost.betterrank.viewModel.PhotographyHomeViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class HiltAppRepositoryModule {

    @Provides
    @Singleton
    fun provideRepository(sharedPreferences:SharedPreferences,
                          requestApi: RequestApi
    ): AppRepository
    {
        return AppRepository(sharedPreferences,requestApi)
    }



}