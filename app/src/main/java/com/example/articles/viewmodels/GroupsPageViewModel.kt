package com.example.articles.viewmodels

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.ArticlesApplication
import com.example.articles.R
import com.example.articles.ai.GroupAgentCommand
import com.example.articles.ai.GroupAgentRepository
import com.example.articles.models.UIGroup
import com.example.articles.models.UserPreferences
import com.example.articles.repositories.ArticleRepository
import com.example.articles.repositories.CommentRepository
import com.example.articles.repositories.GroupMember
import com.example.articles.repositories.GroupRepository
import com.example.articles.repositories.InvitationRepository
import com.example.articles.repositories.SharedGroupRepository
import com.example.articles.utils.CurrentLocationProvider
import com.example.articles.utils.PositionResolver
import com.example.articles.utils.TriggerSync
import com.example.articles.utils.launchCatching
import com.example.articles.utils.normalizeEmail
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GroupsPageViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val articleRepository: ArticleRepository,
    private val commentRepository: CommentRepository,
    private val invitationRepository: InvitationRepository,
    private val sharedGroupRepository: SharedGroupRepository,
    private val groupAgentRepository: GroupAgentRepository,
    private val currentLocationProvider: CurrentLocationProvider,
    private val userPreferences: UserPreferences,
    private val triggerSync: TriggerSync,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val state = MutableStateFlow(value = GroupsPageState())
    val uiState = state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                flow = groupRepository.getActiveGroups(),
                flow2 = articleRepository.getAllArticles()
            ) { groups, articles ->
                groups.map { group ->
                    group.copy(articles = articles.filter { article -> article.groupId == group.id })
                }
            }.collectLatest { groupsWithArticles ->
                state.update { currentState ->
                    currentState.copy(groups = groupsWithArticles)
                }
                val currentUid = Firebase.auth.currentUser?.uid
                groupsWithArticles
                    .filter { it.isShared && it.sharedGroupId != null && it.isGroupOwner(currentUid) }
                    .mapNotNull { it.sharedGroupId }
                    .distinct()
                    .forEach { sharedGroupId -> refreshGroupMembers(sharedGroupId) }
            }
        }
        viewModelScope.launch {
            val uid = Firebase.auth.currentUser?.uid ?: return@launch
            invitationRepository.observePendingInvitations(uid).collectLatest { invitations ->
                state.update { currentState ->
                    currentState.copy(pendingInvitationsCount = invitations.size)
                }
            }
        }
        state.update { currentState ->
            currentState.copy(currentUid = Firebase.auth.currentUser?.uid)
        }
    }

    fun handleAction(action: GroupsPageAction) {
        when (action) {
            is GroupsPageAction.ShowAddNewGroupDialog -> updateAddNewGroupDialogVisibility(visible = true)
            is GroupsPageAction.HideAddNewGroupDialog -> updateAddNewGroupDialogVisibility(visible = false)
            is GroupsPageAction.SaveGroup -> saveGroup(name = action.name)
            is GroupsPageAction.ShowEditGroupDialog -> updateEditGroupDialogVisibility(visible = true, group = action.group)
            is GroupsPageAction.HideEditGroupDialog -> updateEditGroupDialogVisibility(visible = false, group = null)
            is GroupsPageAction.EditGroup -> editGroup(group = action.group, name = action.name)
            is GroupsPageAction.DeleteGroup -> deleteGroup(group = action.group)
            is GroupsPageAction.ArchiveGroup -> archiveGroup(group = action.group)
            is GroupsPageAction.ReorderGroup -> reorderGroup(
                groupId = action.groupId,
                orderedGroupIds = action.orderedGroupIds
            )
            is GroupsPageAction.ConsumeArchivedGroupEvent -> consumeArchivedGroupEvent()
            is GroupsPageAction.LongClick -> updateEditDeleteBottomSheetVisibility(visible = true, group = action.group)
            is GroupsPageAction.DismissBottomSheet -> updateEditDeleteBottomSheetVisibility(visible = false, group = null)
            is GroupsPageAction.ExpandGroup -> updateGroupExpandedStatus(group = action.group, status = true)
            is GroupsPageAction.CollapseGroup -> updateGroupExpandedStatus(group = action.group, status = false)
            is GroupsPageAction.ShowShareGroupDialog -> updateShareGroupDialogVisibility(visible = true, group = action.group)
            is GroupsPageAction.HideShareGroupDialog -> updateShareGroupDialogVisibility(visible = false, group = null)
            is GroupsPageAction.ShareGroup -> shareGroup(group = state.value.longPressedGroup ?: UIGroup(), email = action.email)
            is GroupsPageAction.SelectRecentShareRecipient -> selectRecentShareRecipient(email = action.email)
            is GroupsPageAction.ShowGroupMembersDialog -> showGroupMembersDialog(group = action.group)
            is GroupsPageAction.HideGroupMembersDialog -> hideGroupMembersDialog()
            is GroupsPageAction.RemoveGroupMember -> removeGroupMember(member = action.member)
            is GroupsPageAction.ConsumeInvitationSentEvent -> consumeInvitationSentEvent()
            is GroupsPageAction.ShowAiAgentDialog -> updateAiAgentDialogVisibility(visible = true)
            is GroupsPageAction.HideAiAgentDialog -> updateAiAgentDialogVisibility(visible = false)
            is GroupsPageAction.SubmitAgentPrompt -> submitAgentPrompt(text = action.text)
            is GroupsPageAction.ConfirmPendingAgentCommand -> confirmPendingAgentCommand()
            is GroupsPageAction.CancelPendingAgentCommand -> cancelPendingAgentCommand()
        }
    }

    private fun updateAddNewGroupDialogVisibility(visible: Boolean) {
        state.update { currentState ->
            currentState.copy(
                showAddNewGroupDialog = visible,
                groupToCreateName = TextFieldState(),
            )
        }
    }

    private fun isGroupNameTaken(name: String, exceptGroupId: UUID? = null) =
        state.value.groups.any { it.id != exceptGroupId && it.name.trim().equals(name.trim(), ignoreCase = true) }

    private fun saveGroup(name: String) {
        val trimmedName = name.trim()
        val errorResource = when {
            trimmedName.isEmpty() -> R.string.group_name_blank
            isGroupNameTaken(trimmedName) -> R.string.group_already_exists
            else -> null
        }
        if (errorResource != null) {
            state.update { currentState ->
                currentState.copy(
                    isCreateGroupDialogLoading = false,
                    isGroupCreationError = true,
                    groupCreationErrorResource = errorResource
                )
            }
            return
        }
        state.update { currentState ->
            currentState.copy(
                isCreateGroupDialogLoading = true,
                isGroupCreationError = false,
                isShareGroupError = false,
                groupCreationErrorResource = null
            )
        }
        launchCatching(
            operation = "saveGroup",
            onError = { state.update { it.copy(isCreateGroupDialogLoading = false) } }
        ) {
            val position = groupRepository.getNextGroupPosition()
            groupRepository.upsertGroup(group = UIGroup(name = trimmedName, position = position))
            state.update { currentState ->
                currentState.copy(
                    isCreateGroupDialogLoading = false,
                    groupToCreateName = TextFieldState(),
                    showAddNewGroupDialog = false,
                    isGroupCreationError = false,
                )
            }
            triggerSync()
        }
    }

    private fun updateEditGroupDialogVisibility(visible: Boolean, group: UIGroup?) {
        state.update { currentState ->
            currentState.copy(
                showEditDeleteBottonSheet = false,
                showEditGroupDialog = visible,
                longPressedGroup = group,
                groupToEditName =
                    if (group != null) TextFieldState(group.name)
                    else TextFieldState()
            )
        }
    }

    private fun editGroup(group: UIGroup, name: String) {
        val trimmedName = name.trim()
        val isUnchanged = trimmedName == group.name
        val isNotAllowed = group.isShared && !group.isGroupOwner(Firebase.auth.currentUser?.uid)
        if (isUnchanged || isNotAllowed) {
            state.update { currentState ->
                currentState.copy(
                    showEditGroupDialog = false,
                )
            }
            return
        }
        if (trimmedName.isEmpty() || isGroupNameTaken(trimmedName, exceptGroupId = group.id)) return
        state.update { currentState ->
            currentState.copy(
                isEditDialogLoading = true,
            )
        }
        launchCatching(
            operation = "editGroup",
            onError = { state.update { it.copy(isEditDialogLoading = false) } }
        ) {
            renameGroup(group = group, newName = trimmedName)
            state.update { currentState ->
                currentState.copy(
                    isEditDialogLoading = false,
                    groupToCreateName = TextFieldState(),
                    showEditGroupDialog = false,
                )
            }
            triggerSync()
        }
    }

    private suspend fun renameGroup(group: UIGroup, newName: String) {
        val sharedGroupId = group.sharedGroupId
        if (group.isShared && sharedGroupId != null) {
            sharedGroupRepository.renameSharedGroup(sharedGroupId, newName)
            groupRepository.upsertGroupLocal(group.copy(name = newName))
        } else {
            groupRepository.upsertGroup(group = group.copy(name = newName))
        }
    }

    private fun deleteGroup(group: UIGroup) {
        state.update { it.copy(showEditDeleteBottonSheet = false) }
        launchCatching(operation = "deleteGroup") {
            if (group.isShared && group.sharedGroupId != null) {
                val uid = Firebase.auth.currentUser?.uid ?: return@launchCatching
                if (group.isGroupOwner(uid)) {
                    sharedGroupRepository.deleteSharedGroup(group.sharedGroupId)
                } else {
                    sharedGroupRepository.leaveSharedGroup(group.sharedGroupId, uid)
                }
                articleRepository.hardDeleteLocalArticlesByGroupId(group.id)
                commentRepository.hardDeleteLocalCommentsByGroupId(group.id)
                groupRepository.deleteGroupLocalById(group.id)
            } else {
                articleRepository.deleteArticlesByGroupId(group.id)
                commentRepository.deleteCommentsByGroupId(group.id)
                groupRepository.deleteGroupById(group.id)
            }
        }
    }

    private fun archiveGroup(group: UIGroup) {
        state.update { it.copy(showEditDeleteBottonSheet = false) }
        if (!group.isGroupOwner(Firebase.auth.currentUser?.uid)) return
        launchCatching(operation = "archiveGroup") {
            articleRepository.resetBoughtArticles(group.id)
            val archived = group.copy(isArchived = true, position = null)
            val sharedGroupId = group.sharedGroupId
            if (group.isShared && sharedGroupId != null) {
                sharedGroupRepository.setSharedGroupArchived(sharedGroupId, archived = true)
                groupRepository.upsertSharedGroupLocal(archived)
            } else {
                groupRepository.upsertGroup(group = archived)
            }
            triggerSync()
            state.update { it.copy(archivedGroupEvent = group.name) }
        }
    }

    private fun consumeArchivedGroupEvent() = state.update { currentState ->
        currentState.copy(archivedGroupEvent = null)
    }

    private fun reorderGroup(groupId: UUID, orderedGroupIds: List<UUID>) {
        launchCatching(operation = "reorderGroup") {
            val index = orderedGroupIds.indexOf(groupId)
            if (index == -1) return@launchCatching
            val positionById = state.value.groups.associate { it.id to it.position }
            val previousPosition = orderedGroupIds.getOrNull(index - 1)?.let { positionById[it] }
            val nextPosition = orderedGroupIds.getOrNull(index + 1)?.let { positionById[it] }
            val newPosition = PositionResolver.resolve(previousPosition, nextPosition)
            if (newPosition == null) {
                groupRepository.renumberGroups(orderedGroupIds)
            } else {
                groupRepository.updateGroupPosition(groupId, newPosition)
            }
        }
    }

    private fun updateEditDeleteBottomSheetVisibility(visible: Boolean, group: UIGroup?) {
        state.update { currentState ->
            currentState.copy(
                showEditDeleteBottonSheet = visible,
                longPressedGroup = group
            )
        }
    }

    private fun updateGroupExpandedStatus(group: UIGroup, status: Boolean) {
        state.update { currentState ->
            currentState.copy(
                groups = currentState.groups.map { if (it.id == group.id) it.copy(expanded = status) else it }
            )
        }
    }

    private fun updateShareGroupDialogVisibility(visible: Boolean, group: UIGroup?) {
        state.update { currentState ->
            currentState.copy(
                showShareGroupDialog = visible,
                shareGroupEmail = TextFieldState(),
                showEditDeleteBottonSheet = false,
                recentShareRecipients = emptyList()
            )
        }
        if (!visible) return
        val uid = Firebase.auth.currentUser?.uid ?: return
        launchCatching(operation = "getRecentShareRecipients") {
            val recents = invitationRepository.getRecentShareRecipients(uid)
            state.update { it.copy(recentShareRecipients = recents) }
        }
    }

    private fun showGroupMembersDialog(group: UIGroup) {
        state.update { currentState ->
            currentState.copy(
                showGroupMembersDialog = true,
                showEditDeleteBottonSheet = false,
                longPressedGroup = group
            )
        }
        group.sharedGroupId?.let { refreshGroupMembers(it) }
    }

    private fun refreshGroupMembers(sharedGroupId: String) {
        if (sharedGroupId in state.value.loadingGroupMembersFor) return
        state.update { it.copy(loadingGroupMembersFor = it.loadingGroupMembersFor + sharedGroupId) }
        launchCatching(
            operation = "refreshGroupMembers",
            onError = { state.update { it.copy(loadingGroupMembersFor = it.loadingGroupMembersFor - sharedGroupId) } }
        ) {
            val members = sharedGroupRepository.getMembers(sharedGroupId)
            state.update { currentState ->
                currentState.copy(
                    groupMembers = currentState.groupMembers + (sharedGroupId to members),
                    loadingGroupMembersFor = currentState.loadingGroupMembersFor - sharedGroupId
                )
            }
        }
    }

    private fun removeGroupMember(member: GroupMember) {
        val sharedGroupId = state.value.longPressedGroup?.sharedGroupId ?: return
        state.update { currentState ->
            currentState.copy(
                groupMembers = currentState.groupMembers +
                    (sharedGroupId to currentState.groupMembers[sharedGroupId].orEmpty().filter { it.uid != member.uid })
            )
        }
        launchCatching(operation = "removeGroupMember") {
            sharedGroupRepository.removeMember(sharedGroupId, member.uid)
        }
    }

    private fun selectRecentShareRecipient(email: String) {
        state.update { currentState ->
            currentState.copy(
                shareGroupEmail = TextFieldState(email),
                isShareGroupError = false
            )
        }
    }

    fun hideGroupMembersDialog() = state.update { currentState ->
        currentState.copy(showGroupMembersDialog = false)
    }

    private fun consumeInvitationSentEvent() = state.update { currentState ->
        currentState.copy(invitationSentEvent = false)
    }

    private fun shareGroup(group: UIGroup, email: String) {
        val currentUser = Firebase.auth.currentUser ?: return
        state.update { currentState ->
            currentState.copy(
                isShareGroupLoading = true,
                shareGroupEmail = TextFieldState(),
                isShareGroupError = false,
            )
        }
        fun showShareError(@StringRes errorResource: Int) = state.update { currentState ->
            currentState.copy(
                isShareGroupLoading = false,
                groupSharingErrorResource = errorResource,
                isShareGroupError = true,
            )
        }
        val normalizedEmail = email.normalizeEmail()
        if (normalizedEmail == currentUser.email?.normalizeEmail()) {
            showShareError(R.string.cannot_share_with_yourself)
            return
        }
        viewModelScope.launch {
            try {
                val toUid = invitationRepository.lookupUidByEmail(normalizedEmail)
                if (toUid == null) {
                    showShareError(R.string.user_not_found)
                    return@launch
                }
                if (toUid == currentUser.uid) {
                    showShareError(R.string.cannot_share_with_yourself)
                    return@launch
                }
                val sharedGroupId = group.sharedGroupId
                if (group.isShared && sharedGroupId != null &&
                    sharedGroupRepository.getMembers(sharedGroupId).any { it.uid == toUid }
                ) {
                    showShareError(R.string.user_already_member)
                    return@launch
                }
                invitationRepository.sendInvitation(
                    fromUid = currentUser.uid,
                    fromEmail = currentUser.email ?: "",
                    toEmail = normalizedEmail,
                    toUid = toUid,
                    group = group
                )
                state.update { currentState ->
                    currentState.copy(
                        isShareGroupLoading = false,
                        showShareGroupDialog = false,
                        shareGroupEmail = TextFieldState(),
                        isShareGroupError = false,
                        invitationSentEvent = true,
                    )
                }
            } catch (e: Exception) {
                Log.e(ArticlesApplication.LOG_TAG, "shareGroup failed: ${e.message}", e)
                state.update { currentState ->
                    currentState.copy(
                        isShareGroupLoading = false,
                        groupSharingErrorResource = R.string.something_went_wrong,
                        isShareGroupError = true,
                    )
                }
            }
        }
    }

    private fun updateAiAgentDialogVisibility(visible: Boolean) {
        state.update { currentState ->
            currentState.copy(
                showAiAgentDialog = visible,
                agentPromptText = TextFieldState(),
                agentError = null,
                agentPendingCommand = null,
                agentCommandQueue = emptyList(),
                isAgentProcessing = false
            )
        }
    }

    private fun submitAgentPrompt(text: String) {
        if (text.isBlank()) return
        state.update { currentState -> currentState.copy(isAgentProcessing = true, agentError = null) }
        viewModelScope.launch {
            val location = currentLocationProvider.getCurrentLocation()
            val commands = groupAgentRepository.handlePrompt(
                promptText = text,
                groups = state.value.groups,
                location = location
            )
            state.update { currentState -> currentState.copy(isAgentProcessing = false) }
            if (commands.all { it is GroupAgentCommand.Unresolved }) {
                val message = (commands.firstOrNull() as? GroupAgentCommand.Unresolved)?.message
                    ?: context.getString(R.string.agent_could_not_understand)
                state.update { currentState -> currentState.copy(agentError = message) }
                return@launch
            }
            state.update { currentState -> currentState.copy(agentCommandQueue = commands) }
            processNextQueuedAgentCommand()
        }
    }

    private fun processNextQueuedAgentCommand() {
        val queue = state.value.agentCommandQueue
        val next = queue.firstOrNull()
        if (next == null) {
            closeAiAgentDialog()
            return
        }
        state.update { currentState -> currentState.copy(agentCommandQueue = queue.drop(1)) }

        if (next is GroupAgentCommand.Unresolved) {
            processNextQueuedAgentCommand()
            return
        }

        val needsConfirmation = when (next) {
            is GroupAgentCommand.DeleteGroup, is GroupAgentCommand.RemoveArticle -> true
            is GroupAgentCommand.CreateGroup,
            is GroupAgentCommand.RenameGroup,
            is GroupAgentCommand.AddArticle,
            is GroupAgentCommand.RenameArticle -> userPreferences.confirmNonDestructiveAgentActions
            is GroupAgentCommand.AnswerQuestion -> true
            is GroupAgentCommand.Unresolved -> false
        }
        if (needsConfirmation) {
            state.update { currentState -> currentState.copy(agentPendingCommand = next) }
        } else {
            executeAgentCommand(next)
        }
    }

    private fun confirmPendingAgentCommand() {
        val command = state.value.agentPendingCommand ?: return
        state.update { currentState -> currentState.copy(agentPendingCommand = null) }
        executeAgentCommand(command)
    }

    private fun cancelPendingAgentCommand() {
        state.update { currentState -> currentState.copy(agentPendingCommand = null, agentCommandQueue = emptyList()) }
    }

    private fun failAgentBatch(message: String) {
        state.update { currentState ->
            currentState.copy(agentError = message, agentCommandQueue = emptyList())
        }
    }

    private fun runAgentStep(operation: String, block: suspend () -> Unit) {
        launchCatching(
            operation = operation,
            onError = { failAgentBatch(context.getString(R.string.something_went_wrong)) }
        ) {
            block()
            triggerSync()
            processNextQueuedAgentCommand()
        }
    }

    private fun executeAgentCommand(command: GroupAgentCommand) {
        when (command) {
            is GroupAgentCommand.CreateGroup -> {
                val name = command.group.name.trim()
                if (name.isEmpty() || isGroupNameTaken(name)) {
                    failAgentBatch(context.getString(R.string.group_already_exists))
                    return
                }
                runAgentStep("agentCreateGroup") {
                    val position = groupRepository.getNextGroupPosition()
                    groupRepository.upsertGroup(group = command.group.copy(name = name, position = position))
                }
            }

            is GroupAgentCommand.RenameGroup -> {
                val group = command.group
                val newName = command.newName.trim()
                if (isGroupNameTaken(newName, exceptGroupId = group.id)) {
                    failAgentBatch(context.getString(R.string.group_already_exists))
                    return
                }
                if (group.isShared) {
                    val uid = Firebase.auth.currentUser?.uid
                    if (!group.isGroupOwner(uid)) {
                        failAgentBatch(context.getString(R.string.agent_permission_denied_rename))
                        return
                    }
                    if (group.sharedGroupId == null) {
                        failAgentBatch(context.getString(R.string.something_went_wrong))
                        return
                    }
                }
                runAgentStep("agentRenameGroup") { renameGroup(group = group, newName = newName) }
            }

            is GroupAgentCommand.DeleteGroup -> {
                deleteGroup(group = command.group)
                processNextQueuedAgentCommand()
            }

            is GroupAgentCommand.AddArticle -> runAgentStep("agentAddArticle") {
                val position = articleRepository.getNextArticlePosition(command.article.groupId)
                articleRepository.upsertArticle(article = command.article.copy(position = position))
            }

            is GroupAgentCommand.RenameArticle -> runAgentStep("agentRenameArticle") {
                articleRepository.upsertArticle(article = command.article.copy(name = command.newName))
            }

            is GroupAgentCommand.RemoveArticle -> runAgentStep("agentRemoveArticle") {
                articleRepository.deleteArticleById(articleId = command.article.id)
                commentRepository.deleteCommentsByArticleId(
                    groupId = command.article.groupId,
                    articleId = command.article.id
                )
            }

            is GroupAgentCommand.AnswerQuestion -> processNextQueuedAgentCommand()

            is GroupAgentCommand.Unresolved -> processNextQueuedAgentCommand()
        }
    }

    private fun closeAiAgentDialog() {
        state.update { currentState ->
            currentState.copy(
                showAiAgentDialog = false,
                agentPromptText = TextFieldState(),
                agentPendingCommand = null,
                agentCommandQueue = emptyList(),
                agentError = null
            )
        }
    }

}

