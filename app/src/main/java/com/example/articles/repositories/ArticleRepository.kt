package com.example.articles.repositories

import com.example.articles.db.dao.ArticleDao
import com.example.articles.db.dao.GroupDao
import com.example.articles.db.entities.ArticleEntity
import com.example.articles.models.FirestoreModelMapper
import com.example.articles.models.UIArticle
import com.example.articles.models.toArticleEntity
import com.example.articles.models.toUIArticle
import com.example.articles.utils.ARTICLES_COLLECTION
import com.example.articles.utils.SHARED_GROUPS_COLLECTION
import com.example.articles.utils.USERS_COLLECTION
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.CollectionReference
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

    private suspend fun articlesCollection(groupId: UUID): CollectionReference? {
        val uid = Firebase.auth.currentUser?.uid ?: return null
        val group = groupDao.getGroupByIdSuspend(groupId)
        return if (group?.isShared == true && group.sharedGroupId != null) {
            firestore
                .collection(SHARED_GROUPS_COLLECTION)
                .document(group.sharedGroupId)
                .collection(ARTICLES_COLLECTION)
        } else {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(ARTICLES_COLLECTION)
        }
    }

    fun getAllArticlesNoMapping() = articleDao.getAllArticlesRaw()

    suspend fun upsertArticle(article: UIArticle) {
        val articleEntity = article.toArticleEntity()
        articleDao.upsertArticle(articleEntity)
        articlesCollection(article.groupId)
            ?.document(article.id.toString())
            ?.set(firestoreModelMapper.toArticlesFirestoreMap(articleEntity))
            ?.await()
    }

    suspend fun deleteArticleById(idArticle: UUID) {
        val article = articleDao.getArticleByIdSuspend(idArticle) ?: return
        val deleted = article.copy(deleted = true, updatedAt = System.currentTimeMillis())
        articleDao.upsertArticle(deleted)
        articlesCollection(article.groupId)
            ?.document(idArticle.toString())
            ?.set(firestoreModelMapper.toArticlesFirestoreMap(deleted))
            ?.await()
    }

    suspend fun upsertAllArticlesLocal(articles: List<ArticleEntity>) =
        articleDao.upsertAllArticles(articles)

    suspend fun getArticleByIdSuspend(id: UUID): ArticleEntity? =
        articleDao.getArticleByIdSuspend(id)

    suspend fun hardDeleteLocalArticlesByGroupId(groupId: UUID) =
        articleDao.deleteArticlesByGroupId(groupId)

    suspend fun softDeleteArticlesByGroupId(groupId: UUID) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val now = System.currentTimeMillis()
        val articles = articleDao.getArticlesByGroupIdSuspend(groupId)
        val deleted = articles.map {
            it.copy(
                deleted = true,
                updatedAt = now
            )
        }
        articleDao.upsertAllArticles(deleted)
        if (deleted.isNotEmpty()) {
            val collection = firestore
                .collection(USERS_COLLECTION)
                .document(uid)
                .collection(ARTICLES_COLLECTION)
            val batch = firestore.batch()
            deleted.forEach { article ->
                batch.set(
                    collection.document(article.idArticle.toString()),
                    firestoreModelMapper.toArticlesFirestoreMap(article)
                )
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
