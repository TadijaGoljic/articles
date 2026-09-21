package com.example.articles.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.articles.db.entities.ArticleEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles")
    fun getAllArticles(): Flow<List<ArticleEntity>>

    @Upsert suspend fun upsertArticle(article: ArticleEntity)

    @Upsert suspend fun upsertAllArticles(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles WHERE idArticle = :idArticle")
    suspend fun deleteArticleById(idArticle: UUID)

    @Query("DELETE FROM articles WHERE idArticle IN (:ids)")
    suspend fun deleteArticlesByIds(ids: List<UUID>)

    @Query("DELETE FROM articles")
    suspend fun deleteAllArticles()

    @Query("DELETE FROM articles WHERE groupId = :groupId")
    suspend fun deleteArticlesByGroupId(groupId: UUID)

    @Query("UPDATE articles SET groupId = :newGroupId WHERE groupId = :oldGroupId")
    suspend fun reassignGroupId(oldGroupId: UUID, newGroupId: UUID)

    @Query("SELECT * FROM articles WHERE groupId = :groupId ORDER BY position IS NULL, position ASC")
    fun getArticlesByGroupId(groupId: UUID): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE groupId = :groupId")
    suspend fun getArticlesByGroupIdSuspend(groupId: UUID): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE groupId = :groupId AND bought = 1 ORDER BY position IS NULL, position ASC")
    fun getBoughtArticlesByGroupId(groupId: UUID): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE groupId = :groupId AND bought = 0 ORDER BY position IS NULL, position ASC")
    fun getUnboughtArticlesByGroupId(groupId: UUID): Flow<List<ArticleEntity>>

    @Query("SELECT MAX(position) FROM articles WHERE groupId = :groupId")
    suspend fun getMaxArticlePosition(groupId: UUID): Double?

    @Query("UPDATE articles SET position = :position, updatedAt = :updatedAt WHERE idArticle = :idArticle")
    suspend fun updatePosition(idArticle: UUID, position: Double, updatedAt: Long)

    @Query("UPDATE articles SET position = :position, updatedAt = :updatedAt WHERE idArticle = :idArticle AND updatedAt < :updatedAt")
    suspend fun updatePositionIfNewer(idArticle: UUID, position: Double, updatedAt: Long)

    @Query("SELECT * FROM articles WHERE idArticle = :id")
    fun getArticleById(id: UUID): Flow<ArticleEntity?>

    @Query("SELECT * FROM articles WHERE idArticle = :id LIMIT 1")
    suspend fun getArticleByIdSuspend(id: UUID): ArticleEntity?

}
