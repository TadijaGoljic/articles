package com.example.articles.repositories

import com.example.articles.db.dao.ArticleDao
import com.example.articles.db.dao.GroupDao
import com.example.articles.db.entities.ArticleEntity
import com.example.articles.models.mappers.FirestoreModelMapper
import com.example.articles.models.UIArticle
import com.example.articles.models.mappers.toArticleEntity
import com.example.articles.models.mappers.toUIArticle
import com.example.articles.utils.ARTICLES_COLLECTION
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val articleDao: ArticleDao,
    private val groupDao: GroupDao,
    private val firestoreModelMapper: FirestoreModelMapper
) {

    private val firestore = Firebase.firestore

    /** Returns (ownerUid, sharedGroupId) firestore scoping for an article's group; null if signed out. */
    private suspend fun ownerContext(groupId: UUID): Pair<String?, String?>? {
        val uid = Firebase.auth.currentUser?.uid ?: return null
        val group = groupDao.getGroupByIdSuspend(groupId)
        return if (group?.isShared == true && group.sharedGroupId != null) {
            null to group.sharedGroupId
        } else {
            uid to null
        }
    }

    fun getAllArticlesNoMapping() = articleDao.getAllArticles()

    suspend fun upsertArticle(article: UIArticle) {
        val articleEntity = article.toArticleEntity()
        articleDao.upsertArticle(articleEntity)
        val (ownerUid, sharedGroupId) = ownerContext(article.groupId) ?: return
        firestore.collection(ARTICLES_COLLECTION)
            .document(article.id.toString())
            .set(firestoreModelMapper.toArticlesFirestoreMap(articleEntity, ownerUid, sharedGroupId))
            .await()
    }

    suspend fun deleteArticleById(articleId: UUID) {
        articleDao.getArticleByIdSuspend(articleId) ?: return
        articleDao.deleteArticleById(articleId)
        firestore.collection(ARTICLES_COLLECTION)
            .document(articleId.toString())
            .delete()
            .await()
    }

    suspend fun upsertAllArticlesLocal(articles: List<ArticleEntity>) =
        articleDao.upsertAllArticles(articles)

    suspend fun getArticleByIdSuspend(id: UUID): ArticleEntity? =
        articleDao.getArticleByIdSuspend(id)

    suspend fun getArticlesByGroupIdSuspend(groupId: UUID): List<ArticleEntity> =
        articleDao.getArticlesByGroupIdSuspend(groupId)

    suspend fun hardDeleteLocalArticlesByGroupId(groupId: UUID) =
        articleDao.deleteArticlesByGroupId(groupId)

    suspend fun hardDeleteLocalArticlesByIds(ids: List<UUID>) =
        articleDao.deleteArticlesByIds(ids)

    suspend fun deleteArticlesByGroupId(groupId: UUID) {
        Firebase.auth.currentUser ?: return
        val articles = articleDao.getArticlesByGroupIdSuspend(groupId)
        articleDao.deleteArticlesByGroupId(groupId)
        if (articles.isNotEmpty()) {
            val collection = firestore.collection(ARTICLES_COLLECTION)
            val batch = firestore.batch()
            articles.forEach { article ->
                batch.delete(collection.document(article.idArticle.toString()))
            }
            batch.commit().await()
        }
    }

    fun getArticlesByGroupId(groupId: UUID) = articleDao.getArticlesByGroupId(groupId).map { list ->
        list.map { article -> article.toUIArticle() }
    }

    fun getAllArticles() = articleDao.getAllArticles().map { list ->
        list.map { article -> article.toUIArticle() }
    }

    fun getArticleById(id: UUID) = articleDao.getArticleById(id).map {
        it?.toUIArticle()
    }

    fun getBoughtArticlesByGroupId(groupId: UUID) = articleDao.getBoughtArticlesByGroupId(groupId).map { list ->
        list.map { it.toUIArticle() }
    }

    fun getUnboughtArticlesByGroupId(groupId: UUID) = articleDao.getUnboughtArticlesByGroupId(groupId).map { list ->
        list.map { it.toUIArticle() }
    }

}
