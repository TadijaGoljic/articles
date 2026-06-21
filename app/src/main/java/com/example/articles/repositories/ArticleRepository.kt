package com.example.articles.repositories

import com.example.articles.db.dao.ArticleDao
import com.example.articles.models.FirestoreModelMapper
import com.example.articles.models.UIArticle
import com.example.articles.models.toArticleEntity
import com.example.articles.models.toUIArticle
import com.example.articles.utils.ARTICLES_COLLECTION
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val articleDao: ArticleDao,
    private val firestoreModelMapper: FirestoreModelMapper
) {

    private val firestore = Firebase.firestore

    fun getAllArticlesNoMapping() = articleDao.getAllArticles()

    suspend fun upsertArticle(article: UIArticle) {
        val articleEntity = article.toArticleEntity()
        articleDao.upsertArticle(articleEntity)
        firestore.collection(ARTICLES_COLLECTION)
            .document(article.id.toString())
            .update(firestoreModelMapper.toArticlesFirestoreMap(articleEntity))
            .await()
    }

    suspend fun upsertAllArticles(articles: List<UIArticle>) = articles.forEach { article ->
        upsertArticle(article)
    }

    suspend fun deleteArticleById(idArticle: UUID) {
        articleDao.deleteArticleById(idArticle)
        firestore.collection(ARTICLES_COLLECTION)
            .document(idArticle.toString())
            .delete()
            .await()
    }

    fun getArticlesByGroupId(groupId: UUID) = articleDao.getArticlesByGroupId(groupId).map { list ->
        list.map { article -> article.toUIArticle() }
    }

    fun getAllArticles() = articleDao.getAllArticles().map { list ->
        list.map { article -> article.toUIArticle() }
    }

    fun getArticleById(id: UUID) = articleDao.getArticleById(id).map { it?.toUIArticle() }

    fun getBoughtArticlesByGroupId(groupId: UUID) =
        articleDao.getBoughtArticlesByGroupId(groupId).map { list ->
            list.map { it.toUIArticle() }
        }

    fun getUnboughtArticlesByGroupId(groupId: UUID) =
        articleDao.getUnboughtArticlesByGroupId(groupId).map { list ->
            list.map { it.toUIArticle() }
        }

}
