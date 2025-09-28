package com.example.articles.repositories

import com.example.articles.db.ArticleDao
import com.example.articles.modelMapers.mapToDBEntity
import com.example.articles.ui.models.UIArticle
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
        articleDao.delete(articles.map { it.mapToDBEntity() })

    suspend fun insert(article: UIArticle) =
        articleDao.insertAll(listOf(article.mapToDBEntity()))

    suspend fun delete(article: UIArticle) =
        articleDao.delete(listOf(article.mapToDBEntity()))

}