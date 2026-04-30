package com.example.articles.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun photoUri(): Uri {
        val currentTimestamp = System.currentTimeMillis()
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            File(context.cacheDir, buildString {
                append(IMAGE_PREFIX)
                append(currentTimestamp)
                append(IMAGE_SUFFIX)
            })
        )
        context.grantUriPermission(context.packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        return uri
    }

    companion object {
        const val IMAGE_PREFIX = "article"
        const val IMAGE_SUFFIX = ".jpg"
    }

    fun getFileFromUri(context: Context, uri: Uri): File? {
        if (uri.scheme == "file" || uri.scheme == null) {
            return uri.path?.let { File(it) }
        }
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("temp", null, context.cacheDir)
            tempFile.outputStream().use { output -> inputStream.copyTo(output) }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}