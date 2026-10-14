package com.example.articles.repositories

import com.example.articles.db.dao.CommentDao
import com.example.articles.db.dao.GroupDao
import com.example.articles.db.entities.CommentEntity
import com.example.articles.models.mappers.FirestoreModelMapper
import com.example.articles.models.UIComment
import com.example.articles.models.mappers.toCommentEntity
import com.example.articles.models.mappers.toUIComment
import com.example.articles.sync.RemoteContentWriter
import com.example.articles.utils.COMMENTS_COLLECTION
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepository @Inject constructor(
    private val commentDao: CommentDao,
    private val groupDao: GroupDao,
    private val firestoreModelMapper: FirestoreModelMapper,
    private val remoteWriter: RemoteContentWriter
) {

    private val firestore = Firebase.firestore

    private suspend fun ownerContext(groupId: UUID): Pair<String?, String?>? {
        val uid = Firebase.auth.currentUser?.uid ?: return null
        val group = groupDao.getGroupByIdSuspend(groupId)
        return if (group?.isShared == true && group.sharedGroupId != null) {
            null to group.sharedGroupId
        } else {
            uid to null
        }
    }

    fun getAllCommentsNoMapping() = commentDao.getAllComments()

    suspend fun upsertComment(comment: UIComment) {
        val commentEntity = comment.toCommentEntity()
        commentDao.upsertComment(commentEntity)
        val (ownerUid, sharedGroupId) = ownerContext(comment.groupId) ?: return
        remoteWriter.set(
            firestore.collection(COMMENTS_COLLECTION).document(comment.id.toString()),
            firestoreModelMapper.toCommentsFirestoreMap(commentEntity, ownerUid, sharedGroupId)
        )
    }

    suspend fun deleteCommentById(id: UUID) {
        commentDao.getCommentByIdSuspend(id) ?: return
        commentDao.deleteCommentById(id)
        remoteWriter.delete(firestore.collection(COMMENTS_COLLECTION).document(id.toString()))
    }

    suspend fun upsertAllCommentsLocal(comments: List<CommentEntity>) =
        commentDao.upsertAllComments(comments)

    suspend fun getCommentsByGroupIdSuspend(groupId: UUID): List<CommentEntity> =
        commentDao.getCommentsByGroupIdSuspend(groupId)

    suspend fun hardDeleteLocalCommentsByGroupId(groupId: UUID) =
        commentDao.deleteCommentsByGroupId(groupId)

    suspend fun reassignCommentsGroupIdLocal(oldGroupId: UUID, newGroupId: UUID) =
        commentDao.reassignGroupId(oldGroupId, newGroupId)

    suspend fun hardDeleteLocalCommentsByArticleId(articleId: UUID) =
        commentDao.deleteCommentsByArticleId(articleId)

    suspend fun hardDeleteLocalCommentsByIds(ids: List<UUID>) =
        commentDao.deleteCommentsByIds(ids)

    suspend fun deleteCommentsByGroupId(groupId: UUID) {
        Firebase.auth.currentUser ?: return
        val comments = commentDao.getCommentsByGroupIdSuspend(groupId)
        commentDao.deleteCommentsByGroupId(groupId)
        deleteRemoteComments(comments)
    }

    suspend fun deleteCommentsByArticleId(groupId: UUID, articleId: UUID) {
        val comments = commentDao.getCommentsByGroupIdSuspend(groupId)
            .filter { it.articleId == articleId }
        commentDao.deleteCommentsByArticleId(articleId)
        deleteRemoteComments(comments)
    }

    private suspend fun deleteRemoteComments(comments: List<CommentEntity>) {
        val collection = firestore.collection(COMMENTS_COLLECTION)
        remoteWriter.deleteAll(comments.map { collection.document(it.idComment.toString()) })
    }

    fun getCommentsForGroup(groupId: UUID) = commentDao.getCommentsForGroup(groupId).map { list ->
        list.map { comment -> comment.toUIComment() }
    }

    fun getCommentsForArticle(articleId: UUID) = commentDao.getCommentsForArticle(articleId).map { list ->
        list.map { comment -> comment.toUIComment() }
    }

    fun getAllCommentsForGroup(groupId: UUID) = commentDao.getAllCommentsForGroupId(groupId).map { list ->
        list.map { comment -> comment.toUIComment() }
    }

}
