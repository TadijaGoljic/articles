package com.example.articles.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.articles.db.entities.CommentEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CommentDao {

    @Query("SELECT * FROM comments")
    fun getAllComments(): Flow<List<CommentEntity>>

    @Upsert suspend fun upsertComment(comment: CommentEntity)

    @Upsert suspend fun upsertAllComments(comments: List<CommentEntity>)

    @Query("DELETE FROM comments WHERE idComment = :idComment")
    suspend fun deleteCommentById(idComment: UUID)

    @Query("DELETE FROM comments WHERE idComment IN (:ids)")
    suspend fun deleteCommentsByIds(ids: List<UUID>)

    @Query("DELETE FROM comments WHERE groupId = :groupId")
    suspend fun deleteCommentsByGroupId(groupId: UUID)

    @Query("DELETE FROM comments WHERE articleId = :articleId")
    suspend fun deleteCommentsByArticleId(articleId: UUID)

    @Query("SELECT * FROM comments WHERE groupId = :groupId AND articleId IS NULL")
    fun getCommentsForGroup(groupId: UUID): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE articleId = :articleId")
    fun getCommentsForArticle(articleId: UUID): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE groupId = :groupId")
    suspend fun getCommentsByGroupIdSuspend(groupId: UUID): List<CommentEntity>

    @Query("SELECT * FROM comments WHERE idComment = :idComment LIMIT 1")
    suspend fun getCommentByIdSuspend(idComment: UUID): CommentEntity?

}
