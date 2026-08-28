package com.example.articles.models.mappers

import android.content.Context
import android.util.Base64
import androidx.core.net.toUri
import com.example.articles.db.entities.ArticleEntity
import com.example.articles.db.entities.CommentEntity
import com.example.articles.db.entities.GroupEntity
import com.example.articles.models.ArticleUnit
import com.example.articles.models.InvitationStatus
import com.example.articles.models.UIInvitation
import com.example.articles.models.UILocation
import com.example.articles.utils.FileManager
import com.example.articles.utils.FileManager.Companion.IMAGE_PREFIX
import com.example.articles.utils.FileManager.Companion.IMAGE_SUFFIX
import com.example.articles.utils.resizeImage
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreModelMapper @Inject constructor(
    private val fileManager: FileManager,
    @ApplicationContext private val context: Context
) {

    fun toArticlesFirestoreMap(articleEntity: ArticleEntity): Map<String, Any?> {
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
            "photo" to if (photoBytes != null) Base64.encodeToString(photoBytes, Base64.DEFAULT) else null,
            "updatedAt" to articleEntity.updatedAt,
            "syncedAt" to System.currentTimeMillis(),
            "groupId" to articleEntity.groupId.toString(),
            "description" to articleEntity.description,
            "websiteLink" to articleEntity.websiteLink,
            "price" to articleEntity.price,
            "bought" to articleEntity.bought
        )
    }

    fun toArticleEntity(snapshot: DocumentSnapshot): ArticleEntity? {
        val data = snapshot.data ?: return null
        val articleId = data["idArticle"] as? String ?: return null
        val file = File(context.cacheDir, "$IMAGE_PREFIX$articleId$IMAGE_SUFFIX")
        val photo = data["photo"] as? String
        if (photo != null) file.writeBytes(Base64.decode(photo, Base64.DEFAULT))
        return ArticleEntity(
            idArticle = UUID.fromString(articleId),
            name = data["name"] as String,
            dateCreated = (data["dateCreated"] as? Long) ?: 0L,
            location = (data["location"] as? String)?.let { locationString ->
                UILocation(
                    latitude = locationString.split(',')[0].toDouble(),
                    longitude = locationString.split(',')[1].toDouble()
                )
            },
            quantity = data["quantity"] as? Double,
            unit = ArticleUnit.valueOf(data["unit"] as? String ?: "PIECE"),
            photoUri = if (photo == null) null else file.toUri().toString(),
            updatedAt = (data["updatedAt"] as? Long) ?: 0L,
            groupId = UUID.fromString(data["groupId"] as? String) ?: UUID.randomUUID(),
            description = data["description"] as? String,
            websiteLink = data["websiteLink"] as? String,
            price = data["price"] as? Double,
            bought = data["bought"] as? Boolean ?: false
        )
    }

    fun toGroupsFirestoreMap(groupEntity: GroupEntity) = mapOf(
        "idGroup" to groupEntity.idGroup.toString(),
        "name" to groupEntity.name,
        "updatedAt" to groupEntity.updatedAt,
        "syncedAt" to System.currentTimeMillis(),
        "isShared" to groupEntity.isShared,
        "sharedGroupId" to groupEntity.sharedGroupId,
        "ownerUid" to groupEntity.ownerUid
    )

    fun toGroupEntity(snapshot: DocumentSnapshot) = GroupEntity(
        idGroup = (snapshot["idGroup"] as? String)?.let { UUID.fromString(it) } ?: UUID.randomUUID(),
        name = snapshot["name"] as? String ?: "",
        updatedAt = snapshot["updatedAt"] as? Long ?: 0L,
        isShared = snapshot["isShared"] as? Boolean ?: false,
        sharedGroupId = snapshot["sharedGroupId"] as? String,
        ownerUid = snapshot["ownerUid"] as? String
    )

    fun toCommentsFirestoreMap(commentEntity: CommentEntity) = mapOf(
        "idComment" to commentEntity.idComment.toString(),
        "groupId" to commentEntity.groupId.toString(),
        "articleId" to commentEntity.articleId?.toString(),
        "authorUid" to commentEntity.authorUid,
        "authorEmail" to commentEntity.authorEmail,
        "text" to commentEntity.text,
        "createdAt" to commentEntity.createdAt,
        "updatedAt" to commentEntity.updatedAt,
        "syncedAt" to System.currentTimeMillis()
    )

    fun toCommentEntity(snapshot: DocumentSnapshot): CommentEntity? {
        val data = snapshot.data ?: return null
        val commentId = data["idComment"] as? String ?: return null
        return CommentEntity(
            idComment = UUID.fromString(commentId),
            groupId = UUID.fromString(data["groupId"] as? String ?: return null),
            articleId = (data["articleId"] as? String)?.let { UUID.fromString(it) },
            authorUid = data["authorUid"] as? String ?: return null,
            authorEmail = data["authorEmail"] as? String ?: "",
            text = data["text"] as? String ?: "",
            createdAt = (data["createdAt"] as? Long) ?: 0L,
            updatedAt = (data["updatedAt"] as? Long) ?: 0L
        )
    }

    fun toInvitationFirestoreMap(invitation: UIInvitation) = mapOf(
        "invitationId" to invitation.invitationId,
        "fromUid" to invitation.fromUid,
        "fromEmail" to invitation.fromEmail,
        "toEmail" to invitation.toEmail,
        "toUid" to invitation.toUid,
        "groupId" to invitation.groupId,
        "groupName" to invitation.groupName,
        "status" to invitation.status.name,
        "createdAt" to invitation.createdAt
    )

    fun toInvitation(snapshot: DocumentSnapshot): UIInvitation? {
        val data = snapshot.data ?: return null
        return UIInvitation(
            invitationId = data["invitationId"] as? String ?: return null,
            fromUid = data["fromUid"] as? String ?: return null,
            fromEmail = data["fromEmail"] as? String ?: return null,
            toEmail = data["toEmail"] as? String ?: return null,
            toUid = data["toUid"] as? String ?: return null,
            groupId = data["groupId"] as? String ?: return null,
            groupName = data["groupName"] as? String ?: return null,
            status = InvitationStatus.valueOf(data["status"] as? String ?: return null),
            createdAt = data["createdAt"] as? Long ?: 0L
        )
    }

}
