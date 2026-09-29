package com.example.articles.models.mappers

import com.example.articles.models.ArticleUnit
import com.example.articles.models.UIArticle
import com.example.articles.models.UIComment
import com.example.articles.models.UIGroup
import com.example.articles.models.UILocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.UUID

class MappersTest {

    @Test
    fun `article survives a round trip through the entity`() {
        val article = UIArticle(
            name = "Milk",
            description = "2%",
            dateCreated = Date(1_000L),
            location = UILocation(44.8, 20.4),
            quantity = 2.0,
            unit = ArticleUnit.LITERS,
            photoUri = "file:///photo.jpg",
            groupId = UUID.randomUUID(),
            websiteLink = "https://example.com",
            price = 1.5,
            bought = true,
            dateTo = Date(5_000L),
            position = 3000.0,
            assignedToUid = "uid-b"
        )

        val mapped = article.toArticleEntity().toUIArticle()

        assertEquals(article.copy(updatedAt = mapped.updatedAt), mapped)
    }

    @Test
    fun `writing an article bumps updatedAt`() {
        val before = System.currentTimeMillis()
        val entity = UIArticle(name = "Bread", updatedAt = 0L).toArticleEntity()
        assertTrue(entity.updatedAt >= before)
    }

    @Test
    fun `missing deadline stays missing`() {
        assertNull(UIArticle(name = "Eggs").toArticleEntity().toUIArticle().dateTo)
    }

    @Test
    fun `group survives a round trip through the entity`() {
        val group = UIGroup(
            name = "Groceries",
            updatedAt = 42L,
            isShared = true,
            sharedGroupId = "shared-1",
            ownerUid = "uid-a",
            isArchived = true,
            position = 1000.0
        )
        assertEquals(group, group.toGroupEntity().toUIGroup())
    }

    @Test
    fun `comment survives a round trip through the entity`() {
        val comment = UIComment(
            groupId = UUID.randomUUID(),
            articleId = UUID.randomUUID(),
            authorUid = "uid-a",
            authorEmail = "a@example.com",
            text = "hello",
            createdAt = 10L
        )
        val mapped = comment.toCommentEntity().toUIComment()
        assertEquals(comment.copy(updatedAt = mapped.updatedAt), mapped)
    }

    @Test
    fun `personal groups are always owned by the current user`() {
        assertTrue(UIGroup(isShared = false, ownerUid = "someone-else").isGroupOwner("me"))
    }

    @Test
    fun `shared group ownership follows ownerUid`() {
        assertTrue(UIGroup(isShared = true, ownerUid = "me").isGroupOwner("me"))
        assertFalse(UIGroup(isShared = true, ownerUid = "someone-else").isGroupOwner("me"))
    }

    @Test
    fun `shared group without a known owner is treated as owned`() {
        assertTrue(UIGroup(isShared = true, ownerUid = null).isGroupOwner("me"))
    }
}
