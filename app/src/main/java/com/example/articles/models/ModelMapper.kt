package com.example.articles.models

import android.content.Context
import android.util.Base64
import androidx.core.net.toUri
import com.example.articles.db.ArticleEntity
import com.example.articles.ui.utils.FileManager
import com.example.articles.ui.utils.resizeImage
import com.example.articles.viewmodels.AddNewArticleViewModel.Companion.IMAGE_PREFIX
import com.example.articles.viewmodels.AddNewArticleViewModel.Companion.IMAGE_SUFFIX
import com.example.articles.viewmodels.LocationData
import com.google.firebase.firestore.DocumentSnapshot
import java.io.File
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelMapper @Inject constructor(
    private val fileManager: FileManager
) {

    fun mapToDBEntity(uiArticle: UIArticle) = ArticleEntity(
        idArticle = uiArticle.id,
        name = uiArticle.name,
        dateCreated = uiArticle.dateCreated.time,
        location = uiArticle.location,
        quantity = uiArticle.quantity,
        unit = uiArticle.unit,
        photoUri = uiArticle.photoUri,
        deleted = uiArticle.deleted,
        updatedAt = Date().time
    )

    fun mapToUIArticle(articleEntity: ArticleEntity) = UIArticle(
        id = articleEntity.idArticle,
        name = articleEntity.name,
        dateCreated = Date(articleEntity.dateCreated),
        location = articleEntity.location,
        quantity = articleEntity.quantity,
        unit = articleEntity.unit,
        photoUri = articleEntity.photoUri
    )

    fun mapToFirestoreMap(articleEntity: ArticleEntity, context: Context): Map<String, Any?> {
        val file = articleEntity.photoUri?.let { uri ->
            fileManager.getFileFromUri(context, uri.toUri())
        }
        var photoBytes = file?.readBytes()
        while (file != null && file.length() > 10240) {
            photoBytes = resizeImage(context, file.toUri())
            file.writeBytes(photoBytes)
        }
        return mapOf(
            "name" to articleEntity.name,
            "idArticle" to articleEntity.idArticle.toString(),
            "dateCreated" to articleEntity.dateCreated,
            "location" to articleEntity.location?.let { loc ->
                mapOf(
                    "latitude" to loc.latitude,
                    "longitude" to loc.longitude
                )
            },
            "quantity" to articleEntity.quantity,
            "unit" to articleEntity.unit.name,
            "deleted" to articleEntity.deleted,
            "photo" to Base64.encode(photoBytes, Base64.DEFAULT).map { it.toInt() }.toList(),
            "updatedAt" to articleEntity.updatedAt,
            "syncedAt" to System.currentTimeMillis()
        )
    }

    fun toArticleEntity(snapshot: DocumentSnapshot, context: Context): ArticleEntity? {
        val data = snapshot.data ?: return null
        val file = File(context.cacheDir, "$IMAGE_PREFIX${System.currentTimeMillis()}$IMAGE_SUFFIX")
        val photoBytes = data["photo"] as? List<Int> ?: return null
        file.writeBytes(Base64.decode(photoBytes.map { it.toByte() }.toByteArray(), Base64.DEFAULT) ?: return null)
        return ArticleEntity(
            idArticle = UUID.fromString(data["idArticle"] as? String ?: return null),
            name = data["name"] as? String,
            dateCreated = (data["dateCreated"] as? Long) ?: 0L,
            location = (data["location"] as? Map<*, *>)?.let { location ->
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
            photoUri = file.path,
            deleted = data["deleted"] as? Boolean ?: false,
            updatedAt = (data["updatedAt"] as? Long) ?: 0L
        )
    }

}