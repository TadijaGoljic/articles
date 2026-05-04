package com.example.articles.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.articles.ui.composables.screens.AddNewArticle
import com.example.articles.ui.composables.screens.ArticlesList
import com.example.articles.ui.composables.screens.LocationPicker
import com.example.articles.ui.composables.screens.SettingsScreen
import com.example.articles.viewmodels.AddNewArticleViewModel.Companion.KEY_LOCATION
import com.example.articles.models.Location
import com.example.articles.ui.composables.screens.GroupsListPage
import kotlinx.serialization.Serializable

@Composable
fun ArticlesNavigationFlow(
    onSignOut: () -> Unit,
    onAccountDelete: () -> Unit,
    moveTaskToBackground: () -> Unit,
    onOpenNavigation: (Location) -> Unit,
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
                }
            )
        }
        composable<ArticlesDestinations.ArticlesListDestination> {
            ArticlesList(
                openAddNewArticleScreen = {
                    navController.navigate(ArticlesDestinations.AddNewArticleDestination)
                },
                openCreateGroupScreen = {
                    navController.navigate(ArticlesDestinations.GroupsListDestination)
                },
                onOpenNavigation = { locationData ->
                    onOpenNavigation(locationData)
                },
                onBackClick = moveTaskToBackground,
                onOpenSettings = {
                    navController.navigate(ArticlesDestinations.SettingsDestination)
                }
            )
        }
        composable<ArticlesDestinations.AddNewArticleDestination> {
            AddNewArticle(
                onBackButtonClicked = {
                    navController.popBackStack()
                },
                onLocationPickerClick = {
                    navController.navigate(ArticlesDestinations.PickLocationDestination)
                },
                navController = navController,
                onFinish = navController::popBackStack
            )
        }
        composable<ArticlesDestinations.PickLocationDestination> {
            LocationPicker(
                onLocationSelected = { location ->
                    navController.previousBackStackEntry?.savedStateHandle[KEY_LOCATION] = location
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
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

private sealed interface ArticlesDestinations {
    @Serializable data object GroupsListDestination : ArticlesDestinations
    @Serializable data object ArticlesListDestination : ArticlesDestinations
    @Serializable data object AddNewArticleDestination : ArticlesDestinations
    @Serializable data object PickLocationDestination : ArticlesDestinations
    @Serializable data object SettingsDestination : ArticlesDestinations
}