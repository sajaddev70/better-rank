package com.marveloushost.betterrank.validator

import android.content.Context
import android.util.Patterns
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.model.HomeModel
import com.marveloushost.betterrank.model.NoteModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Validator @Inject constructor(@ApplicationContext private val context: Context) {

    data class ValidationResult(
        val isValid: Boolean,
        val error: String = ""
    )

    fun registerValidation(name: String): ValidationResult {
        var error = ""
        if (name.isBlank() || name.length < 2)
            error = context.getString(R.string.name_invalid)

        return ValidationResult(error.isEmpty(), error)
    }

    fun getAirbnbInfoValidation(url: String): ValidationResult {
        var error = ""
        if (url.isBlank() || !Patterns.WEB_URL.matcher(url).matches() || !url.contains("airbnb", ignoreCase = true)) {
            error = context.getString(R.string.url_invalid)
        }
        return ValidationResult(error.isEmpty(), error)
    }

    fun createHomeValidation(homeModel: HomeModel): ValidationResult {
        // Removed homeModel.name validation
        return ValidationResult(isValid = true)
    }

    fun createNoteValidation(noteModel: NoteModel): ValidationResult {
        var error = ""
        if (noteModel.title.isBlank() || noteModel.title.length < 2)
            error = context.getString(R.string.title_invalid)
        else if (noteModel.reminded && noteModel.startAt == 0L) {
            error = context.getString(R.string.start_at_invalid)
        }
        return ValidationResult(error.isEmpty(), error)
    }
}