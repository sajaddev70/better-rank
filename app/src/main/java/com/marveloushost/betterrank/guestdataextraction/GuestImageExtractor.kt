package com.marveloushost.betterrank.guestdataextraction

import android.util.Log
import com.google.gson.Gson
import com.marveloushost.betterrank.hilt.modual.HiltNetworkModule
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.ChatModel
import com.openai.models.chat.completions.ChatCompletionCreateParams
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object GuestImageExtractor {

    private val visionClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val gson = Gson()

    suspend fun extract(jpegBase64: String): GuestExtractedData = withTimeout(25_000) {

        Log.d("Respina", "🤖 GPT (Responses API) extraction started")
        Log.d("Respina", "📏 JPEG base64 length = ${jpegBase64.length}")

        val bodyJson = """
        {
          "model": "gpt-5-mini",
          "input": [
            {
              "role": "user",
              "content": [
                {
  "type": "input_text",
  "text": "Extract guest booking information from the image.\n\nRespond with ONLY a single JSON object using EXACTLY these keys and formats:\n\n{\n  \"full_name\": string,\n  \"check_in\": \"yyyy/MM/dd\",\n  \"check_out\": \"yyyy/MM/dd\",\n  \"adults\": number,\n  \"has_children\": boolean,\n  \"nationality\": string,\n  \"nationality_code\": string,\n  \"language\": string,\n  \"phone_code\": string,\n  \"phone_number\": string,\n  \"notes\": string\n}\n\nRules:\n- has_children must be true or false\n- phone_code must include +\n- phone_number must NOT include country code\n- nationality_code must be ISO-2 uppercase (e.g. \"IR\", \"US\", \"FR\") or null\n- nationality_code must match nationality\n- If a value is missing, use null\n- No extra fields\n- No explanation\n- No markdown"
}
,
                {
                  "type": "input_image",
                  "image_url": "data:image/jpeg;base64,$jpegBase64"
                }
              ]
            }
          ]
        }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/responses")
            .addHeader("Authorization", "Bearer ${HiltNetworkModule.GPT_API_KEY}")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        val start = System.currentTimeMillis()
        val response = visionClient.newCall(request).execute()
        val duration = System.currentTimeMillis() - start

        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code}")
        }


        Log.d("Respina", "⏱️ GPT response time = ${duration}ms")

        val rawBody = response.body?.string()
            ?: throw IllegalStateException("Empty response body")

        Log.d("Respina", "📨 Raw Responses API output:")
        Log.d("Respina", rawBody)

         parseResponse(rawBody)
    }

    private fun parseResponse(raw: String): GuestExtractedData {

        val root = gson.fromJson(raw, Map::class.java)

        val outputs = root["output"] as? List<*>
            ?: throw IllegalStateException("No output array")

        val outputText = outputs
            .flatMap { (it as Map<*, *>)["content"] as? List<*> ?: emptyList<Any>() }
            .firstOrNull { (it as Map<*, *>)["type"] == "output_text" }
            ?.let { (it as Map<*, *>)["text"] as? String }
            ?: throw IllegalStateException("No output_text found")

        Log.d("Respina", "📄 Extracted text (raw):")
        Log.d("Respina", outputText)

        val cleaned = outputText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return gson.fromJson(cleaned, GuestExtractedData::class.java)
    }

}


data class GuestExtractedData(
    val full_name: String?,
    val check_in: String?,
    val check_out: String?,
    val adults: Any?,
    val has_children: Boolean?,
    val nationality: String?,
    val nationality_code: String?,
    val language: String?,
    val phone_code: String?,
    val phone_number: String?,
    val notes: String?
)


