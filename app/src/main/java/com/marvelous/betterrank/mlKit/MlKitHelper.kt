package com.marvelous.betterrank.mlKit

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object MlKitHelper {

    private val recognizer by lazy {
        TextRecognition
            .getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    fun scanImageForUrls(
        context: Context,
        uri: Uri,
        onResult: (List<String>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { text ->
                    val urls = extractUrls(text.text)
                        //.map(::normalizeUrl)
                        //.filter { it.contains("airbnb", ignoreCase = true) }
                        //.filter { it.contains("?", ignoreCase = true) }
                        //.map(::withoutQuery)
                        .distinct()
                    onResult(urls)
                }
                .addOnFailureListener { e ->
                    onError(e)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }

    // OCR often inserts spaces ("rooms / 123"), wraps the id onto the next line,
    // or misreads digits as letters (0 -> O, 1 -> l/I)
    private val roomRegex = Regex("""(?i)rooms?\s*/\s*([0-9OoIl|]{5,})""")
    private val shortLinkRegex = Regex("""(?i)airbnb\.[a-z.]{2,7}/(h|slink)/([A-Za-z0-9_-]+)""")

    private fun extractUrls(text: String): List<String> {
        val urls = mutableListOf<String>()

        // Try the raw text first, then with all whitespace removed to join wrapped lines
        val candidates = listOf(text, text.replace(Regex("""\s+"""), ""))
        for (candidate in candidates) {
            roomRegex.findAll(candidate).forEach { matchResult ->
                val roomId = fixOcrDigits(matchResult.groupValues[1])
                urls.add("https://www.airbnb.com/rooms/$roomId")
            }
            if (urls.isNotEmpty()) break
        }

        // Shared links like airbnb.com/h/<slug> or airbnb.com/slink/<code>
        if (urls.isEmpty()) {
            val compact = text.replace(Regex("""\s+"""), "")
            shortLinkRegex.findAll(compact).forEach { matchResult ->
                val (type, code) = matchResult.destructured
                urls.add("https://www.airbnb.com/${type.lowercase()}/$code")
            }
        }

        return urls.distinct()
    }

    private fun fixOcrDigits(id: String): String = id
        .replace('O', '0').replace('o', '0')
        .replace('I', '1').replace('l', '1').replace('|', '1')

    private fun normalizeUrl(url: String): String {
        return if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "https://${url.removePrefix("www.")}"
        }
    }

    fun withoutQuery(url: String): String {
        val idx = url.indexOf('?')
        return if (idx >= 0) url.substring(0, idx) else url
    }

}