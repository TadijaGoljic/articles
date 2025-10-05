package com.example.articles.modelMapers

import com.example.articles.db.ArticleEntity
import com.example.articles.ui.models.ArticleUnit
import com.example.articles.ui.models.UIArticle
import com.example.articles.viewmodels.LocationData
import kotlin.Int

fun UIArticle.mapToDBEntity() = ArticleEntity(
    id = this.id,
    name = this.name,
    dateCreated = this.dateCreated,
    location = this.location,
    quantity = this.quantity,
    unit = this.unit,
    photoUri = this.photoUri
)

fun ArticleEntity.mapToUIArticle() = UIArticle(
    id = this.id,
    name = this.name,
    dateCreated = this.dateCreated,
    location = this.location,
    quantity = this.quantity,
    unit = this.unit,
    photoUri = this.photoUri
)