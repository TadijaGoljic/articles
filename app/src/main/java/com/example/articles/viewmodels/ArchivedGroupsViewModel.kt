package com.example.articles.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.models.UIGroup
import com.example.articles.repositories.ArticleRepository
import com.example.articles.repositories.GroupRepository
import com.example.articles.repositories.SharedGroupRepository
import com.example.articles.utils.TriggerSync
import com.example.articles.utils.launchCatching
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
class ArchivedGroupsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val articleRepository: ArticleRepository,
    private val sharedGroupRepository: SharedGroupRepository,
    private val triggerSync: TriggerSync
) : ViewModel() {

    private val state = MutableStateFlow(value = ArchivedGroupsState())
    val uiState = state.asStateFlow()

    init {
        state.update { it.copy(currentUid = Firebase.auth.currentUser?.uid) }
        viewModelScope.launch {
            combine(
                flow = groupRepository.getArchivedGroups(),
                flow2 = articleRepository.getAllArticles()
            ) { groups, articles ->
                groups.map { group ->
                    group.copy(articles = articles.filter { article -> article.groupId == group.id })
                }
            }.collectLatest { groupsWithArticles ->
                state.update { currentState -> currentState.copy(groups = groupsWithArticles) }
            }
        }
    }

    fun handleAction(action: ArchivedGroupsAction) {
        when (action) {
            is ArchivedGroupsAction.UnarchiveGroup -> unarchiveGroup(group = action.group)
        }
    }

    private fun unarchiveGroup(group: UIGroup) {
        if (!group.isGroupOwner(Firebase.auth.currentUser?.uid)) return
        launchCatching(operation = "unarchiveGroup") {
            val position = groupRepository.getPositionForRestore()
            val restored = group.copy(isArchived = false, position = position)
            val sharedGroupId = group.sharedGroupId
            if (group.isShared && sharedGroupId != null) {
                sharedGroupRepository.setSharedGroupArchived(sharedGroupId, archived = false)
                groupRepository.upsertSharedGroupLocal(restored)
            } else {
                groupRepository.upsertGroup(group = restored)
            }
            triggerSync()
        }
    }

}

data class ArchivedGroupsState(
    val groups: List<UIGroup> = emptyList(),
    val currentUid: String? = null,
)

sealed interface ArchivedGroupsAction {
    data class UnarchiveGroup(val group: UIGroup) : ArchivedGroupsAction
}
