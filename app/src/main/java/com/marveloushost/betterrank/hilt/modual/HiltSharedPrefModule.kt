package com.marveloushost.betterrank.hilt.modual

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class HiltSharedPrefModule
{
    companion object {
        const val USER_STATUS = "UserStatus"
        const val INTRO_STATUS = "IntroStatus"
        const val UID = "uid"
        const val HOME = "home"
        const val TOKEN = "Token"
        const val REFRESH_TOKEN = "RefreshToken"
    }


    @Provides
    @Singleton
    fun provideSharedPref(@ApplicationContext context: Context): SharedPreferences
    {
        return context.getSharedPreferences("dataUser", Context.MODE_PRIVATE)
    }
}