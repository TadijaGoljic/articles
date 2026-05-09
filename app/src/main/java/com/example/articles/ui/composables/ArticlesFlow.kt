package com.example.articles.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.articles.ui.composables.screens.AddNewArticle
import com.example.articles.ui.composables.screens.LocationPicker
import com.example.articles.ui.composables.screens.SettingsScreen
import com.example.articles.ui.composables.screens.GroupDetailsPage
import com.example.articles.ui.composables.screens.GroupsListPage
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
                onBackClick = {
                    navController.popBackStack()
                },
                onAddArticle = { group ->
                    navController.navigate(ArticlesDestinations.AddNewArticleDestination(groupId = group.id.toString()))
                }
            )
        }
        composable<ArticlesDestinations.AddNewArticleDestination> {
            AddNewArticle(
                onBackClick = {
                    navController.popBackStack()
                },
//                onLocationPickerClick = {
//                    navController.navigate(ArticlesDestinations.PickLocationDestination)
//                }
            )
        }
        composable<ArticlesDestinations.PickLocationDestination> {
            LocationPicker(
                onLocationSelected = { location ->
//                    navController.previousBackStackEntry?.savedStateHandle[KEY_LOCATION] = location
//                    navController.popBackStack()
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
sealed interface ArticlesDestinations {
    @Serializable data object GroupsListDestination : ArticlesDestinations
    @Serializable data class AddNewArticleDestination(val groupId: String) : ArticlesDestinations
    @Serializable data object PickLocationDestination : ArticlesDestinations
    @Serializable data object SettingsDestination : ArticlesDestinations
    @Serializable data class GroupDetailsDestination(val groupId: String) : ArticlesDestinations
}