package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CourierOcrEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processBitmap(bitmap: Bitmap, serialIndex: Int): ExtractedShipment {
        val image = InputImage.fromBitmap(bitmap, 0)
        return processInputImage(image, serialIndex)
    }

    suspend fun processUri(context: Context, uri: Uri, serialIndex: Int): ExtractedShipment {
        val image = InputImage.fromFilePath(context, uri)
        return processInputImage(image, serialIndex)
    }

    suspend fun processInputImage(image: InputImage, serialIndex: Int): ExtractedShipment {
        val rawText = try {
            suspendCancellableCoroutine<String> { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        continuation.resume(visionText.text)
                    }
                    .addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "OCR module initializing"
            if (errorMsg.contains("Waiting for the text optional module", ignoreCase = true)) {
                "Waiting for OCR model initialization. Please tap shutter again in a moment."
            } else {
                "OCR Scan note: $errorMsg"
            }
        }

        return LogisticsDataExtractor.extract(rawText, serialIndex)
    }

    fun close() {
        recognizer.close()
    }
}
