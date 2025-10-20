package com.example.articles.models

import android.content.Context
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.example.articles.db.ArticleEntity
import com.example.articles.viewmodels.LocationData
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Date
import java.util.UUID

fun UIArticle.mapToDBEntity() = ArticleEntity(
    idArticle = this.id,
    name = this.name,
    dateCreated = this.dateCreated.time,
    location = this.location,
    quantity = this.quantity,
    unit = this.unit,
    photoUri = this.photoUri,
    deleted = this.deleted,
    updatedAt = Date().time
)

fun ArticleEntity.mapToUIArticle() = UIArticle(
    id = this.idArticle,
    name = this.name,
    dateCreated = Date(this.dateCreated),
    location = this.location,
    quantity = this.quantity,
    unit = this.unit,
    photoUri = this.photoUri,
    deleted = this.deleted,
)

suspend fun ArticleEntity.mapToFirestoreMap(context: Context): Map<String, Any?> {
    var photoFileName: String? = null
    photoUri?.let { uri ->
        val fileName = "article_${idArticle}"
        val downloadUrl = uploadPhotoFile(context, uri.toUri(), fileName)
        if (downloadUrl != null) {
            photoFileName = fileName
        }
    }
    return mapOf(
        "idArticle" to idArticle.toString(),
        "name" to name,
        "dateCreated" to dateCreated,
        "location" to location?.let { location ->
            mapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude
            )
        },
        "quantity" to quantity,
        "unit" to unit.name,
        "photoFileName" to photoFileName,
        "deleted" to deleted,
        "updatedAt" to updatedAt,
        "syncedAt" to System.currentTimeMillis()
    )
}

suspend fun DocumentSnapshot.toArticleEntity(context: Context): ArticleEntity? {
    return try {
        val data = this.data ?: return null
        val localPhotoUri = (data["photoFileName"] as? String)?.let { fileName ->
            downloadPhotoToLocalUri(context, fileName)
        }
        ArticleEntity(
            idArticle = UUID.fromString(data["idArticle"] as? String ?: return null),
            name = data["name"] as? String,
            dateCreated = (data["dateCreated"] as? Long) ?: 0L,
            location = (data["location"] as Map<*, *>).let { location ->
                LocationData(
                    latitude = location["latitude"] as? Double ?: 0.0,
                    longitude = location["longitude"] as? Double ?: 0.0
                )
            },
            quantity = data["quantity"] as? Double,
            unit = try {
                ArticleUnit.valueOf(data["unit"] as? String ?: "PIECE")
            } catch (e: Exception) {
                ArticleUnit.default
            },
            photoUri = localPhotoUri?.toString(),
            deleted = data["deleted"] as? Boolean ?: false,
            updatedAt = (data["updatedAt"] as? Long) ?: 0L
        )
    } catch (e: Exception) {
        Log.e("Firestore", "Error converting document ${this.id} to ArticleEntity", e)
        null
    }
}

suspend fun uploadPhotoFile(context: Context, photoUri: Uri, fileName: String): String? {
    return try {
        val storageRef: StorageReference = Firebase.storage.reference
        val photoRef = storageRef.child("article_photos/$fileName.jpg")
        context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
            photoRef.putStream(inputStream).await()
        }
        photoRef.downloadUrl.await().toString()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun downloadPhotoToLocalUri(context: Context, fileName: String): Uri? {
    return try {
        val storageRef: StorageReference = Firebase.storage.reference
        val photoRef = storageRef.child("article_photos/$fileName.jpg")
        val localFile = File.createTempFile("temp_photo_", ".jpg", context.cacheDir)
        photoRef.getFile(localFile).await()
        Uri.fromFile(localFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}