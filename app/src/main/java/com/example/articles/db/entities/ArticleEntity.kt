package com.example.articles.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.articles.models.ArticleUnit
import com.example.articles.models.Location
import java.util.UUID

@Entity(
    tableName = "articles",
    foreignKeys = [ForeignKey(
        entity = GroupEntity::class,
        parentColumns = ["idGroup"],
        childColumns = ["groupId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("groupId")]
)
data class ArticleEntity(
    @PrimaryKey val idArticle: UUID = UUID.randomUUID(),
    val name: String?,
    val dateCreated: Long,
    val location: Location?,
    val quantity: Double?,
    val unit: ArticleUnit,
    val photoUri: String?,
    val deleted: Boolean,
    val description: String?,
    val updatedAt: Long,
    val groupId: UUID,
    val websiteLink: String?,
    val pricePerUnit: Double?,
    val bought: Boolean
)