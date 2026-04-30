package com.example.articles.db.entities

import androidx.room.Embedded
import androidx.room.Relation

data class GroupWithArticles(
    @Embedded val group: GroupEntity,
    @Relation(
        parentColumn = "idGroup",
        entityColumn = "groupId"
    )
    val articles: List<ArticleEntity>
)