data class GroupsPageState(
    val groups: List<UIGroup> = emptyList(),
    val groupToEditName: TextFieldState = TextFieldState(),
    val shareGroupEmail: TextFieldState = TextFieldState(),
    val groupToCreateName: TextFieldState = TextFieldState(),
    val showEditGroupDialog: Boolean = false,
    val showShareGroupDialog: Boolean = false,
    val showAddNewGroupDialog: Boolean = false,
    val showEditDeleteBottonSheet: Boolean = false,
    val isShareGroupError: Boolean = false,
    val isShareGroupLoading: Boolean = false,
    val isEditDialogLoading: Boolean = false,
    val isGroupCreationError: Boolean = false,
    val isCreateGroupDialogLoading: Boolean = false,
    val longPressedGroup: UIGroup? = null,
    val pendingInvitationsCount: Int = 0,
    val invitationSentEvent: Boolean = false,
    val archivedGroupEvent: String? = null,
    val recentShareRecipients: List<String> = emptyList(),
    val currentUid: String? = null,
    val showGroupMembersDialog: Boolean = false,
    val groupMembers: Map<String, List<GroupMember>> = emptyMap(),
    val loadingGroupMembersFor: Set<String> = emptySet(),
    @StringRes val groupSharingErrorResource: Int? = null,
    @StringRes val groupCreationErrorResource: Int? = null,
    val showAiAgentDialog: Boolean = false,
    val agentPromptText: TextFieldState = TextFieldState(),
    val isAgentProcessing: Boolean = false,
    val agentError: String? = null,
    val agentPendingCommand: GroupAgentCommand? = null,
    val agentCommandQueue: List<GroupAgentCommand> = emptyList(),
)

