package com.marveloushost.betterrank.util


import android.content.Context
import androidx.core.content.edit

object Prefs {

    private const val FILE = "home_prefs"
    private const val KEY_HOME_ID = "home_id"

    fun saveHomeId(context: Context, homeId: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_HOME_ID, homeId)
            }
    }

    fun getHomeId(context: Context): String {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_HOME_ID, "") ?: ""
    }
}
