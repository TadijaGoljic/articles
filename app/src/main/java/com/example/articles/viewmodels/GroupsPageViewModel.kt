package com.example.articles.viewmodels

import android.content.Context
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.models.UIGroup
import com.example.articles.repositories.ArticleRepository
import com.example.articles.repositories.GroupRepository
import com.example.articles.utils.triggerSync
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context
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
            triggerSync(context = context)
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
                triggerSync(context = context)
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
}