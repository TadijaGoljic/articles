package com.example.articles.repositories

import com.example.articles.db.ArticleDao
import com.example.articles.models.ModelMapper
import com.example.articles.models.UIArticle
import java.util.UUID
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

    suspend fun delete(idArticle: UUID) = articleDao.delete(idArticle)

    suspend fun deleteAll() = articleDao.deleteAll()

    suspend fun markDeleted(articleId: UUID, timestamp: Long) =
        articleDao.markDeleted(articleId, timestamp)

}