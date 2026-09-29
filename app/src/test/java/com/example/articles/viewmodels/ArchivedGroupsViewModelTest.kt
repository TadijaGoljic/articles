package com.example.articles.viewmodels

import com.example.articles.models.UIArticle
import com.example.articles.models.UIGroup
import com.example.articles.repositories.ArticleRepository
import com.example.articles.repositories.GroupRepository
import com.example.articles.repositories.SharedGroupRepository
import com.example.articles.testing.FakeFirebaseAuth
import com.example.articles.testing.MainDispatcherRule
import com.example.articles.utils.TriggerSync
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ArchivedGroupsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAuth: FakeFirebaseAuth

    private val personal = UIGroup(name = "Weekend", isArchived = true)
    private val sharedOwned = UIGroup(name = "Trip", isArchived = true, isShared = true, sharedGroupId = "shared-mine", ownerUid = "uid-a")
    private val sharedForeign = UIGroup(name = "Party", isArchived = true, isShared = true, sharedGroupId = "shared-theirs", ownerUid = "uid-b")
    private val snacks = UIArticle(name = "Snacks", groupId = personal.id)

    private val groupRepository = mockk<GroupRepository>(relaxed = true) {
        every { getArchivedGroups() } returns MutableStateFlow(listOf(personal, sharedOwned, sharedForeign))
        coEvery { getPositionForRestore() } returns -1000.0
    }
    private val articleRepository = mockk<ArticleRepository>(relaxed = true) {
        every { getAllArticles() } returns MutableStateFlow(listOf(snacks))
    }
    private val sharedGroupRepository = mockk<SharedGroupRepository>(relaxed = true)
    private val triggerSync = mockk<TriggerSync>(relaxed = true)

    private lateinit var viewModel: ArchivedGroupsViewModel

    @Before
    fun setUp() {
        fakeAuth = FakeFirebaseAuth(uid = "uid-a")
        viewModel = ArchivedGroupsViewModel(groupRepository, articleRepository, sharedGroupRepository, triggerSync)
    }

    @After
    fun tearDown() = fakeAuth.release()

    @Test
    fun `lists archived groups with their articles`() {
        val groups = viewModel.uiState.value.groups
        assertEquals(listOf(personal.id, sharedOwned.id, sharedForeign.id), groups.map { it.id })
        assertEquals(listOf(snacks), groups.first().articles)
        assertEquals("uid-a", viewModel.uiState.value.currentUid)
    }

    @Test
    fun `restoring a personal group puts it back at the top`() {
        viewModel.handleAction(ArchivedGroupsAction.UnarchiveGroup(personal))

        coVerify { groupRepository.upsertGroup(match { it.id == personal.id && !it.isArchived && it.position == -1000.0 }) }
        verify { triggerSync() }
    }

    @Test
    fun `the owner restores a shared group for everyone`() {
        viewModel.handleAction(ArchivedGroupsAction.UnarchiveGroup(sharedOwned))

        coVerify { sharedGroupRepository.setSharedGroupArchived("shared-mine", archived = false) }
        coVerify { groupRepository.upsertSharedGroupLocal(match { it.id == sharedOwned.id && !it.isArchived }) }
        coVerify(exactly = 0) { groupRepository.upsertGroup(any()) }
    }

    @Test
    fun `a member cannot restore a shared group`() {
        viewModel.handleAction(ArchivedGroupsAction.UnarchiveGroup(sharedForeign))

        coVerify(exactly = 0) { sharedGroupRepository.setSharedGroupArchived(any(), any()) }
        coVerify(exactly = 0) { groupRepository.upsertSharedGroupLocal(any()) }
        coVerify(exactly = 0) { groupRepository.upsertGroup(any()) }
    }
}
