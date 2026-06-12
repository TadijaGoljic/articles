package com.example.articles.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.articles.models.Location
import com.example.articles.ui.composables.screens.AddNewArticle
import com.example.articles.ui.composables.screens.LocationPicker
import com.example.articles.ui.composables.screens.SettingsScreen
import com.example.articles.ui.composables.screens.GroupDetailsPage
import com.example.articles.ui.composables.screens.GroupsListPage
import com.example.articles.viewmodels.AddNewArticleAction
import com.example.articles.viewmodels.AddNewArticleViewModel
import kotlinx.serialization.Serializable

@Composable
fun ArticlesNavigationFlow(
    onSignOut: () -> Unit,
    onAccountDelete: () -> Unit,
    modifier: Modifier = Modifier
) {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ArticlesDestinations.GroupsListDestination,
        modifier = modifier
    ) {
        composable<ArticlesDestinations.GroupsListDestination> {
            GroupsListPage(
                onSettingsClick = {
                    navController.navigate(ArticlesDestinations.SettingsDestination)
                },
                onGroupClick = { group ->
                    navController.navigate(ArticlesDestinations.GroupDetailsDestination(groupId = group.id.toString()))
                }
            )
        }
        composable<ArticlesDestinations.GroupDetailsDestination> {
            GroupDetailsPage(
                onBackClick = navController::popBackStack,
                onAddArticle = { group ->
                    navController.navigate(ArticlesDestinations.AddNewArticleDestination(groupId = group.id.toString()))
                }
            )
        }
        composable<ArticlesDestinations.AddNewArticleDestination> { backStackEntry ->
            val addNewArticleViewModel: AddNewArticleViewModel = hiltViewModel()
            val pickedLocation by backStackEntry.savedStateHandle
                .getStateFlow<Location?>(KEY_LOCATION, null)
                .collectAsStateWithLifecycle()

            LaunchedEffect(pickedLocation) {
                pickedLocation?.let { location ->
                    addNewArticleViewModel.handleAction(AddNewArticleAction.UpdateLocation(location))
                    backStackEntry.savedStateHandle.remove<Location>(KEY_LOCATION)
                }
            }

            AddNewArticle(
                onBackClick = navController::popBackStack,
                onLocationPickerClick = { location ->
                    navController.navigate(
                        ArticlesDestinations.PickLocationDestination(
                            latitude = location?.latitude,
                            longitude = location?.longitude
                        )
                    )
                },
                addNewArticleViewModel = addNewArticleViewModel
            )
        }
        composable<ArticlesDestinations.PickLocationDestination> {
            LocationPicker(
                onLocationSelected = { location ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(KEY_LOCATION, location)
                    navController.popBackStack()
                },
                onCancel = navController::popBackStack
            )
        }
        composable<ArticlesDestinations.SettingsDestination> {
            SettingsScreen(
                onSignOut = onSignOut,
                onBackClick = navController::popBackStack,
                onAccountDelete = onAccountDelete
            )
        }
    }

}

private const val KEY_LOCATION = "KEY_LOCATION"

sealed interface ArticlesDestinations {
    @Serializable data object GroupsListDestination : ArticlesDestinations
    @Serializable data class AddNewArticleDestination(val groupId: String) : ArticlesDestinations
    @Serializable data class PickLocationDestination(val latitude: Double? = null, val longitude: Double? = null) : ArticlesDestinations
    @Serializable data object SettingsDestination : ArticlesDestinations
    @Serializable data class GroupDetailsDestination(val groupId: String) : ArticlesDestinations
}