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
import com.example.articles.utils.TriggerSync
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
import javax.inject.Inject

@HiltViewModel
class GroupsPageViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val articleRepository: ArticleRepository,
    private val commentRepository: CommentRepository,
    private val invitationRepository: InvitationRepository,
    private val sharedGroupRepository: SharedGroupRepository,
    private val groupAgentRepository: GroupAgentRepository,
    private val userPreferences: UserPreferences,
    private val triggerSync: TriggerSync,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val state = MutableStateFlow(value = GroupsPageState())
    val uiState = state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                flow = groupRepository.getAllGroups(),
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

    private fun saveGroup(name: String) {
        state.update { currentState ->
            currentState.copy(
                isCreateGroupDialogLoading = true,
                isGroupCreationError = false,
                isShareGroupError = false,
                groupCreationErrorResource = null
            )
        }
        if (name.isEmpty() || name.uppercase() in state.value.groups.map { it.name.uppercase() }) {
            state.update { currentState ->
                currentState.copy(
                    isCreateGroupDialogLoading = false,
                    isGroupCreationError = true,
                    groupCreationErrorResource = R.string.group_already_exists
                )
            }
            return
        } else {
            viewModelScope.launch {
                groupRepository.upsertGroup(group = UIGroup(name = name))
                state.update { currentState ->
                    currentState.copy(
                        isCreateGroupDialogLoading = false,
                        groupToCreateName = TextFieldState(),
                        showAddNewGroupDialog = false,
                        isGroupCreationError = false,
                    )
                }
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
        if (name.equals(group.name, ignoreCase = true)) {
            state.update { currentState ->
                currentState.copy(
                    showEditGroupDialog = false,
                )
            }
            return
        }
        state.update { currentState ->
            currentState.copy(
                isEditDialogLoading = true,
            )
        }
        if (name.isEmpty() || name.uppercase() in state.value.groups.map { it.name.uppercase() }) {
            state.update { currentState ->
                currentState.copy(
                    isEditDialogLoading = false,
                )
            }
            return
        } else {
            viewModelScope.launch {
                groupRepository.upsertGroup(
                    group = group.copy(name = name)
                )
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
    }

    private fun deleteGroup(group: UIGroup) {
        state.update { it.copy(showEditDeleteBottonSheet = false) }
        viewModelScope.launch {
            if (group.isShared && group.sharedGroupId != null) {
                val uid = Firebase.auth.currentUser?.uid ?: return@launch
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
            try {
                val toUid = invitationRepository.lookupUidByEmail(email)
                if (toUid == null) {
                    state.update { currentState ->
                        currentState.copy(
                            isShareGroupLoading = false,
                            groupSharingErrorResource = R.string.user_not_found,
                            isShareGroupError = true,
                        )
                    }
                    return@launch
                }
                invitationRepository.sendInvitation(
                    fromUid = currentUser.uid,
                    fromEmail = currentUser.email ?: "",
                    toEmail = email,
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
                isAgentProcessing = false
            )
        }
    }

    private fun submitAgentPrompt(text: String) {
        if (text.isBlank()) return
        state.update { currentState -> currentState.copy(isAgentProcessing = true, agentError = null) }
        viewModelScope.launch {
            val command = groupAgentRepository.parseCommand(promptText = text, groups = state.value.groups)
            state.update { currentState -> currentState.copy(isAgentProcessing = false) }
            when (command) {
                is GroupAgentCommand.Unresolved ->
                    state.update { currentState -> currentState.copy(agentError = command.message) }

                is GroupAgentCommand.DeleteGroup ->
                    state.update { currentState -> currentState.copy(agentPendingCommand = command) }

                is GroupAgentCommand.CreateGroup, is GroupAgentCommand.RenameGroup ->
                    if (userPreferences.confirmNonDestructiveAgentActions) {
                        state.update { currentState -> currentState.copy(agentPendingCommand = command) }
                    } else {
                        executeAgentCommand(command)
                    }
            }
        }
    }

    private fun confirmPendingAgentCommand() {
        val command = state.value.agentPendingCommand ?: return
        state.update { currentState -> currentState.copy(agentPendingCommand = null) }
        executeAgentCommand(command)
    }

    private fun cancelPendingAgentCommand() {
        state.update { currentState -> currentState.copy(agentPendingCommand = null) }
    }

    private fun executeAgentCommand(command: GroupAgentCommand) {
        when (command) {
            is GroupAgentCommand.CreateGroup -> {
                if (command.name.uppercase() in state.value.groups.map { it.name.uppercase() }) {
                    state.update { currentState ->
                        currentState.copy(agentError = context.getString(R.string.group_already_exists))
                    }
                    return
                }
                viewModelScope.launch {
                    groupRepository.upsertGroup(group = UIGroup(name = command.name))
                    triggerSync()
                    closeAiAgentDialog()
                }
            }

            is GroupAgentCommand.RenameGroup -> {
                val group = command.group
                val isDuplicate = !command.newName.equals(group.name, ignoreCase = true) &&
                    command.newName.uppercase() in state.value.groups.map { it.name.uppercase() }
                if (isDuplicate) {
                    state.update { currentState ->
                        currentState.copy(agentError = context.getString(R.string.group_already_exists))
                    }
                    return
                }
                if (group.isShared) {
                    val uid = Firebase.auth.currentUser?.uid
                    if (!group.isGroupOwner(uid)) {
                        state.update { currentState ->
                            currentState.copy(agentError = context.getString(R.string.agent_permission_denied_rename))
                        }
                        return
                    }
                    val sharedGroupId = group.sharedGroupId
                    if (sharedGroupId == null) {
                        state.update { currentState ->
                            currentState.copy(agentError = context.getString(R.string.something_went_wrong))
                        }
                        return
                    }
                    viewModelScope.launch {
                        sharedGroupRepository.renameSharedGroup(sharedGroupId, command.newName)
                        triggerSync()
                        closeAiAgentDialog()
                    }
                } else {
                    viewModelScope.launch {
                        groupRepository.upsertGroup(group = group.copy(name = command.newName))
                        triggerSync()
                        closeAiAgentDialog()
                    }
                }
            }

            is GroupAgentCommand.DeleteGroup -> {
                deleteGroup(group = command.group)
                closeAiAgentDialog()
            }

            is GroupAgentCommand.Unresolved -> Unit
        }
    }

    private fun closeAiAgentDialog() {
        state.update { currentState ->
            currentState.copy(
                showAiAgentDialog = false,
                agentPromptText = TextFieldState(),
                agentPendingCommand = null,
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
)

sealed interface GroupsPageAction {
    data object ShowAddNewGroupDialog : GroupsPageAction
    data object HideAddNewGroupDialog : GroupsPageAction
    data class SaveGroup(val name: String) : GroupsPageAction
    data class ShowEditGroupDialog(val group: UIGroup) : GroupsPageAction
    data object HideEditGroupDialog : GroupsPageAction
    data class EditGroup(val group: UIGroup, val name: String) : GroupsPageAction
    data class DeleteGroup(val group: UIGroup) : GroupsPageAction
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