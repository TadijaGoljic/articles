package com.example.articles.modelMapers

import com.example.articles.db.ArticleEntity
import com.example.articles.models.UIArticle
import java.util.Date

fun UIArticle.mapToDBEntity() = ArticleEntity(
    idArticle = this.id,
    name = this.name,
    dateCreated = this.dateCreated.time,
    location = this.location,
    quantity = this.quantity,
    unit = this.unit,
    photoUri = this.photoUri,
    deleted = this.deleted,
    updatedAt = Date().time
)

fun ArticleEntity.mapToUIArticle() = UIArticle(
    id = this.idArticle,
    name = this.name,
    dateCreated = Date(this.dateCreated),
    location = this.location,
    quantity = this.quantity,
    unit = this.unit,
    photoUri = this.photoUri
)