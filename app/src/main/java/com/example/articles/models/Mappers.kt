package com.example.articles.models

import com.example.articles.db.entities.ArticleEntity
import com.example.articles.db.entities.GroupEntity
import com.example.articles.db.entities.GroupWithArticles
import java.util.Date

fun ArticleEntity.toUIArticle() = UIArticle(
    id = idArticle,
    name = name,
    dateCreated = Date(dateCreated),
    location = location,
    quantity = quantity,
    unit = unit,
    photoUri = photoUri,
    deleted = deleted
)

fun UIArticle.toArticleEntity() = ArticleEntity(
    idArticle = id,
    name = name,
    dateCreated = dateCreated.time,
    location = location,
    quantity = quantity,
    unit = unit,
    photoUri = photoUri,
    deleted = deleted,
    updatedAt = Date().time
)

fun GroupEntity.toUIGroup() = UIGroup(
    id = idGroup,
    name = name
)

fun GroupWithArticles.toUIGroup() = UIGroup(
    id = group.idGroup,
    name = group.name,
    articles = articles.map { it.toUIArticle() }
)

fun UIGroup.toGroupEntity() = GroupEntity(
    idGroup = id,
    name = name
)
