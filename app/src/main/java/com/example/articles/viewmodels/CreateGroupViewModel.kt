package com.example.articles.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.models.UIGroup
import com.example.articles.repositories.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    private var state = MutableStateFlow(GroupState())
    val uiState = state

    init {
        viewModelScope.launch {
            groupRepository.getAllGroupsWithArticles().collectLatest { groups ->
                state.update { currentState ->
                    currentState.copy(
                        groupNames = groups.map { it.group.name }
                    )
                }
            }
        }
    }

    fun handleAction(action: GroupAction) {
        when (action) {
            is GroupAction.UpdateName -> state.update { it.copy(name = action.name) }
            is GroupAction.CreateGroup -> createGroup()
        }
    }

    private fun createGroup() = viewModelScope.launch {
        state.update { it.copy(isLoading = true) }
        groupRepository.addNewGroup(UIGroup(name = state.value.name))
        state.update { it.copy(isLoading = false) }
    }

}

data class GroupState(
    val name: String = "",
    val groupNames: List<String> = emptyList(),
    val isLoading: Boolean = false,
)

sealed interface GroupAction {
    data class UpdateName(val name: String) : GroupAction
    data object CreateGroup : GroupAction
}
