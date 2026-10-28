package com.marvelous.betterrank.viewModel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.marvelous.betterrank.hilt.modual.HiltSharedPrefModule
import com.marvelous.betterrank.model.NoteModel
import com.marvelous.betterrank.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch
import java.util.Date

@HiltViewModel
class CreateNoteViewModel @Inject constructor(appRepository: AppRepository, application: Application) :BaseViewModel(appRepository, application) {

    var note=MutableLiveData<NoteModel>()
    var createNoteResult=MutableLiveData<NoteModel>()
    var updateNoteResult=MutableLiveData<NoteModel>()
    var reminded=MutableLiveData(false)
    var startAt=MutableLiveData<Long>(0)

    fun initNote(noteModel: NoteModel){
        note.value=noteModel
        reminded.value=noteModel.reminded
        startAt.value=noteModel.startAt
    }

    fun validationAndCreateNote(){
        val noteModel=note.value?:NoteModel()
        noteModel.reminded=reminded.value!!
        noteModel.startAt=startAt.value!!
        val validationResult = validator.createNoteValidation(noteModel)
        if (validationResult.isValid){
            if (noteModel.id.isBlank())
                createNote(noteModel)
            else
                updateNote(noteModel)
        }else{
            showMessage.value = validationResult.error
        }
    }

    private fun createNote(noteModel: NoteModel)
    {
        noteModel.userId=appRepository.getDataSharedPref(HiltSharedPrefModule.UID)
        isLoading.value = true
        viewModelScope.launch {
            try {
                createNoteResult.value=appRepository.createNote(noteModel)
            } catch (e: Exception) {
//                Log.e("BetterRankException",e.toString())
            }finally {
                isLoading.value = false
            }
        }
    }

    private fun updateNote(noteModel: NoteModel)
    {
        isLoading.value = true
        viewModelScope.launch {
            try {
                updateNoteResult.value=appRepository.updateNote(noteModel)
            } catch (e: Exception) {
//                Log.e("BetterRankException",e.toString())
            }finally {
                isLoading.value = false
            }
        }
    }

}