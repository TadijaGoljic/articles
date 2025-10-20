package com.example.articles.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.articles.models.ArticleUnit
import com.example.articles.viewmodels.LocationData
import java.util.UUID

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val idArticle: UUID = UUID.randomUUID(),
    val name: String?,
    val dateCreated: Long,
    val location: LocationData?,
    val quantity: Double?,
    val unit: ArticleUnit,
    val photoUri: String?,
    val deleted: Boolean,
    val updatedAt: Long
) {
    constructor() : this(
        idArticle = UUID.randomUUID(),
        name = null,
        dateCreated = 0L,
        location = null,
        quantity = null,
        unit = ArticleUnit.default,
        photoUri = null,
        deleted = false,
        updatedAt = 0L
    )
}