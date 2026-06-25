package com.example.articles.models

import com.example.articles.db.entities.ArticleEntity
import com.example.articles.db.entities.GroupEntity
import java.util.Date

fun ArticleEntity.toUIArticle() = UIArticle(
    id = idArticle,
    name = name,
    dateCreated = Date(dateCreated),
    location = location,
    quantity = quantity,
    unit = unit,
    photoUri = photoUri,
    deleted = deleted,
    groupId = groupId,
    description = description,
    updatedAt = updatedAt,
    websiteLink = websiteLink,
    price = price,
    bought = bought
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
    updatedAt = System.currentTimeMillis(),
    groupId = groupId,
    description = description,
    websiteLink = websiteLink,
    price = price,
    bought = bought
)

fun GroupEntity.toUIGroup() = UIGroup(
    id = idGroup,
    name = name,
    updatedAt = updatedAt,
    deleted = deleted,
    isShared = isShared,
    sharedGroupId = sharedGroupId
)

fun UIGroup.toGroupEntity() = GroupEntity(
    idGroup = id,
    name = name,
    updatedAt = updatedAt,
    deleted = deleted,
    isShared = isShared,
    sharedGroupId = sharedGroupId
)
