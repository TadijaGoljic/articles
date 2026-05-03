package com.example.articles.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.articles.db.entities.ArticleEntity
import com.example.articles.db.entities.GroupEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles")
    fun getAll(): Flow<List<ArticleEntity>>

    @Upsert suspend fun insert(article: ArticleEntity)

    @Upsert suspend fun insertAll(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles WHERE idArticle = :idArticle")
    suspend fun delete(idArticle: UUID)

    @Query("DELETE FROM articles")
    suspend fun deleteAll()

    @Query("UPDATE articles SET deleted = 1, updatedAt = :timestamp WHERE idArticle = :id")
    suspend fun markDeleted(id: UUID, timestamp: Long)

    @Query("SELECT * FROM articles WHERE groupId IS NULL")
    fun getAllUngroupedArticles(): Flow<List<ArticleEntity>>

}