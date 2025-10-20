package com.example.articles.repositories

import com.example.articles.db.ArticleDao
import com.example.articles.models.mapToDBEntity
import com.example.articles.models.UIArticle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val articleDao: ArticleDao
) {

    fun getAllArticles() = articleDao.getAll()

    suspend fun insertAll(articles: List<UIArticle>) =
        articleDao.insertAll(articles.map { it.mapToDBEntity() })

    suspend fun deleteAll(articles: List<UIArticle>) =
        articles.forEach { article -> articleDao.delete(article.mapToDBEntity().idArticle) }

}