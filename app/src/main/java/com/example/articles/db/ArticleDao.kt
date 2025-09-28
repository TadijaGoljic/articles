package com.example.articles.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles")
    fun getAll(): Flow<List<ArticleEntity>>

    @Insert
    suspend fun insertAll(articles: List<ArticleEntity>)

    @Delete
    suspend fun delete(article: List<ArticleEntity>)

}