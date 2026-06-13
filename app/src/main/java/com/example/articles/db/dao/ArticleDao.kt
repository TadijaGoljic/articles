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

    @Query("DELETE FROM articles")
    suspend fun deleteAllArticles()

    @Query("SELECT * FROM articles WHERE groupId = :groupId")
    fun getArticlesByGroupId(groupId: UUID): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE idArticle = :id")
    fun getArticleById(id: UUID): Flow<ArticleEntity?>

}