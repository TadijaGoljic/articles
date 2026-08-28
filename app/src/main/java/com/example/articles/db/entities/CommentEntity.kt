package com.example.articles.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["idGroup"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["idArticle"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId"), Index("articleId")]
)
data class CommentEntity(
    @PrimaryKey val idComment: UUID = UUID.randomUUID(),
    val groupId: UUID,
    val articleId: UUID?,
    val authorUid: String,
    val authorEmail: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long
)
