package com.example.articles.viewmodels

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.models.UIGroup
import com.example.articles.repositories.ArticleRepository
import com.example.articles.repositories.GroupRepository
import com.example.articles.repositories.InvitationRepository
import com.example.articles.utils.TriggerSync
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupsPageViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val articleRepository: ArticleRepository,
    private val invitationRepository: InvitationRepository,
    private val triggerSync: TriggerSync
) : ViewModel() {

    private val state = MutableStateFlow(GroupsPageState())
    val uiState = state

    init {
        viewModelScope.launch {
            combine(
                groupRepository.getAllGroups(),
                articleRepository.getAllArticles()
            ) { groups, articles ->
                groups.map { group ->
                    group.copy(
                        articles = articles.filter { article -> article.groupId == group.id }
                    )
                }
            }.collectLatest { groupsWithArticles ->
                state.update { currentState ->
                    currentState.copy(
                        groups = groupsWithArticles
                    )
                }
            }
        }
        viewModelScope.launch {
            val uid = Firebase.auth.currentUser?.uid ?: return@launch
            invitationRepository.observePendingInvitations(uid).collectLatest { invitations ->
                state.update { it.copy(pendingInvitationsCount = invitations.size) }
            }
        }
    }

    fun handleAction(action: GroupsPageAction) {
        when (action) {
            is GroupsPageAction.ShowAddNewGroupDialog ->
                updateAddNewGroupDialogVisibility(visible = true)
            is GroupsPageAction.HideAddNewGroupDialog ->
                updateAddNewGroupDialogVisibility(visible = false)
            is GroupsPageAction.SaveGroup ->
                saveGroup(name = action.name)
            is GroupsPageAction.ShowEditGroupDialog ->
                updateEditGroupDialogVisibility(visible = true, group = action.group)
            is GroupsPageAction.HideEditGroupDialog ->
                updateEditGroupDialogVisibility(visible = false, group = null)
            is GroupsPageAction.EditGroup ->
                editGroup(group = action.group, name = action.name)
            is GroupsPageAction.DeleteGroup ->
                deleteGroup(group = action.group)
            is GroupsPageAction.LongClick ->
                updateEditDeleteBottomSheetVisibility(visible = true, group = action.group)
            is GroupsPageAction.DismissBottomSheet ->
                updateEditDeleteBottomSheetVisibility(visible = false, group = null)
            is GroupsPageAction.ExpandGroup -> updateGroupExpandedStatus(action.group, status = true)
            is GroupsPageAction.CollapseGroup -> updateGroupExpandedStatus(action.group, status = false)
            is GroupsPageAction.ShowShareGroupDialog ->
                updateShareGroupDialogVisibility(visible = true, group = action.group)
            is GroupsPageAction.HideShareGroupDialog ->
                updateShareGroupDialogVisibility(visible = false, group = null)
            is GroupsPageAction.ShareGroup ->
                shareGroup(group = action.group, email = action.email)
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
            )
        }
        if (name.isEmpty() || name.uppercase() in state.value.groups.map { it.name.uppercase() }) {
            state.update { currentState ->
                currentState.copy(
                    isCreateGroupDialogLoading = false,
                )
            }
            return
        } else {
            viewModelScope.launch {
                groupRepository.upsertGroup(
                    group = UIGroup(name = name)
                )
                state.update { currentState ->
                    currentState.copy(
                        isCreateGroupDialogLoading = false,
                        groupToCreateName = TextFieldState(),
                        showAddNewGroupDialog = false,
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
        state.update { currentState ->
            currentState.copy(
                showEditDeleteBottonSheet = false,
            )
        }
        viewModelScope.launch {
            groupRepository.deleteGroupById(
                groupId = group.id,
            )
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
                shareGroupError = null,
                groupToShare = group,
                showEditDeleteBottonSheet = false
            )
        }
    }

    private fun shareGroup(group: UIGroup, email: String) {
        val currentUser = Firebase.auth.currentUser ?: return
        state.update { it.copy(isShareGroupLoading = true, shareGroupError = null) }
        viewModelScope.launch {
            val toUid = invitationRepository.lookupUidByEmail(email)
            if (toUid == null) {
                state.update { it.copy(isShareGroupLoading = false, shareGroupError = "user_not_found") }
                return@launch
            }
            invitationRepository.sendInvitation(
                fromUid = currentUser.uid,
                fromEmail = currentUser.email ?: "",
                toEmail = email,
                toUid = toUid,
                group = group
            )
            state.update { it.copy(isShareGroupLoading = false, showShareGroupDialog = false) }
        }
    }

}

data class GroupsPageState(
    val groups: List<UIGroup> = emptyList(),
    val showAddNewGroupDialog: Boolean = false,
    val groupToCreateName: TextFieldState = TextFieldState(),
    val isCreateGroupDialogLoading: Boolean = false,
    val showEditGroupDialog: Boolean = false,
    val groupToEditName: TextFieldState = TextFieldState(),
    val longPressedGroup: UIGroup? = null,
    val isEditDialogLoading: Boolean = false,
    val showEditDeleteBottonSheet: Boolean = false,
    val showShareGroupDialog: Boolean = false,
    val shareGroupEmail: TextFieldState = TextFieldState(),
    val isShareGroupLoading: Boolean = false,
    val shareGroupError: String? = null,
    val groupToShare: UIGroup? = null,
    val pendingInvitationsCount: Int = 0,
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
    data class ShareGroup(val group: UIGroup, val email: String) : GroupsPageAction
}