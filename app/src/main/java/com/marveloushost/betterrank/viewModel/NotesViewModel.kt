package com.marveloushost.betterrank.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.marveloushost.betterrank.hilt.modual.HiltSharedPrefModule
import com.marveloushost.betterrank.model.HomeModel
import com.marveloushost.betterrank.model.NoteModel
import com.marveloushost.betterrank.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class NotesViewModel @Inject constructor(appRepository: AppRepository, application: Application) :BaseViewModel(appRepository, application) {

    private val _resultGetNotes = MutableLiveData<MutableList<NoteModel>>()
    val resultGetNotes: MutableLiveData<MutableList<NoteModel>> = _resultGetNotes
    var resultDeleteNote=MutableLiveData<Boolean>()

    fun getNotes()
    {
        isLoading.value = true
        viewModelScope.launch {
            try {
                val uid=appRepository.getDataSharedPref(HiltSharedPrefModule.UID)
                _resultGetNotes.value=appRepository.getNotes(uid)
            }catch (e:Exception){
//                Log.e("BetterRankException",e.toString())
            }finally {
                isLoading.value = false
            }
        }
    }

    fun deleteNote(id:String)
    {
        isLoading.value = true
        viewModelScope.launch {
            try {
                resultDeleteNote.value=appRepository.deleteNote(id)
            }catch (e:Exception){
//                Log.e("BetterRankException",e.toString())
            }finally {
                isLoading.value = false
            }
        }
    }
}