package com.example.articles.models.mappers

import com.example.articles.db.entities.ArticleEntity
import com.example.articles.db.entities.CommentEntity
import com.example.articles.db.entities.GroupEntity
import com.example.articles.models.UIArticle
import com.example.articles.models.UIComment
import com.example.articles.models.UIGroup
import java.util.Date

fun ArticleEntity.toUIArticle() = UIArticle(
    id = idArticle,
    name = name,
    dateCreated = Date(dateCreated),
    location = location,
    quantity = quantity,
    unit = unit,
    photoUri = photoUri,
    groupId = groupId,
    description = description,
    updatedAt = updatedAt,
    websiteLink = websiteLink,
    price = price,
    bought = bought,
    dateTo = dateTo?.let { Date(it) },
    position = position,
    assignedToUid = assignedToUid
)

fun UIArticle.toArticleEntity() = ArticleEntity(
    idArticle = id,
    name = name,
    dateCreated = dateCreated.time,
    location = location,
    quantity = quantity,
    unit = unit,
    photoUri = photoUri,
    updatedAt = System.currentTimeMillis(),
    groupId = groupId,
    description = description,
    websiteLink = websiteLink,
    price = price,
    bought = bought,
    dateTo = dateTo?.time,
    position = position,
    assignedToUid = assignedToUid
)

fun GroupEntity.toUIGroup() = UIGroup(
    id = idGroup,
    name = name,
    updatedAt = updatedAt,
    isShared = isShared,
    sharedGroupId = sharedGroupId,
    ownerUid = ownerUid,
    isArchived = isArchived,
    position = position
)

fun UIGroup.toGroupEntity() = GroupEntity(
    idGroup = id,
    name = name,
    updatedAt = updatedAt,
    isShared = isShared,
    sharedGroupId = sharedGroupId,
    ownerUid = ownerUid,
    isArchived = isArchived,
    position = position
)

fun CommentEntity.toUIComment() = UIComment(
    id = idComment,
    groupId = groupId,
    articleId = articleId,
    authorUid = authorUid,
    authorEmail = authorEmail,
    text = text,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun UIComment.toCommentEntity() = CommentEntity(
    idComment = id,
    groupId = groupId,
    articleId = articleId,
    authorUid = authorUid,
    authorEmail = authorEmail,
    text = text,
    createdAt = createdAt,
    updatedAt = System.currentTimeMillis()
)
