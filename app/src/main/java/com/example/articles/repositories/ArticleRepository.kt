package com.example.articles.repositories

import com.example.articles.db.dao.ArticleDao
import com.example.articles.db.dao.GroupDao
import com.example.articles.db.entities.ArticleEntity
import com.example.articles.models.mappers.FirestoreModelMapper
import com.example.articles.models.UIArticle
import com.example.articles.models.mappers.toArticleEntity
import com.example.articles.models.mappers.toUIArticle
import com.example.articles.reminders.DeadlineReminderScheduler
import com.example.articles.sync.RemoteContentWriter
import com.example.articles.utils.ARTICLES_COLLECTION
import com.example.articles.utils.ARTICLE_ORDER_SUBCOLLECTION
import com.example.articles.utils.USERS_COLLECTION
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val articleDao: ArticleDao,
    private val groupDao: GroupDao,
    private val firestoreModelMapper: FirestoreModelMapper,
    private val deadlineReminderScheduler: DeadlineReminderScheduler,
    private val remoteWriter: RemoteContentWriter
) {

    private val firestore = Firebase.firestore

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
        deadlineReminderScheduler.scheduleOrCancel(articleEntity)
        val (ownerUid, sharedGroupId) = ownerContext(article.groupId) ?: return
        remoteWriter.set(
            firestore.collection(ARTICLES_COLLECTION).document(article.id.toString()),
            firestoreModelMapper.toArticlesFirestoreMap(articleEntity, ownerUid, sharedGroupId)
        )
        if (article.position != null) {
            val currentUid = Firebase.auth.currentUser?.uid ?: return
            pushArticlePosition(currentUid, article.id, article.groupId, article.position)
        }
    }

    suspend fun updateArticleAssignee(article: UIArticle, assignedToUid: String?) {
        upsertArticle(article.copy(assignedToUid = assignedToUid))
    }

    suspend fun getNextArticlePosition(groupId: UUID): Double =
        articleDao.getMaxArticlePosition(groupId)?.plus(GAP) ?: GAP

    suspend fun applyPulledArticlePosition(articleId: UUID, position: Double, updatedAt: Long) {
        articleDao.updatePositionIfNewer(articleId, position, updatedAt)
    }

    suspend fun updateArticlePosition(articleId: UUID, groupId: UUID, position: Double) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        articleDao.updatePosition(articleId, position, System.currentTimeMillis())
        pushArticlePosition(uid, articleId, groupId, position)
    }

    suspend fun renumberArticlePositions(groupId: UUID) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val articles = articleDao.getArticlesByGroupIdSuspend(groupId)
            .sortedBy { it.position ?: Double.MAX_VALUE }
        articles.forEachIndexed { index, article ->
            val position = index * GAP
            articleDao.updatePosition(article.idArticle, position, System.currentTimeMillis())
            pushArticlePosition(uid, article.idArticle, groupId, position)
        }
    }

    private suspend fun pushArticlePosition(uid: String, articleId: UUID, groupId: UUID, position: Double) {
        remoteWriter.set(
            firestore
                .collection(USERS_COLLECTION)
                .document(uid)
                .collection(ARTICLE_ORDER_SUBCOLLECTION)
                .document(articleId.toString()),
            firestoreModelMapper.toArticleOrderFirestoreMap(position, groupId)
        )
    }

    suspend fun deleteArticleById(articleId: UUID) {
        articleDao.getArticleByIdSuspend(articleId) ?: return
        articleDao.deleteArticleById(articleId)
        deadlineReminderScheduler.cancel(articleId)
        remoteWriter.delete(firestore.collection(ARTICLES_COLLECTION).document(articleId.toString()))
    }

    suspend fun resetBoughtArticles(groupId: UUID) {
        articleDao.getArticlesByGroupIdSuspend(groupId)
            .filter { it.bought }
            .forEach { upsertArticle(it.toUIArticle().copy(bought = false)) }
    }

    suspend fun upsertAllArticlesLocal(articles: List<ArticleEntity>) {
        articleDao.upsertAllArticles(articles)
        articles.forEach { deadlineReminderScheduler.scheduleOrCancel(it) }
    }

    suspend fun getArticleByIdSuspend(id: UUID): ArticleEntity? =
        articleDao.getArticleByIdSuspend(id)

    suspend fun getArticlesByGroupIdSuspend(groupId: UUID): List<ArticleEntity> =
        articleDao.getArticlesByGroupIdSuspend(groupId)

    suspend fun hardDeleteLocalArticlesByGroupId(groupId: UUID) {
        val articles = articleDao.getArticlesByGroupIdSuspend(groupId)
        articleDao.deleteArticlesByGroupId(groupId)
        articles.forEach { deadlineReminderScheduler.cancel(it.idArticle) }
    }

    suspend fun reassignArticlesGroupIdLocal(oldGroupId: UUID, newGroupId: UUID) =
        articleDao.reassignGroupId(oldGroupId, newGroupId)

    suspend fun hardDeleteLocalArticlesByIds(ids: List<UUID>) {
        articleDao.deleteArticlesByIds(ids)
        ids.forEach { deadlineReminderScheduler.cancel(it) }
    }

    suspend fun deleteArticlesByGroupId(groupId: UUID) {
        Firebase.auth.currentUser ?: return
        val articles = articleDao.getArticlesByGroupIdSuspend(groupId)
        articleDao.deleteArticlesByGroupId(groupId)
        articles.forEach { deadlineReminderScheduler.cancel(it.idArticle) }
        val collection = firestore.collection(ARTICLES_COLLECTION)
        remoteWriter.deleteAll(articles.map { collection.document(it.idArticle.toString()) })
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

    fun getArticlesAssignedToUser(groupId: UUID, uid: String) = articleDao.getArticlesAssignedToUser(groupId, uid).map { list ->
        list.map { it.toUIArticle() }
    }

    companion object {
        private const val GAP = 1000.0
    }

}
