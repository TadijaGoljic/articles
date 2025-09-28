package com.example.articles.modelMapers

import com.example.articles.db.ArticleEntity
import com.example.articles.ui.models.UIArticle

fun UIArticle.mapToDBEntity() = ArticleEntity(
    id = this.id,
    title = this.title,
    dateCreated = this.dateCreated
)

fun ArticleEntity.mapToUIArticle() = UIArticle(
    id = this.id,
    title = this.title,
    dateCreated = this.dateCreated
)