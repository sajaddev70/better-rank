package com.marveloushost.betterrank.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.marveloushost.betterrank.BuildConfig
import com.marveloushost.betterrank.pdf.MutableSingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.marveloushost.betterrank.repository.AppRepository
import com.marveloushost.betterrank.validator.Validator
import org.json.JSONObject
import retrofit2.HttpException
import java.text.DecimalFormat
import java.text.NumberFormat

@HiltViewModel
open class BaseViewModel @Inject constructor(open val appRepository: AppRepository, application: Application) : AndroidViewModel(application)
{
    var showMessage: MutableLiveData<String> = MutableLiveData()
    var showSnackBar: MutableLiveData<String> = MutableLiveData()
    var isLoading: MutableLiveData<Boolean> = MutableLiveData()
    var validator=Validator(application)
    protected open val _navigateBack = MutableSingleLiveEvent<Unit>()
    open val navigateBack: LiveData<Unit> = _navigateBack

    fun appVersionName():String{
        return BuildConfig.VERSION_NAME
    }

    fun getMoneyFormat(amount: Double): String {
        val formatter: NumberFormat = DecimalFormat("#,###")
        return formatter.format(amount)
    }

    fun timestampToDateTime(timestamp: Long): String {
        if (timestamp==0L)
            return ""
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd  HH:mm", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getDefault()
        return sdf.format(java.util.Date(timestamp))
    }

    fun errorHandler(error: Throwable): String {
        return when (error) {
            is HttpException -> {
                val errorJson = error.response()?.errorBody()?.string()
                try {
                    val jsonObject = JSONObject(errorJson ?: "")
                    jsonObject.optString("message", "خطایی رخ داده است")
                } catch (e: Exception) {
                    "خطایی در پردازش پاسخ سرور رخ داد"
                }
            }
            else -> error.message ?: "خطای نامشخص"
        }
    }
    fun setLoading(show: Boolean) {
        isLoading.postValue(show)
    }
}