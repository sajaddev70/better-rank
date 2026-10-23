package com.marveloushost.betterrank.mlKit

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

    private fun extractUrls(text: String): List<String> {
        val urls = mutableListOf<String>()

        val roomRegex = Regex("""rooms/(\d+)""")

        roomRegex.findAll(text).forEach { matchResult ->
            val roomId = matchResult.groupValues[1]
            val url = "https://www.airbnb.com/rooms/$roomId"
            urls.add(url)
        }

        return urls.distinct()
    }

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