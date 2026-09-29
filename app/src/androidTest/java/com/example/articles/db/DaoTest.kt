package com.example.articles.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.articles.db.entities.ArticleEntity
import com.example.articles.db.entities.CommentEntity
import com.example.articles.db.entities.GroupEntity
import com.example.articles.models.ArticleUnit
import com.example.articles.models.UILocation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DaoTest {

    private lateinit var database: ArticleDatabase
    private val groupDao get() = database.groupDao()
    private val articleDao get() = database.articleDao()
    private val commentDao get() = database.commentDao()

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ArticleDatabase::class.java
        ).build()
    }

    @After
    fun closeDatabase() = database.close()

    private fun group(name: String, position: Double? = null, updatedAt: Long = 0L, isArchived: Boolean = false) =
        GroupEntity(name = name, updatedAt = updatedAt, position = position, isArchived = isArchived)

    private fun article(
        groupId: UUID,
        name: String,
        position: Double? = null,
        bought: Boolean = false,
        updatedAt: Long = 0L,
        assignedToUid: String? = null
    ) = ArticleEntity(
        name = name,
        dateCreated = 0L,
        location = UILocation(44.81, 20.46),
        quantity = 1.0,
        unit = ArticleUnit.PIECES,
        photoUri = null,
        description = null,
        updatedAt = updatedAt,
        groupId = groupId,
        websiteLink = null,
        price = null,
        bought = bought,
        position = position,
        assignedToUid = assignedToUid
    )

    private fun comment(groupId: UUID, articleId: UUID?) = CommentEntity(
        groupId = groupId,
        articleId = articleId,
        authorUid = "uid-a",
        authorEmail = "a@example.com",
        text = "hi",
        createdAt = 0L,
        updatedAt = 0L
    )

    // region groups

    @Test
    fun groupsAreOrderedByPositionWithUnpositionedLast() = runTest {
        val unpositioned = group("C").also { groupDao.upsertGroup(it) }
        val second = group("B", position = 2000.0).also { groupDao.upsertGroup(it) }
        val first = group("A", position = 1000.0).also { groupDao.upsertGroup(it) }

        assertEquals(listOf(first, second, unpositioned), groupDao.getAllGroups().first())
    }

    @Test
    fun archivedGroupsAreSeparatedFromActiveOnes() = runTest {
        val active = group("Active", position = 1000.0).also { groupDao.upsertGroup(it) }
        val archived = group("Archived", isArchived = true).also { groupDao.upsertGroup(it) }

        assertEquals(listOf(active), groupDao.getActiveGroups().first())
        assertEquals(listOf(archived), groupDao.getArchivedGroups().first())
    }

    @Test
    fun maxAndMinPositionIgnoreArchivedGroups() = runTest {
        groupDao.upsertGroup(group("A", position = 1000.0))
        groupDao.upsertGroup(group("B", position = 3000.0))
        groupDao.upsertGroup(group("Archived", position = 9000.0, isArchived = true))

        assertEquals(3000.0, groupDao.getMaxActiveGroupPosition()!!, 0.0)
        assertEquals(1000.0, groupDao.getMinActiveGroupPosition()!!, 0.0)
    }

    @Test
    fun maxPositionIsNullWithoutGroups() = runTest {
        assertNull(groupDao.getMaxActiveGroupPosition())
    }

    @Test
    fun pulledGroupPositionOnlyReplacesAnOlderOne() = runTest {
        val group = group("A", position = 1000.0, updatedAt = 500L).also { groupDao.upsertGroup(it) }

        groupDao.updatePositionIfNewer(group.idGroup, 9000.0, updatedAt = 100L)
        assertEquals(1000.0, groupDao.getGroupByIdSuspend(group.idGroup)!!.position!!, 0.0)

        groupDao.updatePositionIfNewer(group.idGroup, 9000.0, updatedAt = 900L)
        assertEquals(9000.0, groupDao.getGroupByIdSuspend(group.idGroup)!!.position!!, 0.0)
    }

    @Test
    fun pulledGroupPositionFillsAMissingOneEvenIfOlder_regression() = runTest {
        // After signing in again the group is pulled without a position, and its content
        // (updatedAt) is newer than the saved order entry. The order must still be restored.
        val group = group("Shared", position = null, updatedAt = 900L).also { groupDao.upsertGroup(it) }

        groupDao.updatePositionIfNewer(group.idGroup, 1000.0, updatedAt = 100L)

        val restored = groupDao.getGroupByIdSuspend(group.idGroup)!!
        assertEquals(1000.0, restored.position!!, 0.0)
        assertEquals("content timestamp is not moved backwards", 900L, restored.updatedAt)
    }

    @Test
    fun findsGroupBySharedGroupId() = runTest {
        val shared = GroupEntity(name = "S", updatedAt = 0L, isShared = true, sharedGroupId = "shared-1")
        groupDao.upsertGroup(shared)

        assertEquals(shared, groupDao.getGroupBySharedGroupId("shared-1"))
        assertNull(groupDao.getGroupBySharedGroupId("other"))
    }

    // endregion

    // region articles

    @Test
    fun articleFiltersSplitBoughtUnboughtAndAssigned() = runTest {
        val groupId = group("G").also { groupDao.upsertGroup(it) }.idGroup
        val milk = article(groupId, "Milk", position = 1000.0, bought = true)
        val bread = article(groupId, "Bread", position = 2000.0, assignedToUid = "uid-a")
        val eggs = article(groupId, "Eggs", position = 3000.0, assignedToUid = "uid-b")
        articleDao.upsertAllArticles(listOf(eggs, milk, bread))

        assertEquals(listOf(milk, bread, eggs), articleDao.getArticlesByGroupId(groupId).first())
        assertEquals(listOf(milk), articleDao.getBoughtArticlesByGroupId(groupId).first())
        assertEquals(listOf(bread, eggs), articleDao.getUnboughtArticlesByGroupId(groupId).first())
        assertEquals(listOf(bread), articleDao.getArticlesAssignedToUser(groupId, "uid-a").first())
    }

    @Test
    fun articlesRoundTripWithAllFields() = runTest {
        val groupId = group("G").also { groupDao.upsertGroup(it) }.idGroup
        val article = article(groupId, "Milk").copy(
            price = 1.5, description = "fresh", websiteLink = "https://example.com", dateTo = 123L
        )
        articleDao.upsertArticle(article)

        assertEquals(article, articleDao.getArticleByIdSuspend(article.idArticle))
    }

    @Test
    fun nextArticlePositionIsPerGroup() = runTest {
        val a = group("A").also { groupDao.upsertGroup(it) }.idGroup
        val b = group("B").also { groupDao.upsertGroup(it) }.idGroup
        articleDao.upsertArticle(article(a, "x", position = 5000.0))

        assertEquals(5000.0, articleDao.getMaxArticlePosition(a)!!, 0.0)
        assertNull(articleDao.getMaxArticlePosition(b))
    }

    @Test
    fun pulledArticlePositionFillsAMissingOneEvenIfOlder_regression() = runTest {
        val groupId = group("G").also { groupDao.upsertGroup(it) }.idGroup
        val edited = article(groupId, "Milk", position = null, updatedAt = 900L)
        val ordered = article(groupId, "Bread", position = 2000.0, updatedAt = 900L)
        articleDao.upsertAllArticles(listOf(edited, ordered))

        articleDao.updatePositionIfNewer(edited.idArticle, 1000.0, updatedAt = 100L)
        articleDao.updatePositionIfNewer(ordered.idArticle, 9999.0, updatedAt = 100L)

        assertEquals(1000.0, articleDao.getArticleByIdSuspend(edited.idArticle)!!.position!!, 0.0)
        assertEquals("an existing, newer position is kept", 2000.0, articleDao.getArticleByIdSuspend(ordered.idArticle)!!.position!!, 0.0)
    }

    @Test
    fun reassigningArticlesMovesThemToTheNewGroup() = runTest {
        val old = group("Old").also { groupDao.upsertGroup(it) }.idGroup
        val new = group("New").also { groupDao.upsertGroup(it) }.idGroup
        articleDao.upsertAllArticles(listOf(article(old, "a"), article(old, "b")))

        articleDao.reassignGroupId(old, new)

        assertTrue(articleDao.getArticlesByGroupIdSuspend(old).isEmpty())
        assertEquals(2, articleDao.getArticlesByGroupIdSuspend(new).size)
    }

    // endregion

    // region cascades and comments

    @Test
    fun deletingAGroupCascadesToArticlesAndComments() = runTest {
        val groupId = group("G").also { groupDao.upsertGroup(it) }.idGroup
        val article = article(groupId, "Milk").also { articleDao.upsertArticle(it) }
        commentDao.upsertAllComments(listOf(comment(groupId, null), comment(groupId, article.idArticle)))

        groupDao.deleteGroupById(groupId)

        assertTrue(articleDao.getAllArticles().first().isEmpty())
        assertTrue(commentDao.getAllComments().first().isEmpty())
    }

    @Test
    fun deletingAnArticleCascadesToItsComments() = runTest {
        val groupId = group("G").also { groupDao.upsertGroup(it) }.idGroup
        val article = article(groupId, "Milk").also { articleDao.upsertArticle(it) }
        val groupComment = comment(groupId, null)
        commentDao.upsertAllComments(listOf(groupComment, comment(groupId, article.idArticle)))

        articleDao.deleteArticleById(article.idArticle)

        assertEquals(listOf(groupComment), commentDao.getAllComments().first())
    }

    @Test
    fun groupAndArticleCommentsAreQueriedSeparately() = runTest {
        val groupId = group("G").also { groupDao.upsertGroup(it) }.idGroup
        val article = article(groupId, "Milk").also { articleDao.upsertArticle(it) }
        val groupComment = comment(groupId, null)
        val articleComment = comment(groupId, article.idArticle)
        commentDao.upsertAllComments(listOf(groupComment, articleComment))

        assertEquals(listOf(groupComment), commentDao.getCommentsForGroup(groupId).first())
        assertEquals(listOf(articleComment), commentDao.getCommentsForArticle(article.idArticle).first())
        assertEquals(2, commentDao.getAllCommentsForGroupId(groupId).first().size)
    }

    // endregion
}
