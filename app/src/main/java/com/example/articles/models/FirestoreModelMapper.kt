package com.example.articles.models

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.core.net.toUri
import com.example.articles.ArticlesApplication
import com.example.articles.db.entities.ArticleEntity
import com.example.articles.db.entities.GroupEntity
import com.example.articles.utils.FileManager
import com.example.articles.utils.FileManager.Companion.IMAGE_PREFIX
import com.example.articles.utils.FileManager.Companion.IMAGE_SUFFIX
import com.example.articles.utils.resizeImage
import com.google.firebase.firestore.DocumentSnapshot
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreModelMapper @Inject constructor(
    private val fileManager: FileManager
) {

    fun toArticlesFirestoreMap(articleEntity: ArticleEntity, context: Context): Map<String, Any?> {
        val file = articleEntity.photoUri?.let { uri ->
            fileManager.getFileFromUri(context, uri.toUri())
        }
        var photoBytes: ByteArray? = null
        if (file != null) {
            while (file.length() > 10240) {
                photoBytes = resizeImage(context, file.toUri())
                file.writeBytes(photoBytes)
            }
            if (photoBytes == null && file.exists()) {
                photoBytes = file.readBytes()
            }
        }
        return mapOf(
            "name" to articleEntity.name,
            "idArticle" to articleEntity.idArticle.toString(),
            "dateCreated" to articleEntity.dateCreated,
            "location" to articleEntity.location?.let { location ->
                location.latitude.toString() + "," + location.longitude.toString()
            },
            "quantity" to articleEntity.quantity,
            "unit" to articleEntity.unit.name,
            "deleted" to articleEntity.deleted,
            "photo" to if (photoBytes != null) Base64.encodeToString(photoBytes, Base64.DEFAULT) else null,
            "updatedAt" to articleEntity.updatedAt,
            "syncedAt" to System.currentTimeMillis(),
            "groupId" to articleEntity.groupId.toString(),
            "description" to articleEntity.description,
            "websiteLink" to articleEntity.websiteLink,
            "pricePerUnit" to articleEntity.pricePerUnit,
            "bought" to articleEntity.bought
        )
    }

    fun toArticleEntity(snapshot: DocumentSnapshot, context: Context): ArticleEntity? {
        val data = snapshot.data ?: return null
        val file = File(context.cacheDir, "$IMAGE_PREFIX${System.currentTimeMillis()}$IMAGE_SUFFIX")
        val photo = data["photo"] as? String
        if (photo != null) file.writeBytes(Base64.decode(photo, Base64.DEFAULT))
        return ArticleEntity(
            idArticle = UUID.fromString(data["idArticle"] as? String ?: return null),
            name = data["name"] as String,
            dateCreated = (data["dateCreated"] as? Long) ?: 0L,
            location = (data["location"] as? String)?.let { locationString ->
                Location(
                    latitude = locationString.split(',')[0].toDouble(),
                    longitude = locationString.split(',')[1].toDouble()
                )
            },
            quantity = data["quantity"] as? Double,
            unit = ArticleUnit.valueOf(data["unit"] as? String ?: "PIECE"),
            photoUri = if (photo == null) null else file.toUri().toString(),
            deleted = data["deleted"] as? Boolean ?: false,
            updatedAt = (data["updatedAt"] as? Long) ?: 0L,
            groupId = UUID.fromString(data["groupId"] as? String) ?: UUID.randomUUID(),
            description = data["description"] as? String,
            websiteLink = data["websiteLink"] as? String,
            pricePerUnit = data["pricePerUnit"] as? Double,
            bought = data["bought"] as? Boolean ?: false
        )
    }

    fun toGroupsFirestoreMap(groupEntity: GroupEntity) = mapOf(
        "idGroup" to groupEntity.idGroup.toString(),
        "name" to groupEntity.name,
        "updatedAt" to groupEntity.updatedAt,
        "syncedAt" to System.currentTimeMillis(),
        "deleted" to groupEntity.deleted
    )

    fun toGroupEntity(snapshot: DocumentSnapshot) = GroupEntity(
        idGroup = (snapshot["idGroup"] as? String)?.let { UUID.fromString(it) } ?: UUID.randomUUID(),
        name = snapshot["name"] as? String ?: "",
        updatedAt = snapshot["updatedAt"] as? Long ?: 0L,
        deleted = snapshot["deleted"] as? Boolean ?: false
    )

}
