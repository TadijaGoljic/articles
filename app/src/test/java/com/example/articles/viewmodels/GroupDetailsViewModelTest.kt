package com.example.articles.viewmodels

import com.example.articles.models.UIArticle
import com.example.articles.models.UIComment
import com.example.articles.models.UIGroup
import com.example.articles.repositories.ArticleRepository
import com.example.articles.repositories.CommentRepository
import com.example.articles.repositories.GroupRepository
import com.example.articles.repositories.SharedGroupRepository
import com.example.articles.testing.FakeFirebaseAuth
import com.example.articles.testing.MainDispatcherRule
import com.example.articles.testing.releaseRoute
import com.example.articles.testing.savedStateHandleFor
import com.example.articles.ui.composables.ArticlesDestinations
import com.example.articles.utils.TriggerSync
import com.google.firebase.firestore.FirebaseFirestoreException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GroupDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAuth: FakeFirebaseAuth

    private val group = UIGroup(name = "Groceries")
    private val milk = UIArticle(name = "Milk", groupId = group.id, bought = true, position = 1000.0)
    private val bread = UIArticle(name = "Bread", groupId = group.id, bought = false, position = 2000.0)
    private val eggs = UIArticle(name = "Eggs", groupId = group.id, bought = false, position = 3000.0, assignedToUid = "uid-a")

    private val allArticles = MutableStateFlow(listOf(milk, bread, eggs))
    private val boughtArticles = MutableStateFlow(listOf(milk))
    private val unboughtArticles = MutableStateFlow(listOf(bread, eggs))
    private val assignedToMe = MutableStateFlow(listOf(eggs))

    private val milkComments = MutableStateFlow(listOf(UIComment(groupId = group.id, articleId = milk.id, text = "milk comment")))
    private val breadComments = MutableStateFlow(listOf(UIComment(groupId = group.id, articleId = bread.id, text = "bread comment")))

    private val groupRepository = mockk<GroupRepository>(relaxed = true) {
        every { getGroupById(group.id) } returns MutableStateFlow(group)
    }
    private val articleRepository = mockk<ArticleRepository>(relaxed = true) {
        every { getArticlesByGroupId(group.id) } returns allArticles
        every { getBoughtArticlesByGroupId(group.id) } returns boughtArticles
        every { getUnboughtArticlesByGroupId(group.id) } returns unboughtArticles
        every { getArticlesAssignedToUser(group.id, "uid-a") } returns assignedToMe
    }
    private val commentRepository = mockk<CommentRepository>(relaxed = true) {
        every { getAllCommentsForGroup(group.id) } returns MutableStateFlow(milkComments.value + breadComments.value)
        every { getCommentsForArticle(milk.id) } returns milkComments
        every { getCommentsForArticle(bread.id) } returns breadComments
    }
    private val sharedGroupRepository = mockk<SharedGroupRepository>(relaxed = true)

    private lateinit var viewModel: GroupDetailsViewModel

    @Before
    fun setUp() {
        fakeAuth = FakeFirebaseAuth(uid = "uid-a")
        viewModel = GroupDetailsViewModel(
            savedStateHandle = savedStateHandleFor(ArticlesDestinations.GroupDetailsDestination(groupId = group.id.toString())),
            groupRepository = groupRepository,
            articleRepository = articleRepository,
            commentRepository = commentRepository,
            sharedGroupRepository = sharedGroupRepository,
            triggerSync = mockk<TriggerSync>(relaxed = true)
        )
    }

    @After
    fun tearDown() {
        fakeAuth.release()
        releaseRoute()
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun `shows all articles and the group by default`() {
        assertEquals(listOf(milk, bread, eggs), state.articles)
        assertEquals(group, state.group)
    }

    @Test
    fun `comment counts are grouped per article`() {
        assertEquals(mapOf(milk.id to 1, bread.id to 1), state.articleCommentCounts)
    }

    @Test
    fun `each filter shows its own articles`() {
        viewModel.handleAction(GroupDetailsAction.FilterSelected(FilterType.BOUGHT))
        assertEquals(listOf(milk), state.articles)

        viewModel.handleAction(GroupDetailsAction.FilterSelected(FilterType.UNBOUGHT))
        assertEquals(listOf(bread, eggs), state.articles)

        viewModel.handleAction(GroupDetailsAction.FilterSelected(FilterType.ASSIGNED_TO_ME))
        assertEquals(listOf(eggs), state.articles)

        viewModel.handleAction(GroupDetailsAction.FilterSelected(FilterType.ALL))
        assertEquals(listOf(milk, bread, eggs), state.articles)
    }

    @Test
    fun `a database change keeps the selected filter - regression`() {
        viewModel.handleAction(GroupDetailsAction.FilterSelected(FilterType.BOUGHT))

        val milkNotBought = milk.copy(bought = false)
        boughtArticles.value = emptyList()
        unboughtArticles.value = listOf(milkNotBought, bread, eggs)
        allArticles.value = listOf(milkNotBought, bread, eggs)

        assertEquals(emptyList<UIArticle>(), state.articles)
    }

    @Test
    fun `switching filters repeatedly leaves only the last one active`() {
        viewModel.handleAction(GroupDetailsAction.FilterSelected(FilterType.UNBOUGHT))
        viewModel.handleAction(GroupDetailsAction.FilterSelected(FilterType.BOUGHT))

        unboughtArticles.value = listOf(bread)

        assertEquals(listOf(milk), state.articles)
    }

    @Test
    fun `the comment sheet only follows the article it is open for`() {
        viewModel.handleAction(GroupDetailsAction.ShowArticleComments(milk))
        viewModel.handleAction(GroupDetailsAction.HideCommentsSheet)
        viewModel.handleAction(GroupDetailsAction.ShowArticleComments(bread))

        milkComments.value += UIComment(groupId = group.id, articleId = milk.id, text = "another")

        assertEquals(breadComments.value, state.articleComments)
        assertEquals(bread, state.articleCommentsTarget)
    }

    @Test
    fun `adding a comment stores it on the open article`() {
        viewModel.handleAction(GroupDetailsAction.ShowArticleComments(bread))
        viewModel.handleAction(GroupDetailsAction.AddComment("needs to be fresh"))

        coVerify {
            commentRepository.upsertComment(match {
                it.articleId == bread.id && it.groupId == group.id && it.text == "needs to be fresh" && it.authorUid == "uid-a"
            })
        }
    }

    @Test
    fun `blank comments are ignored`() {
        viewModel.handleAction(GroupDetailsAction.AddComment("  "))
        coVerify(exactly = 0) { commentRepository.upsertComment(any()) }
    }

    @Test
    fun `marking as bought updates the long-pressed article`() {
        viewModel.handleAction(GroupDetailsAction.ArticleLongClick(bread))
        viewModel.handleAction(GroupDetailsAction.MarkAsBought)

        coVerify { articleRepository.upsertArticle(bread.copy(bought = true)) }
    }

    @Test
    fun `assigning an article updates its assignee and closes the dialog`() {
        viewModel.handleAction(GroupDetailsAction.ArticleLongClick(bread))
        viewModel.handleAction(GroupDetailsAction.ShowAssignDialog)
        viewModel.handleAction(GroupDetailsAction.AssignArticle("uid-b"))

        coVerify { articleRepository.updateArticleAssignee(bread, "uid-b") }
        assertEquals(false, state.showAssignDialog)
    }

    @Test
    fun `removing an article also removes its comments`() {
        viewModel.handleAction(GroupDetailsAction.ArticleLongClick(bread))
        viewModel.handleAction(GroupDetailsAction.RemoveArticle)

        coVerify { articleRepository.deleteArticleById(bread.id) }
        coVerify { commentRepository.deleteCommentsByArticleId(group.id, bread.id) }
    }

    @Test
    fun `a rejected remote write does not crash the screen`() {
        coEvery { articleRepository.upsertArticle(any()) } throws FirebaseFirestoreException(
            "PERMISSION_DENIED", FirebaseFirestoreException.Code.PERMISSION_DENIED
        )

        viewModel.handleAction(GroupDetailsAction.ArticleLongClick(bread))
        viewModel.handleAction(GroupDetailsAction.MarkAsBought)

        assertEquals(listOf(milk, bread, eggs), state.articles)
    }

    @Test
    fun `reordering between converged neighbours renumbers the group`() {
        viewModel.handleAction(GroupDetailsAction.ReorderArticle(bread.id, previousPosition = 1000.0, nextPosition = 1000.0))
        coVerify { articleRepository.renumberArticlePositions(group.id) }
    }

    @Test
    fun `reordering between spaced neighbours takes the midpoint`() {
        viewModel.handleAction(GroupDetailsAction.ReorderArticle(eggs.id, previousPosition = 1000.0, nextPosition = 2000.0))
        coVerify { articleRepository.updateArticlePosition(eggs.id, group.id, 1500.0) }
    }
}
