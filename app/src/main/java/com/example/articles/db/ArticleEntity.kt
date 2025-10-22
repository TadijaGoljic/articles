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
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ArticleEntity
        if (dateCreated != other.dateCreated) return false
        if (quantity != other.quantity) return false
        if (deleted != other.deleted) return false
        if (updatedAt != other.updatedAt) return false
        if (idArticle != other.idArticle) return false
        if (name != other.name) return false
        if (location != other.location) return false
        if (unit != other.unit) return false
        if (photoUri != other.photoUri) return false
        return true
    }
    override fun hashCode(): Int {
        var result = dateCreated.hashCode()
        result = 31 * result + (quantity?.hashCode() ?: 0)
        result = 31 * result + deleted.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + idArticle.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (location?.hashCode() ?: 0)
        result = 31 * result + unit.hashCode()
        result = 31 * result + (photoUri?.hashCode() ?: 0)
        return result
    }
}