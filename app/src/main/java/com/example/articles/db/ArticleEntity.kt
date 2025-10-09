package com.example.articles.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.articles.ui.models.ArticleUnit
import com.example.articles.viewmodels.LocationData

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String?,
    val dateCreated: Long,
    val location: LocationData?,
    val quantity: Double?,
    val unit: ArticleUnit,
    val photoUri: String?,
)