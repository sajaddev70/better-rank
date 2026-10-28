package com.marvelous.betterrank.instructionstranslation


import android.util.Log
import com.marvelous.betterrank.hilt.modual.HiltNetworkModule
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.ChatModel
import com.openai.models.chat.completions.ChatCompletionCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object InstructionsTranslator {

    private val openAIClient = OpenAIOkHttpClient.builder()
        .apiKey(HiltNetworkModule.GPT_API_KEY)
        .build()

    /**
     * Translates line-by-line text generated from the selected instructions sections.
     *
     * @param text the raw multi-line string (exact lines user selected)
     * @param lang target language ("Turkish", "Spanish", etc.)
     * @return the translated text as a multi-line string with the same line order
     */
    suspend fun translate(text: String, lang: String): String =
        withContext(Dispatchers.IO) {

//            Log.d("INSTR_TRANSLATOR", "START → targetLang=$lang, lines=${text.lines().size}")

            if (text.isBlank()) {
//                Log.w("INSTR_TRANSLATOR", "Input text blank → return original")
                return@withContext text
            }

            // SYSTEM PROMPT
            val systemPrompt = """
                You are a professional translator. Translate the following text to "$lang" only.
                - Keep meaning, tone, and structure.
                - Do not add, remove, or explain anything.
                - Translate strictly line by line.
                - Output ONLY the translated text.
                - No markdown, no JSON, no quotes, no numbering.
                - Preserve line count exactly.
            """.trimIndent()

            val params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_5_MINI)
                .addSystemMessage(systemPrompt)
                .addUserMessage(text)
                .build()

            return@withContext try {

                val start = System.currentTimeMillis()
                val response = openAIClient.chat().completions().create(params)
                val end = System.currentTimeMillis()

//                Log.d("INSTR_TRANSLATOR", "GPT SUCCESS → time=${end - start}ms")

                val content = response.choices()
                    .firstOrNull()
                    ?.message()
                    ?.content()
                    ?.toString()
                    ?.trim()

                if (content.isNullOrBlank()) {
//                    Log.e("INSTR_TRANSLATOR", "GPT returned NULL/blank → fallback to original")
                    return@withContext text
                }

//                Log.d("INSTR_TRANSLATOR", "TRANSLATED_LINES=${content.lines().size}")
                content

            } catch (e: Exception) {
//                Log.e("INSTR_TRANSLATOR", "GPT FAILED → ${e.message}", e)
                text // fallback
            }
        }
}
