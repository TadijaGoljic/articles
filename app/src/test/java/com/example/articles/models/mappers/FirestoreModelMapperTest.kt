package com.example.articles.models.mappers

import android.content.Context
import com.example.articles.db.entities.CommentEntity
import com.example.articles.db.entities.GroupEntity
import com.example.articles.models.ArticleUnit
import com.example.articles.models.InvitationStatus
import com.example.articles.models.UIInvitation
import com.example.articles.models.UILocation
import com.example.articles.utils.FileManager
import com.google.firebase.firestore.DocumentSnapshot
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.UUID

class FirestoreModelMapperTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mapper: FirestoreModelMapper

    @Before
    fun setUp() {
        val context = mockk<Context> {
            every { cacheDir } returns tempFolder.root
        }
        mapper = FirestoreModelMapper(mockk<FileManager>(), context)
    }

    private fun snapshot(data: Map<String, Any?>?): DocumentSnapshot {
        val snapshot = mockk<DocumentSnapshot>()
        every { snapshot.data } returns data
        every { snapshot.get(any<String>()) } answers { data?.get(firstArg<String>()) }
        return snapshot
    }

    private val articleId = UUID.randomUUID()
    private val groupId = UUID.randomUUID()

    private fun articleData(vararg overrides: Pair<String, Any?>) = mutableMapOf<String, Any?>(
        "idArticle" to articleId.toString(),
        "name" to "Milk",
        "dateCreated" to 100L,
        "location" to "44.8,20.4",
        "quantity" to 2.0,
        "unit" to "LITERS",
        "photo" to null,
        "updatedAt" to 200L,
        "groupId" to groupId.toString(),
        "description" to "fresh",
        "websiteLink" to "https://example.com",
        "price" to 1.5,
        "bought" to true,
        "dateTo" to 300L,
        "assignedToUid" to "uid-b"
    ).apply { putAll(overrides) }

    @Test
    fun `maps a complete article document`() {
        val entity = mapper.toArticleEntity(snapshot(articleData()))!!

        assertEquals(articleId, entity.idArticle)
        assertEquals("Milk", entity.name)
        assertEquals(UILocation(44.8, 20.4), entity.location)
        assertEquals(ArticleUnit.LITERS, entity.unit)
        assertEquals(groupId, entity.groupId)
        assertEquals(1.5, entity.price!!, 0.0)
        assertEquals(true, entity.bought)
        assertEquals(300L, entity.dateTo)
        assertEquals("uid-b", entity.assignedToUid)
        assertNull(entity.photoUri)
    }

    @Test
    fun `article without a unit falls back to the default instead of crashing`() {
        // Regression: the fallback used to be "PIECE", which is not an ArticleUnit and threw.
        val entity = mapper.toArticleEntity(snapshot(articleData("unit" to null)))!!
        assertEquals(ArticleUnit.default, entity.unit)
    }

    @Test
    fun `article with an unknown unit falls back to the default`() {
        val entity = mapper.toArticleEntity(snapshot(articleData("unit" to "BUSHELS")))!!
        assertEquals(ArticleUnit.default, entity.unit)
    }

    @Test
    fun `article without a name or group is skipped`() {
        assertNull(mapper.toArticleEntity(snapshot(articleData("name" to null))))
        assertNull(mapper.toArticleEntity(snapshot(articleData("groupId" to null))))
        assertNull(mapper.toArticleEntity(snapshot(articleData("groupId" to "not-a-uuid"))))
    }

    @Test
    fun `article without an id or data is skipped`() {
        assertNull(mapper.toArticleEntity(snapshot(articleData("idArticle" to null))))
        assertNull(mapper.toArticleEntity(snapshot(null)))
    }

    @Test
    fun `optional article fields may be missing`() {
        val entity = mapper.toArticleEntity(
            snapshot(
                articleData(
                    "location" to null, "quantity" to null, "price" to null,
                    "bought" to null, "dateTo" to null, "updatedAt" to null
                )
            )
        )!!
        assertNull(entity.location)
        assertNull(entity.quantity)
        assertNull(entity.price)
        assertEquals(false, entity.bought)
        assertNull(entity.dateTo)
        assertEquals(0L, entity.updatedAt)
    }

    @Test
    fun `group map and entity agree`() {
        val group = GroupEntity(
            idGroup = UUID.randomUUID(),
            name = "Groceries",
            updatedAt = 5L,
            isShared = true,
            sharedGroupId = "shared-1",
            ownerUid = "uid-a",
            isArchived = true
        )
        val map = mapper.toGroupsFirestoreMap(group)
        assertEquals(group, mapper.toGroupEntity(snapshot(map)))
    }

    @Test
    fun `comment map and entity agree`() {
        val comment = CommentEntity(
            idComment = UUID.randomUUID(),
            groupId = UUID.randomUUID(),
            articleId = null,
            authorUid = "uid-a",
            authorEmail = "a@example.com",
            text = "hi",
            createdAt = 1L,
            updatedAt = 2L
        )
        val map = mapper.toCommentsFirestoreMap(comment, ownerUid = "uid-a")
        assertEquals("uid-a", map["ownerUid"])
        assertNull(map["sharedGroupId"])
        assertEquals(comment, mapper.toCommentEntity(snapshot(map)))
    }

    @Test
    fun `comment without an author is skipped`() {
        val map = mapper.toCommentsFirestoreMap(
            CommentEntity(
                groupId = UUID.randomUUID(), articleId = null, authorUid = "x",
                authorEmail = "", text = "", createdAt = 0L, updatedAt = 0L
            )
        ).toMutableMap().apply { this["authorUid"] = null }
        assertNull(mapper.toCommentEntity(snapshot(map)))
    }

    @Test
    fun `invitation map and model agree`() {
        val invitation = UIInvitation(
            invitationId = "inv-1",
            fromUid = "uid-a",
            fromEmail = "a@example.com",
            toEmail = "b@example.com",
            toUid = "uid-b",
            groupId = UUID.randomUUID().toString(),
            groupName = "Groceries",
            status = InvitationStatus.PENDING,
            createdAt = 99L,
            existingSharedGroupId = "shared-1"
        )
        assertEquals(invitation, mapper.toInvitation(snapshot(mapper.toInvitationFirestoreMap(invitation))))
    }

    @Test
    fun `invitation missing a required field is skipped`() {
        val map = mapOf("invitationId" to "inv-1", "fromUid" to "uid-a")
        assertNull(mapper.toInvitation(snapshot(map)))
    }
}
