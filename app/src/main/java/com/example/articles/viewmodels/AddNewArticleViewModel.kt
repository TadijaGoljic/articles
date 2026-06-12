package com.example.articles.viewmodels

import android.content.Context
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import android.net.Uri
import android.util.Log
import com.example.articles.ArticlesApplication.Companion.LOG_TAG
import com.example.articles.models.ArticleUnit
import com.example.articles.models.Location
import com.example.articles.models.UIArticle
import com.example.articles.nominatim.NominatimAPI
import com.example.articles.repositories.ArticleRepository
import com.example.articles.utils.FileManager
import com.example.articles.ui.composables.ArticlesDestinations
import com.example.articles.utils.triggerSync
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
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
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<ArticlesDestinations.AddNewArticleDestination>()
    private val groupId: UUID = UUID.fromString(args.groupId)

    private var state = MutableStateFlow(AddNewArticleState())
    val uiState = state

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
        val articlePricePerUnit = state.value.articlePricePerUnit.text.toString().toDoubleOrNull()
        val articleWebsiteLink = state.value.articleWebsiteLink.text.toString()
        if (articleName.isBlank()) {
            state.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    isError = true
                )
            }
            return
        }
        val article = state.value.article.copy(
            groupId = groupId,
            name = articleName,
            description = articleDescription,
            quantity = articleQuantity,
            pricePerUnit = articlePricePerUnit,
            websiteLink = articleWebsiteLink,
        )
        viewModelScope.launch {
            articleRepository.upsertArticle(article)
            triggerSync(context)
            state.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    okToSave = true,
                )
            }
        }
    }

    private fun updateLocation(location: Location?) {
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
    val articlePricePerUnit: TextFieldState = TextFieldState(),
    val articleWebsiteLink: TextFieldState = TextFieldState(),
    val unitsDropdownMenuVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val okToSave: Boolean = false,
    val isBottomSheetVisible: Boolean = false,
    val isLocationStringLoading: Boolean = false,
)

sealed interface AddNewArticleAction {
    data object SaveArticle : AddNewArticleAction
    data class UpdateLocation(val location: Location) : AddNewArticleAction
    data class UpdatePhoto(val photoUri: String) : AddNewArticleAction
    data object ClearLocation : AddNewArticleAction
    data object ClearPhoto : AddNewArticleAction
    data object ShowUnitsDropdownMenu : AddNewArticleAction
    data object HideUnitsDropdownMenu : AddNewArticleAction
    data class UpdateArticleUnit(val unit: ArticleUnit) : AddNewArticleAction
    data object ShowBottomSheet : AddNewArticleAction
    data object HideBottomSheet : AddNewArticleAction
}
