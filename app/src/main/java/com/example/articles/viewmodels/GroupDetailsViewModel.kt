package com.example.articles.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.articles.models.UIArticle
import com.example.articles.models.UIGroup
import com.example.articles.repositories.ArticleRepository
import com.example.articles.repositories.GroupRepository
import com.example.articles.ui.composables.ArticlesDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GroupDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val articleRepository: ArticleRepository,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<ArticlesDestinations.GroupDetailsDestination>()
    private val groupId: UUID = UUID.fromString(args.groupId)

    private val state = MutableStateFlow(GroupDetailsState())
    val uiState = state

    init {
        viewModelScope.launch {
            combine(
                groupRepository.getGroupById(groupId),
                articleRepository.getArticlesByGroupId(groupId)
            ) { group, articles -> group to articles }
                .collect { (group, articles) ->
                    state.update { currentState ->
                        currentState.copy(
                            group = group,
                            articles = articles
                        )
                    }
                }
        }
    }

}

data class GroupDetailsState(
    val group: UIGroup = UIGroup(),
    val articles: List<UIArticle> = emptyList(),
)
