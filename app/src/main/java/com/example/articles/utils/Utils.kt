package com.example.articles.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import androidx.core.net.toUri
import com.example.articles.models.UILocation
import java.util.Locale

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
    val openStream = {
        if (uri.scheme == "file" || uri.scheme == null) uri.path?.let { FileInputStream(it) }
        else context.contentResolver.openInputStream(uri)
    }

    val originalBitmap = openStream()?.use { BitmapFactory.decodeStream(it) }
        ?: return ByteArray(0)

    val rotation = openStream()?.use { stream ->
        val exif = ExifInterface(stream)
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } ?: 0f

    val source = if (rotation != 0f) {
        Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height,
            Matrix().apply { postRotate(rotation) }, true)
    } else {
        originalBitmap
    }

    val width = (source.width * 0.9).toInt()
    val height = (source.height * 0.9).toInt()
    val resized = source.scale(width, height)
    val outputStream = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
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

fun UILocation.toCoordinatesString(): String =
    String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

fun String.normalizeEmail(): String = trim().lowercase(Locale.ROOT)

fun String.toDecimalOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

fun Double.isInteger() = this % 1 == 0.0

fun Double.tryToInt() = if (isInteger()) toInt() else this