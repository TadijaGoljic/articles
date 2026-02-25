package com.example.articles.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles")
    fun getAll(): Flow<List<ArticleEntity>>

    @Upsert
    suspend fun insertAll(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles WHERE idArticle = :idArticle")
    suspend fun delete(idArticle: UUID)

    @Query("DELETE FROM articles")
    suspend fun deleteAll()

    @Query("UPDATE articles SET deleted = 1, updatedAt = :timestamp WHERE idArticle = :id")
    suspend fun markDeleted(id: String, timestamp: Long)

}