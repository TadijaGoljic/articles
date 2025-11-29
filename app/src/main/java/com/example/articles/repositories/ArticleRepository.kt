package com.example.articles.repositories

import com.example.articles.db.ArticleDao
import com.example.articles.models.ModelMapper
import com.example.articles.models.UIArticle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val articleDao: ArticleDao,
    private val modelMapper: ModelMapper,
) {

    fun getAllArticles() = articleDao.getAll()

    suspend fun insertAll(articles: List<UIArticle>) =
        articleDao.insertAll(articles.map { modelMapper.mapToDBEntity(it) })

    suspend fun deleteAll(articles: List<UIArticle>) =
        articles.forEach { article -> articleDao.delete(modelMapper.mapToDBEntity(article).idArticle) }

    suspend fun deleteAll() = articleDao.deleteAll()

}