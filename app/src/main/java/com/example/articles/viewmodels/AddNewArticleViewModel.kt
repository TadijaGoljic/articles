package com.example.articles.viewmodels

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import android.net.Uri
import android.util.Log
import com.example.articles.ArticlesApplication.Companion.LOG_TAG
import com.example.articles.models.ArticleUnit
import com.example.articles.models.UILocation
import com.example.articles.models.UIArticle
import com.example.articles.nominatim.NominatimAPI
import com.example.articles.repositories.ArticleRepository
import com.example.articles.utils.FileManager
import com.example.articles.ui.composables.ArticlesDestinations
import com.example.articles.utils.TriggerSync
import com.example.articles.utils.isValidUri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddNewArticleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val articleRepository: ArticleRepository,
    private val fileManager: FileManager,
    private val nominatimAPI: NominatimAPI,
    private val triggerSync: TriggerSync,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<ArticlesDestinations.AddNewArticleDestination>()
    private val groupId: UUID = UUID.fromString(args.groupId)

    private var state = MutableStateFlow(AddNewArticleState())
    val uiState = state.asStateFlow()

    init {
        args.articleId?.let { id ->
            viewModelScope.launch {
                articleRepository.getArticleById(UUID.fromString(id)).first()?.let { article ->
                    state.value.articleName.edit {
                        replace(0, length, article.name)
                    }
                    state.value.articleDescription.edit {
                        replace(0, length, article.description ?: "")
                    }
                    state.value.articleQuantity.edit {
                        replace(0, length, article.quantity?.toString() ?: "")
                    }
                    state.value.price.edit {
                        replace(0, length, article.price?.toString() ?: "")
                    }
                    state.value.articleWebsiteLink.edit {
                        replace(0, length, article.websiteLink ?: "")
                    }
                    state.update { currentState ->
                        currentState.copy(
                            article = article,
                            isEditMode = true
                        )
                    }
                    article.location?.let { updateLocation(it) }
                }
            }
        }
    }

    fun photoUri(): Uri = fileManager.photoUri()

    fun handleAction(action: AddNewArticleAction) {
        when (action) {
            is AddNewArticleAction.SaveArticle -> saveArticle()
            is AddNewArticleAction.UpdateLocation -> updateLocation(location = action.location)
            is AddNewArticleAction.UpdatePhoto -> updatePhoto(photoUri = action.photoUri)
            is AddNewArticleAction.ClearLocation -> updateLocation(null)
            is AddNewArticleAction.ClearPhoto -> updatePhoto(null)
            is AddNewArticleAction.ShowUnitsDropdownMenu -> updateUnitsDropdownMenuVisibility(visible = true)
            is AddNewArticleAction.HideUnitsDropdownMenu -> updateUnitsDropdownMenuVisibility(visible = false)
            is AddNewArticleAction.UpdateArticleUnit -> updateArticleUnit(unit = action.unit)
            is AddNewArticleAction.ShowBottomSheet -> updateBottomSheetVisibility(visible = true)
            is AddNewArticleAction.HideBottomSheet -> updateBottomSheetVisibility(visible = false)
        }
    }

    private fun saveArticle() {
        state.update { currentState ->
            currentState.copy(isLoading = true)
        }
        val articleName = state.value.articleName.text.toString()
        val articleDescription = state.value.articleDescription.text.toString()
        val articleQuantity = state.value.articleQuantity.text.toString().toDoubleOrNull()
        val articlePrice = state.value.price.text.toString().toDoubleOrNull()
        val articleWebsiteLink = state.value.articleWebsiteLink.text.toString()
        if (articleName.isBlank()) {
            state.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    articleNameError = true
                )
            }
            return
        }
        if (articleWebsiteLink.isNotBlank() && !articleWebsiteLink.isValidUri()) {
            state.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    articleWebsiteLinkError = true
                )
            }
            return
        }
        val article = state.value.article.copy(
            groupId = groupId,
            name = articleName,
            description = articleDescription,
            quantity = articleQuantity,
            price = articlePrice,
            websiteLink = articleWebsiteLink,
        )
        viewModelScope.launch {
            articleRepository.upsertArticle(article)
            triggerSync()
            state.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    okToSave = true,
                )
            }
        }
    }

    private fun updateLocation(location: UILocation?) {
        if (location == null) {
            state.update { currentState ->
                currentState.copy(
                    article = currentState.article.copy(location = location),
                    locationString = null
                )
            }
        } else {
            viewModelScope.launch {
                state.update { currentState ->
                    currentState.copy(
                        isLocationStringLoading = true
                    )
                }
                val locationString = nominatimAPI.reverseGeocode(
                    location.latitude,
                    location.longitude
                ).let {
                    Log.d(LOG_TAG, "updateLocation: $it")
                    val address = it.address
                    val street = listOfNotNull(address?.road, address?.houseNumber).joinToString(" ")
                    val town = address?.city ?: address?.cityDistrict ?: address?.suburb ?: address?.county
                    listOfNotNull(street.ifEmpty { null }, town).joinToString(", ")
                }
                state.update { currentState ->
                    currentState.copy(
                        article = currentState.article.copy(location = location),
                        locationString = locationString,
                        isLocationStringLoading = false
                    )
                }
            }
        }
    }

    private fun updatePhoto(photoUri: String?) {
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.copy(photoUri = photoUri)
            )
        }
    }

    private fun updateUnitsDropdownMenuVisibility(visible: Boolean) {
        state.update { currentState ->
            currentState.copy(unitsDropdownMenuVisible = visible)
        }
    }

    private fun updateArticleUnit(unit: ArticleUnit) {
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.copy(unit = unit)
            )
        }
    }

    private fun updateBottomSheetVisibility(visible: Boolean) {
        state.update { currentState ->
            currentState.copy(
                isBottomSheetVisible = visible
            )
        }
    }

}

data class AddNewArticleState(
    val article: UIArticle = UIArticle(),
    val locationString: String? = null,
    val articleName: TextFieldState = TextFieldState(),
    val articleDescription: TextFieldState = TextFieldState(),
    val articleQuantity: TextFieldState = TextFieldState(),
    val price: TextFieldState = TextFieldState(),
    val articleWebsiteLink: TextFieldState = TextFieldState(),
    val unitsDropdownMenuVisible: Boolean = false,
    val isLoading: Boolean = false,
    val articleNameError: Boolean = false,
    val articleWebsiteLinkError: Boolean = false,
    val okToSave: Boolean = false,
    val isBottomSheetVisible: Boolean = false,
    val isLocationStringLoading: Boolean = false,
    val isEditMode: Boolean = false,
)

sealed interface AddNewArticleAction {
    data object SaveArticle : AddNewArticleAction
    data class UpdateLocation(val location: UILocation) : AddNewArticleAction
    data class UpdatePhoto(val photoUri: String) : AddNewArticleAction
    data object ClearLocation : AddNewArticleAction
    data object ClearPhoto : AddNewArticleAction
    data object ShowUnitsDropdownMenu : AddNewArticleAction
    data object HideUnitsDropdownMenu : AddNewArticleAction
    data class UpdateArticleUnit(val unit: ArticleUnit) : AddNewArticleAction
    data object ShowBottomSheet : AddNewArticleAction
    data object HideBottomSheet : AddNewArticleAction
}
