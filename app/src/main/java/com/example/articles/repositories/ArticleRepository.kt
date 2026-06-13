package com.example.articles.repositories

import com.example.articles.db.dao.ArticleDao
import com.example.articles.models.UIArticle
import com.example.articles.models.toArticleEntity
import com.example.articles.models.toUIArticle
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val articleDao: ArticleDao,
) {

    fun getAllArticlesNoMapping() = articleDao.getAllArticles()

    suspend fun upsertArticle(article: UIArticle) =
        articleDao.upsertArticle(article.toArticleEntity())

    suspend fun upsertAllArticles(articles: List<UIArticle>) =
        articleDao.upsertAllArticles(articles.map { it.toArticleEntity() })

    suspend fun deleteArticleById(idArticle: UUID) = articleDao.deleteArticleById(idArticle)

    suspend fun deleteAllArticles() = articleDao.deleteAllArticles()

    fun getArticlesByGroupId(groupId: UUID) = articleDao.getArticlesByGroupId(groupId).map { list ->
        list.map { article -> article.toUIArticle() }
    }

    fun getAllArticles() = articleDao.getAllArticles().map { list ->
        list.map { article -> article.toUIArticle() }
    }

    fun getArticleById(id: UUID) = articleDao.getArticleById(id).map { it?.toUIArticle() }

}