sealed interface GroupsPageAction {
    data object ShowAddNewGroupDialog : GroupsPageAction
    data object HideAddNewGroupDialog : GroupsPageAction
    data class SaveGroup(val name: String) : GroupsPageAction
    data class ShowEditGroupDialog(val group: UIGroup) : GroupsPageAction
    data object HideEditGroupDialog : GroupsPageAction
    data class EditGroup(val group: UIGroup, val name: String) : GroupsPageAction
    data class DeleteGroup(val group: UIGroup) : GroupsPageAction
    data class ArchiveGroup(val group: UIGroup) : GroupsPageAction
    data object ConsumeArchivedGroupEvent : GroupsPageAction
    data class ReorderGroup(
        val groupId: UUID,
        val orderedGroupIds: List<UUID>
    ) : GroupsPageAction
    data class LongClick(val group: UIGroup) : GroupsPageAction
    data object DismissBottomSheet : GroupsPageAction
    data class ExpandGroup(val group: UIGroup) : GroupsPageAction
    data class CollapseGroup(val group: UIGroup) : GroupsPageAction
    data class ShowShareGroupDialog(val group: UIGroup) : GroupsPageAction
    data object HideShareGroupDialog : GroupsPageAction
    data class ShareGroup(val email: String) : GroupsPageAction
    data class SelectRecentShareRecipient(val email: String) : GroupsPageAction
    data class ShowGroupMembersDialog(val group: UIGroup) : GroupsPageAction
    data object HideGroupMembersDialog : GroupsPageAction
    data class RemoveGroupMember(val member: GroupMember) : GroupsPageAction
    data object ConsumeInvitationSentEvent : GroupsPageAction
    data object ShowAiAgentDialog : GroupsPageAction
    data object HideAiAgentDialog : GroupsPageAction
    data class SubmitAgentPrompt(val text: String) : GroupsPageAction
    data object ConfirmPendingAgentCommand : GroupsPageAction
    data object CancelPendingAgentCommand : GroupsPageAction
}