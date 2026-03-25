package com.example.articles.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.abs

fun Double.isInteger(): Boolean = this % 1 == 0.0

enum class DateUnit {
    DAY, HOUR, MINUTE, SECOND
}

data class UIDate(
    val value: Int,
    val unit: DateUnit
)

fun Date.toUIDate(): UIDate {

    val now = Date()
    val diffMillis = abs(now.time - this.time)

    val days = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()
    val hours = TimeUnit.MILLISECONDS.toHours(diffMillis).toInt()
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis).toInt()
    val seconds = TimeUnit.MILLISECONDS.toSeconds(diffMillis).toInt()

    return if (days >= 1) {
        UIDate(value = days, unit = DateUnit.DAY)
    } else if (hours >= 1) {
        UIDate(value = hours, unit = DateUnit.HOUR)
    } else if (minutes >= 1) {
        UIDate(value = minutes, unit = DateUnit.MINUTE)
    } else {
        UIDate(value = seconds, unit = DateUnit.SECOND)
    }

}

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