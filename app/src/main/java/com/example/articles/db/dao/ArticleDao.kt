package com.example.articles.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.articles.db.entities.ArticleEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles WHERE deleted = 0")
    fun getAllArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles")
    fun getAllArticlesRaw(): Flow<List<ArticleEntity>>

    @Upsert suspend fun upsertArticle(article: ArticleEntity)

    @Upsert suspend fun upsertAllArticles(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles WHERE idArticle = :idArticle")
    suspend fun deleteArticleById(idArticle: UUID)

    @Query("DELETE FROM articles")
    suspend fun deleteAllArticles()

    @Query("DELETE FROM articles WHERE groupId = :groupId")
    suspend fun deleteArticlesByGroupId(groupId: UUID)

    @Query("SELECT * FROM articles WHERE groupId = :groupId AND deleted = 0")
    fun getArticlesByGroupId(groupId: UUID): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE groupId = :groupId")
    suspend fun getArticlesByGroupIdSuspend(groupId: UUID): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE groupId = :groupId AND bought = 1 AND deleted = 0")
    fun getBoughtArticlesByGroupId(groupId: UUID): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE groupId = :groupId AND bought = 0 AND deleted = 0")
    fun getUnboughtArticlesByGroupId(groupId: UUID): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE idArticle = :id AND deleted = 0")
    fun getArticleById(id: UUID): Flow<ArticleEntity?>

    @Query("SELECT * FROM articles WHERE idArticle = :id LIMIT 1")
    suspend fun getArticleByIdSuspend(id: UUID): ArticleEntity?

}
