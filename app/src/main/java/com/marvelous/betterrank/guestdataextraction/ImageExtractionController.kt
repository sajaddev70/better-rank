package com.marvelous.betterrank.guestdataextraction

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.util.Log.e
import android.widget.Toast
import com.google.gson.JsonSyntaxException
import com.marvelous.betterrank.ui.AddGuestActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

object ImageExtractionController {
    private suspend fun handleError(
        context: Context,
        message: String,
        onError: () -> Unit
    ) {
//        Log.e("Respina", message)

        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onError()
        }
    }



    private fun normalizeAdults(value: Any?): String? {
        return when (value) {
            is Number -> value.toInt().toString()   // 2.0 → 2
            is String -> value.trim()
            else -> null
        }
    }



    fun processImage(context: Context, imageUri: Uri,
                     onFinished: () -> Unit,
                     onError: () -> Unit) {
        Log.d("Respina", "🧠 Starting image extraction")
        Log.d("Respina", "🖼️ Image URI = $imageUri")

        CoroutineScope(Dispatchers.IO).launch {
try{
    val jpegBase64 = jpegBase64FromUri(context, imageUri)

            Log.d("Respina", "🔐 JPEG base64 length = ${jpegBase64.length}")

    val extracted = GuestImageExtractor.extract(jpegBase64)

            Log.d("Respina", "✅ Extraction success")
            Log.d("Respina", "👤 Extracted name = ${extracted.full_name}")
            Log.d("Respina", "📅 Check-in = ${extracted.check_in}")
            Log.d("Respina", "📅 Check-out = ${extracted.check_out}")
            Log.d("Respina", "📝 Notes = ${extracted.notes}")
            Log.d("Respina", "📝 Nationality = ${extracted.nationality}")
            Log.d("Respina", "📝 Language = ${extracted.language}")
            Log.d("Respina", "📝 Phonenumber = ${extracted.phone_number}")
            Log.d("Respina", "📝 Phonecode= ${extracted.phone_code}")

            withContext(Dispatchers.Main) {
                Log.d("Respina", "➡️ Navigating to AddGuestActivity")

                val intent = Intent(context, AddGuestActivity::class.java).apply {
                    putExtra("AUTO_NAME", extracted.full_name)
                    putExtra("AUTO_IN", extracted.check_in)
                    putExtra("AUTO_OUT", extracted.check_out)
                    putExtra("AUTO_ADULTS", normalizeAdults(extracted.adults))
                    putExtra("AUTO_HAS_CHILDREN", extracted.has_children ?: false)
                    putExtra("AUTO_NATIONALITY", extracted.nationality)
                    putExtra("AUTO_NATIONALITY_CODE", extracted.nationality_code)
                    putExtra("AUTO_LANGUAGE", extracted.language)
                    putExtra("AUTO_PHONE_CODE", extracted.phone_code)
                    putExtra("AUTO_PHONE_NUMBER", extracted.phone_number)
                    putExtra("AUTO_NOTES", extracted.notes)
                }

                context.startActivity(intent)
                onFinished()
            }
        } catch (e: TimeoutCancellationException) {
            handleError(context, "Please try again", onError)

        } catch (e: IOException) {
            handleError(context, "Network error. Please try again", onError)

        } catch (e: JsonSyntaxException) {
            handleError(context, "Could not read booking image", onError)

        } catch (e: Exception) {
            handleError(context, "Something went wrong. Please try again", onError)
        }
        }
    }
}
fun jpegBase64FromUri(context: Context, uri: Uri): String {
    val bytes = context.contentResolver.openInputStream(uri)!!.readBytes()
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}


