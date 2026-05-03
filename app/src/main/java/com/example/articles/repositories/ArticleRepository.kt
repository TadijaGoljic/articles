package com.example.articles.repositories

import com.example.articles.db.dao.ArticleDao
import com.example.articles.models.UIArticle
import com.example.articles.models.toArticleEntity
import com.example.articles.models.toUIArticle
import com.example.articles.models.toUIGroup
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val articleDao: ArticleDao,
) {

    fun getAllArticles() = articleDao.getAll()

    suspend fun insert(article: UIArticle) =
        articleDao.insert(article.toArticleEntity())

    suspend fun insertAll(articles: List<UIArticle>) =
        articleDao.insertAll(articles.map { it.toArticleEntity() })

    suspend fun delete(idArticle: UUID) = articleDao.delete(idArticle)

    suspend fun deleteAll() = articleDao.deleteAll()

    suspend fun markDeleted(articleId: UUID, timestamp: Long) =
        articleDao.markDeleted(articleId, timestamp)

    fun getAllUngroupedArticles() = articleDao.getAllUngroupedArticles().map { list ->
        list.map { article -> article.toUIArticle() }
    }

}
