package com.example.articles.viewmodels

import com.example.articles.R
import com.example.articles.ai.GroupAgentCommand
import com.example.articles.ai.GroupAgentRepository
import com.example.articles.models.UIArticle
import com.example.articles.models.UIGroup
import com.example.articles.models.UserPreferences
import com.example.articles.repositories.ArticleRepository
import com.example.articles.repositories.CommentRepository
import com.example.articles.repositories.GroupMember
import com.example.articles.repositories.GroupRepository
import com.example.articles.repositories.InvitationRepository
import com.example.articles.repositories.SharedGroupRepository
import com.example.articles.testing.FakeFirebaseAuth
import com.example.articles.testing.MainDispatcherRule
import com.example.articles.testing.fakeContext
import com.example.articles.testing.stringRes
import com.example.articles.utils.CurrentLocationProvider
import com.example.articles.utils.TriggerSync
import com.google.firebase.firestore.FirebaseFirestoreException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GroupsPageViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAuth: FakeFirebaseAuth

    private val groceries = UIGroup(name = "Groceries", position = 1000.0)
    private val sharedOwned = UIGroup(
        name = "Shared mine", isShared = true, sharedGroupId = "shared-mine", ownerUid = "uid-a", position = 2000.0
    )
    private val sharedForeign = UIGroup(
        name = "Shared theirs", isShared = true, sharedGroupId = "shared-theirs", ownerUid = "uid-b", position = 3000.0
    )

    private val activeGroups = MutableStateFlow(listOf(groceries, sharedOwned, sharedForeign))
    private val allArticles = MutableStateFlow(emptyList<UIArticle>())

    private val groupRepository = mockk<GroupRepository>(relaxed = true) {
        every { getActiveGroups() } returns activeGroups
        coEvery { getNextGroupPosition() } returns 4000.0
    }
    private val articleRepository = mockk<ArticleRepository>(relaxed = true) {
        every { getAllArticles() } returns allArticles
        coEvery { getNextArticlePosition(any()) } returns 1000.0
    }
    private val commentRepository = mockk<CommentRepository>(relaxed = true)
    private val invitationRepository = mockk<InvitationRepository>(relaxed = true) {
        every { observePendingInvitations(any()) } returns flowOf(emptyList())
        coEvery { getRecentShareRecipients(any(), any()) } returns emptyList()
    }
    private val sharedGroupRepository = mockk<SharedGroupRepository>(relaxed = true) {
        coEvery { getMembers(any()) } returns emptyList()
    }
    private val groupAgentRepository = mockk<GroupAgentRepository>()
    private val currentLocationProvider = mockk<CurrentLocationProvider> {
        coEvery { getCurrentLocation() } returns null
    }
    private val userPreferences = mockk<UserPreferences>(relaxed = true) {
        every { confirmNonDestructiveAgentActions } returns false
    }
    private val triggerSync = mockk<TriggerSync>(relaxed = true)

    private lateinit var viewModel: GroupsPageViewModel

    @Before
    fun setUp() {
        fakeAuth = FakeFirebaseAuth(uid = "uid-a", email = "a@example.com")
        viewModel = GroupsPageViewModel(
            groupRepository = groupRepository,
            articleRepository = articleRepository,
            commentRepository = commentRepository,
            invitationRepository = invitationRepository,
            sharedGroupRepository = sharedGroupRepository,
            groupAgentRepository = groupAgentRepository,
            currentLocationProvider = currentLocationProvider,
            userPreferences = userPreferences,
            triggerSync = triggerSync,
            context = fakeContext()
        )
    }

    @After
    fun tearDown() = fakeAuth.release()

    private val state get() = viewModel.uiState.value

    @Test
    fun `groups from the repository are exposed with their articles`() {
        val milk = UIArticle(name = "Milk", groupId = groceries.id)
        allArticles.value = listOf(milk, UIArticle(name = "Elsewhere"))

        assertEquals(listOf(milk), state.groups.first { it.id == groceries.id }.articles)
    }

    @Test
    fun `new group names are trimmed`() {
        viewModel.handleAction(GroupsPageAction.SaveGroup("  Party  "))

        coVerify { groupRepository.upsertGroup(match { it.name == "Party" && it.position == 4000.0 }) }
        assertFalse(state.showAddNewGroupDialog)
    }

    @Test
    fun `a whitespace-only group name is rejected`() {
        viewModel.handleAction(GroupsPageAction.ShowAddNewGroupDialog)
        viewModel.handleAction(GroupsPageAction.SaveGroup("   "))

        coVerify(exactly = 0) { groupRepository.upsertGroup(any()) }
        assertTrue(state.isGroupCreationError)
        assertEquals(R.string.group_name_blank, state.groupCreationErrorResource)
    }

    @Test
    fun `a name differing only by case or surrounding spaces is a duplicate`() {
        viewModel.handleAction(GroupsPageAction.SaveGroup("groceries "))

        coVerify(exactly = 0) { groupRepository.upsertGroup(any()) }
        assertEquals(R.string.group_already_exists, state.groupCreationErrorResource)
    }

    @Test
    fun `a failing save does not crash and stops loading`() {
        coEvery { groupRepository.upsertGroup(any()) } throws permissionDenied()

        viewModel.handleAction(GroupsPageAction.SaveGroup("Party"))

        assertFalse(state.isCreateGroupDialogLoading)
    }

    @Test
    fun `renaming a personal group writes the personal group`() {
        viewModel.handleAction(GroupsPageAction.EditGroup(groceries, " Food "))

        coVerify { groupRepository.upsertGroup(match { it.id == groceries.id && it.name == "Food" }) }
        coVerify(exactly = 0) { sharedGroupRepository.renameSharedGroup(any(), any()) }
    }

    @Test
    fun `the owner renaming a shared group renames the shared document - regression for crash`() {
        viewModel.handleAction(GroupsPageAction.EditGroup(sharedOwned, "Shop"))

        coVerify { sharedGroupRepository.renameSharedGroup("shared-mine", "Shop") }
        coVerify { groupRepository.upsertGroupLocal(match { it.id == sharedOwned.id && it.name == "Shop" }) }
        coVerify(exactly = 0) { groupRepository.upsertGroup(any()) }
        assertFalse(state.showEditGroupDialog)
    }

    @Test
    fun `a non-owner cannot rename a shared group`() {
        viewModel.handleAction(GroupsPageAction.ShowEditGroupDialog(sharedForeign))
        viewModel.handleAction(GroupsPageAction.EditGroup(sharedForeign, "Mine now"))

        coVerify(exactly = 0) { sharedGroupRepository.renameSharedGroup(any(), any()) }
        coVerify(exactly = 0) { groupRepository.upsertGroup(any()) }
        assertFalse(state.showEditGroupDialog)
    }

    @Test
    fun `a rejected rename does not crash the app`() {
        coEvery { sharedGroupRepository.renameSharedGroup(any(), any()) } throws permissionDenied()

        viewModel.handleAction(GroupsPageAction.ShowEditGroupDialog(sharedOwned))
        viewModel.handleAction(GroupsPageAction.EditGroup(sharedOwned, "Shop"))

        assertFalse(state.isEditDialogLoading)
        assertTrue(state.showEditGroupDialog)
    }

    @Test
    fun `renaming to another group's name is ignored`() {
        viewModel.handleAction(GroupsPageAction.EditGroup(sharedOwned, "GROCERIES"))

        coVerify(exactly = 0) { sharedGroupRepository.renameSharedGroup(any(), any()) }
    }

    @Test
    fun `changing only the letter case is a real rename`() {
        viewModel.handleAction(GroupsPageAction.EditGroup(groceries, "GROCERIES"))

        coVerify { groupRepository.upsertGroup(match { it.name == "GROCERIES" }) }
    }

    private fun share(group: UIGroup, email: String) {
        viewModel.handleAction(GroupsPageAction.LongClick(group))
        viewModel.handleAction(GroupsPageAction.ShareGroup(email))
    }

    @Test
    fun `sharing with your own email is rejected before any lookup`() {
        share(groceries, "  A@Example.com ")

        assertTrue(state.isShareGroupError)
        assertEquals(R.string.cannot_share_with_yourself, state.groupSharingErrorResource)
        coVerify(exactly = 0) { invitationRepository.lookupUidByEmail(any()) }
    }

    @Test
    fun `sharing with an account that resolves to yourself is rejected`() {
        coEvery { invitationRepository.lookupUidByEmail(any()) } returns "uid-a"

        share(groceries, "alias@example.com")

        assertEquals(R.string.cannot_share_with_yourself, state.groupSharingErrorResource)
        coVerify(exactly = 0) { invitationRepository.sendInvitation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `invite emails are normalized before lookup and storage`() {
        coEvery { invitationRepository.lookupUidByEmail("b@example.com") } returns "uid-b"

        share(groceries, " B@Example.COM ")

        coVerify {
            invitationRepository.sendInvitation(
                fromUid = "uid-a",
                fromEmail = "a@example.com",
                toEmail = "b@example.com",
                toUid = "uid-b",
                group = match { it.id == groceries.id }
            )
        }
        assertTrue(state.invitationSentEvent)
        assertFalse(state.showShareGroupDialog)
    }

    @Test
    fun `an unknown email is reported`() {
        coEvery { invitationRepository.lookupUidByEmail(any()) } returns null

        share(groceries, "nobody@example.com")

        assertEquals(R.string.user_not_found, state.groupSharingErrorResource)
    }

    @Test
    fun `inviting someone who is already a member is rejected`() {
        coEvery { invitationRepository.lookupUidByEmail(any()) } returns "uid-b"
        coEvery { sharedGroupRepository.getMembers("shared-mine") } returns listOf(
            GroupMember("uid-a", "a@example.com"),
            GroupMember("uid-b", "b@example.com")
        )

        share(sharedOwned, "b@example.com")

        assertEquals(R.string.user_already_member, state.groupSharingErrorResource)
        coVerify(exactly = 0) { invitationRepository.sendInvitation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `deleting a personal group removes its articles, comments and the group`() {
        viewModel.handleAction(GroupsPageAction.DeleteGroup(groceries))

        coVerifyOrder {
            articleRepository.deleteArticlesByGroupId(groceries.id)
            commentRepository.deleteCommentsByGroupId(groceries.id)
            groupRepository.deleteGroupById(groceries.id)
        }
    }

    @Test
    fun `the owner deleting a shared group deletes it for everyone`() {
        viewModel.handleAction(GroupsPageAction.DeleteGroup(sharedOwned))

        coVerify { sharedGroupRepository.deleteSharedGroup("shared-mine") }
        coVerify { groupRepository.deleteGroupLocalById(sharedOwned.id) }
    }

    @Test
    fun `a member deleting a shared group only leaves it`() {
        viewModel.handleAction(GroupsPageAction.DeleteGroup(sharedForeign))

        coVerify { sharedGroupRepository.leaveSharedGroup("shared-theirs", "uid-a") }
        coVerify(exactly = 0) { sharedGroupRepository.deleteSharedGroup(any()) }
    }

    @Test
    fun `dropping a group between two others takes the midpoint`() {
        viewModel.handleAction(
            GroupsPageAction.ReorderGroup(
                groupId = sharedForeign.id,
                orderedGroupIds = listOf(groceries.id, sharedForeign.id, sharedOwned.id)
            )
        )

        coVerify { groupRepository.updateGroupPosition(sharedForeign.id, 1500.0) }
    }

    @Test
    fun `dropping between converged neighbours renumbers in the dropped order`() {
        activeGroups.value = listOf(groceries.copy(position = 1000.0), sharedOwned.copy(position = 1000.0), sharedForeign)
        val dropped = listOf(groceries.id, sharedForeign.id, sharedOwned.id)

        viewModel.handleAction(GroupsPageAction.ReorderGroup(groupId = sharedForeign.id, orderedGroupIds = dropped))

        coVerify { groupRepository.renumberGroups(dropped) }
    }

    @Test
    fun `archiving a personal group resets its articles to unbought and hides it`() {
        viewModel.handleAction(GroupsPageAction.ArchiveGroup(groceries))

        coVerifyOrder {
            articleRepository.resetBoughtArticles(groceries.id)
            groupRepository.upsertGroup(match { it.id == groceries.id && it.isArchived && it.position == null })
        }
        assertEquals("Groceries", state.archivedGroupEvent)
    }

    @Test
    fun `the owner archiving a shared group archives it for everyone - regression for sync undoing it`() {
        viewModel.handleAction(GroupsPageAction.ArchiveGroup(sharedOwned))

        coVerifyOrder {
            articleRepository.resetBoughtArticles(sharedOwned.id)
            sharedGroupRepository.setSharedGroupArchived("shared-mine", archived = true)
            groupRepository.upsertSharedGroupLocal(match { it.id == sharedOwned.id && it.isArchived && it.position == null })
        }
        coVerify(exactly = 0) { groupRepository.upsertGroup(any()) }
    }

    @Test
    fun `a member cannot archive someone else's shared group`() {
        viewModel.handleAction(GroupsPageAction.ArchiveGroup(sharedForeign))

        coVerify(exactly = 0) { articleRepository.resetBoughtArticles(any()) }
        coVerify(exactly = 0) { sharedGroupRepository.setSharedGroupArchived(any(), any()) }
        coVerify(exactly = 0) { groupRepository.upsertSharedGroupLocal(any()) }
        assertNull(state.archivedGroupEvent)
    }

    @Test
    fun `a rejected archive does not crash`() {
        coEvery { sharedGroupRepository.setSharedGroupArchived(any(), any()) } throws permissionDenied()

        viewModel.handleAction(GroupsPageAction.ArchiveGroup(sharedOwned))

        assertNull(state.archivedGroupEvent)
    }

    @Test
    fun `agent commands run in order and close the dialog`() {
        val party = UIGroup(name = "Party")
        val chips = UIArticle(name = "Chips", groupId = party.id)
        coEvery { groupAgentRepository.handlePrompt(any(), any(), any()) } returns listOf(
            GroupAgentCommand.CreateGroup(party),
            GroupAgentCommand.AddArticle(party, chips)
        )

        viewModel.handleAction(GroupsPageAction.ShowAiAgentDialog)
        viewModel.handleAction(GroupsPageAction.SubmitAgentPrompt("create Party with chips"))

        coVerifyOrder {
            groupRepository.upsertGroup(match { it.id == party.id && it.position == 4000.0 })
            articleRepository.upsertArticle(match { it.id == chips.id && it.position == 1000.0 })
        }
        assertFalse(state.showAiAgentDialog)
        assertNull(state.agentError)
    }

    @Test
    fun `destructive agent commands wait for confirmation`() {
        coEvery { groupAgentRepository.handlePrompt(any(), any(), any()) } returns listOf(
            GroupAgentCommand.DeleteGroup(groceries)
        )

        viewModel.handleAction(GroupsPageAction.ShowAiAgentDialog)
        viewModel.handleAction(GroupsPageAction.SubmitAgentPrompt("delete groceries"))

        assertEquals(GroupAgentCommand.DeleteGroup(groceries), state.agentPendingCommand)
        coVerify(exactly = 0) { groupRepository.deleteGroupById(any()) }

        viewModel.handleAction(GroupsPageAction.ConfirmPendingAgentCommand)

        coVerify { groupRepository.deleteGroupById(groceries.id) }
        assertFalse(state.showAiAgentDialog)
    }

    @Test
    fun `cancelling a pending command drops the rest of the batch`() {
        coEvery { groupAgentRepository.handlePrompt(any(), any(), any()) } returns listOf(
            GroupAgentCommand.DeleteGroup(groceries),
            GroupAgentCommand.CreateGroup(UIGroup(name = "Party"))
        )

        viewModel.handleAction(GroupsPageAction.ShowAiAgentDialog)
        viewModel.handleAction(GroupsPageAction.SubmitAgentPrompt("..."))
        viewModel.handleAction(GroupsPageAction.CancelPendingAgentCommand)

        assertNull(state.agentPendingCommand)
        assertTrue(state.agentCommandQueue.isEmpty())
        coVerify(exactly = 0) { groupRepository.upsertGroup(any()) }
    }

    @Test
    fun `answers are shown until the user dismisses them`() {
        val answer = GroupAgentCommand.AnswerQuestion("Buy milk first.")
        coEvery { groupAgentRepository.handlePrompt(any(), any(), any()) } returns listOf(answer)

        viewModel.handleAction(GroupsPageAction.ShowAiAgentDialog)
        viewModel.handleAction(GroupsPageAction.SubmitAgentPrompt("I have 10, what should I buy?"))

        assertEquals(answer, state.agentPendingCommand)
        assertTrue(state.showAiAgentDialog)
    }

    @Test
    fun `the prompt is sent with the current groups`() {
        coEvery { groupAgentRepository.handlePrompt(any(), any(), any()) } returns listOf(
            GroupAgentCommand.Unresolved("nope")
        )

        viewModel.handleAction(GroupsPageAction.SubmitAgentPrompt("hello"))

        coVerify { groupAgentRepository.handlePrompt("hello", state.groups, null) }
    }

    @Test
    fun `an unresolved prompt shows its message`() {
        coEvery { groupAgentRepository.handlePrompt(any(), any(), any()) } returns listOf(
            GroupAgentCommand.Unresolved("The AI assistant is busy")
        )

        viewModel.handleAction(GroupsPageAction.ShowAiAgentDialog)
        viewModel.handleAction(GroupsPageAction.SubmitAgentPrompt("create Party"))

        assertEquals("The AI assistant is busy", state.agentError)
        assertFalse(state.isAgentProcessing)
    }

    @Test
    fun `a blank prompt is ignored`() {
        viewModel.handleAction(GroupsPageAction.SubmitAgentPrompt("   "))
        coVerify(exactly = 0) { groupAgentRepository.handlePrompt(any(), any(), any()) }
    }

    @Test
    fun `a failing agent step stops the batch with an error instead of crashing`() {
        coEvery { groupRepository.upsertGroup(any()) } throws permissionDenied()
        coEvery { groupAgentRepository.handlePrompt(any(), any(), any()) } returns listOf(
            GroupAgentCommand.CreateGroup(UIGroup(name = "Party")),
            GroupAgentCommand.CreateGroup(UIGroup(name = "Picnic"))
        )

        viewModel.handleAction(GroupsPageAction.ShowAiAgentDialog)
        viewModel.handleAction(GroupsPageAction.SubmitAgentPrompt("create Party and Picnic"))

        assertEquals(stringRes(R.string.something_went_wrong), state.agentError)
        assertTrue(state.agentCommandQueue.isEmpty())
        coVerify(exactly = 1) { groupRepository.upsertGroup(any()) }
    }

    @Test
    fun `the agent cannot create a duplicate group`() {
        coEvery { groupAgentRepository.handlePrompt(any(), any(), any()) } returns listOf(
            GroupAgentCommand.CreateGroup(UIGroup(name = " groceries"))
        )

        viewModel.handleAction(GroupsPageAction.ShowAiAgentDialog)
        viewModel.handleAction(GroupsPageAction.SubmitAgentPrompt("create groceries"))

        assertEquals(stringRes(R.string.group_already_exists), state.agentError)
        coVerify(exactly = 0) { groupRepository.upsertGroup(any()) }
    }

    @Test
    fun `the agent renames shared groups through the shared document`() {
        coEvery { groupAgentRepository.handlePrompt(any(), any(), any()) } returns listOf(
            GroupAgentCommand.RenameGroup(sharedOwned, "Shop")
        )

        viewModel.handleAction(GroupsPageAction.ShowAiAgentDialog)
        viewModel.handleAction(GroupsPageAction.SubmitAgentPrompt("rename"))

        coVerify { sharedGroupRepository.renameSharedGroup("shared-mine", "Shop") }
        coVerify(exactly = 0) { groupRepository.upsertGroup(any()) }
    }

    @Test
    fun `the agent may not rename someone else's shared group`() {
        coEvery { groupAgentRepository.handlePrompt(any(), any(), any()) } returns listOf(
            GroupAgentCommand.RenameGroup(sharedForeign, "Mine")
        )

        viewModel.handleAction(GroupsPageAction.ShowAiAgentDialog)
        viewModel.handleAction(GroupsPageAction.SubmitAgentPrompt("rename"))

        assertEquals(stringRes(R.string.agent_permission_denied_rename), state.agentError)
        coVerify(exactly = 0) { sharedGroupRepository.renameSharedGroup(any(), any()) }
    }

    private fun permissionDenied() = FirebaseFirestoreException(
        "PERMISSION_DENIED: Missing or insufficient permissions.",
        FirebaseFirestoreException.Code.PERMISSION_DENIED
    )
}
