package com.example.articles.viewmodels

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.ArticlesApplication
import com.example.articles.R
import com.example.articles.models.UIGroup
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
    private val triggerSync: TriggerSync
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
    val recentShareRecipients: List<String> = emptyList(),
    val currentUid: String? = null,
    val showGroupMembersDialog: Boolean = false,
    val groupMembers: Map<String, List<GroupMember>> = emptyMap(),
    val loadingGroupMembersFor: Set<String> = emptySet(),
    @StringRes val groupSharingErrorResource: Int? = null,
    @StringRes val groupCreationErrorResource: Int? = null,
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
}