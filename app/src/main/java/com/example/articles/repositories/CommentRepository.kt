package com.example.articles.repositories

import com.example.articles.db.dao.CommentDao
import com.example.articles.db.dao.GroupDao
import com.example.articles.db.entities.CommentEntity
import com.example.articles.models.mappers.FirestoreModelMapper
import com.example.articles.models.UIComment
import com.example.articles.models.mappers.toCommentEntity
import com.example.articles.models.mappers.toUIComment
import com.example.articles.utils.COMMENTS_COLLECTION
import com.example.articles.utils.SHARED_GROUPS_COLLECTION
import com.example.articles.utils.USERS_COLLECTION
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepository @Inject constructor(
    private val commentDao: CommentDao,
    private val groupDao: GroupDao,
    private val firestoreModelMapper: FirestoreModelMapper
) {

    private val firestore = Firebase.firestore

    private suspend fun commentsCollection(groupId: UUID): CollectionReference? {
        val uid = Firebase.auth.currentUser?.uid ?: return null
        val group = groupDao.getGroupByIdSuspend(groupId)
        return if (group?.isShared == true && group.sharedGroupId != null) {
            firestore
                .collection(SHARED_GROUPS_COLLECTION)
                .document(group.sharedGroupId)
                .collection(COMMENTS_COLLECTION)
        } else {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(COMMENTS_COLLECTION)
        }
    }

    fun getAllCommentsNoMapping() = commentDao.getAllComments()

    suspend fun upsertComment(comment: UIComment) {
        val commentEntity = comment.toCommentEntity()
        commentDao.upsertComment(commentEntity)
        commentsCollection(comment.groupId)
            ?.document(comment.id.toString())
            ?.set(firestoreModelMapper.toCommentsFirestoreMap(commentEntity))
            ?.await()
    }

    suspend fun deleteCommentById(id: UUID) {
        val comment = commentDao.getCommentByIdSuspend(id) ?: return
        commentDao.deleteCommentById(id)
        commentsCollection(comment.groupId)
            ?.document(id.toString())
            ?.delete()
            ?.await()
    }

    suspend fun upsertAllCommentsLocal(comments: List<CommentEntity>) =
        commentDao.upsertAllComments(comments)

    suspend fun getCommentsByGroupIdSuspend(groupId: UUID): List<CommentEntity> =
        commentDao.getCommentsByGroupIdSuspend(groupId)

    suspend fun hardDeleteLocalCommentsByGroupId(groupId: UUID) =
        commentDao.deleteCommentsByGroupId(groupId)

    suspend fun hardDeleteLocalCommentsByArticleId(articleId: UUID) =
        commentDao.deleteCommentsByArticleId(articleId)

    suspend fun hardDeleteLocalCommentsByIds(ids: List<UUID>) =
        commentDao.deleteCommentsByIds(ids)

    suspend fun deleteCommentsByGroupId(groupId: UUID) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val comments = commentDao.getCommentsByGroupIdSuspend(groupId)
        commentDao.deleteCommentsByGroupId(groupId)
        if (comments.isNotEmpty()) {
            val collection = firestore
                .collection(USERS_COLLECTION)
                .document(uid)
                .collection(COMMENTS_COLLECTION)
            val batch = firestore.batch()
            comments.forEach { comment ->
                batch.delete(collection.document(comment.idComment.toString()))
            }
            batch.commit().await()
        }
    }

    suspend fun deleteCommentsByArticleId(groupId: UUID, articleId: UUID) {
        val comments = commentDao.getCommentsByGroupIdSuspend(groupId)
            .filter { it.articleId == articleId }
        commentDao.deleteCommentsByArticleId(articleId)
        if (comments.isNotEmpty()) {
            val collection = commentsCollection(groupId) ?: return
            val batch = firestore.batch()
            comments.forEach { comment ->
                batch.delete(collection.document(comment.idComment.toString()))
            }
            batch.commit().await()
        }
    }

    fun getCommentsForGroup(groupId: UUID) = commentDao.getCommentsForGroup(groupId).map { list ->
        list.map { comment -> comment.toUIComment() }
    }

    fun getCommentsForArticle(articleId: UUID) = commentDao.getCommentsForArticle(articleId).map { list ->
        list.map { comment -> comment.toUIComment() }
    }

}
