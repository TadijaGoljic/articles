package com.example.articles.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import androidx.core.net.toUri

fun copyUriToUri(sourceUri: Uri, destinationUri: Uri, context: Context): Uri? {
    return try {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                input.copyTo(output)
            }
        }
        destinationUri
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun resizeImage(context: Context, uri: Uri): ByteArray {
    val inputStream = if (uri.scheme == "file" || uri.scheme == null) {
        uri.path?.let { FileInputStream(it) }
    } else {
        context.contentResolver.openInputStream(uri)
    }
    val originalBitmap = BitmapFactory.decodeStream(inputStream)
    inputStream?.close()
    val width = (originalBitmap.width * 0.9).toInt()
    val height = (originalBitmap.height * 0.9).toInt()
    val resizedBitmap = originalBitmap.scale(width, height)
    val outputStream = ByteArrayOutputStream()
    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    return outputStream.toByteArray()
}

fun String.isValidUri(): Boolean {
    return try {
        val parsed = this.toUri()
        !parsed.scheme.isNullOrBlank()
    } catch (_: Exception) {
        false
    }
}