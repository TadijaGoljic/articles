package com.example.articles.modelMapers

import com.example.articles.db.ArticleEntity
import com.example.articles.ui.models.UIArticle

fun UIArticle.mapToDBEntity() = ArticleEntity(
    id = this.id,
    name = this.name,
    dateCreated = this.dateCreated,
    location = this.location
)

fun ArticleEntity.mapToUIArticle() = UIArticle(
    id = this.id,
    name = this.name,
    dateCreated = this.dateCreated,
    location = this.location
)